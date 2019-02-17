package com.jecstar.etm.signaler;

import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.BucketAggregator;
import com.jecstar.etm.server.core.domain.aggregator.metric.MetricsAggregator;
import com.jecstar.etm.server.core.domain.aggregator.pipeline.PipelineAggregator;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.signaler.domain.Data;
import com.jecstar.etm.signaler.domain.Signal;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.joda.time.DateTimeZone;

import java.util.function.Function;

public class SignalSearchRequestBuilderBuilder {

    public static final String CARDINALITY_AGGREGATION_KEY = "cardinality";

    private final DataRepository dataRepository;
    private final EtmConfiguration etmConfiguration;

    private Signal signal;

    public SignalSearchRequestBuilderBuilder(DataRepository dataRepository, EtmConfiguration etmConfiguration) {
        this.dataRepository = dataRepository;
        this.etmConfiguration = etmConfiguration;
    }

    public SignalSearchRequestBuilderBuilder setSignal(Signal signal) {
        this.signal = signal;
        return this;
    }

    public SearchRequestBuilder build(Function<BoolQueryBuilder, QueryBuilder> enhanceCallback, EtmPrincipal etmPrincipal) {
        return createAggregatedSearchRequest(enhanceCallback, etmPrincipal);

    }

    private SearchRequestBuilder createAggregatedSearchRequest(Function<BoolQueryBuilder, QueryBuilder> enhanceCallback, EtmPrincipal etmPrincipal) {
        final Data data = this.signal.getData();
        SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder()
                .setIndices(data.getDataSource())
                .setFetchSource(false)
                .setTimeout(TimeValue.timeValueMillis(this.etmConfiguration.getQueryTimeout()))
                .setSize(0);
        QueryStringQueryBuilder queryStringBuilder = new QueryStringQueryBuilder(data.getQuery())
                .allowLeadingWildcard(true)
                .analyzeWildcard(true);
        if (etmPrincipal != null) {
            queryStringBuilder.timeZone(DateTimeZone.forTimeZone(etmPrincipal.getTimeZone()));
        }
        queryStringBuilder.defaultField(ElasticsearchLayout.ETM_ALL_FIELDS_ATTRIBUTE_NAME);
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder().must(queryStringBuilder);
        if (data.getFrom() != null || data.getTill() != null) {
            RangeQueryBuilder timestampFilter = new RangeQueryBuilder(data.getTimeFilterField());
            if (data.getFrom() != null) {
                timestampFilter.gte(data.getFrom());
            }
            if (data.getTill() != null) {
                timestampFilter.lte(data.getTill());
            }
            boolQueryBuilder.filter(timestampFilter);
        }
        QueryBuilder queryBuilder = enhanceCallback.apply(boolQueryBuilder);
        searchRequestBuilder.setQuery(queryBuilder);

        DateHistogramAggregationBuilder aggregationBuilder = AggregationBuilders.dateHistogram(CARDINALITY_AGGREGATION_KEY).field(data.getTimeFilterField());
        aggregationBuilder.dateHistogramInterval(new DateHistogramInterval(this.signal.getThreshold().getCardinalityUnit().toTimestampExpression(this.signal.getThreshold().getCardinality())));

        for (Aggregator aggregator : this.signal.getThreshold().getAggregators()) {
            if (aggregator instanceof BucketAggregator) {
                BucketAggregator bucketAggregator = (BucketAggregator) aggregator;
                bucketAggregator.prepareForSearch(this.dataRepository, searchRequestBuilder);
                aggregationBuilder.subAggregation(bucketAggregator.toAggregationBuilder());
            } else if (aggregator instanceof MetricsAggregator) {
                aggregationBuilder.subAggregation(((MetricsAggregator) aggregator).toAggregationBuilder());
            } else if (aggregator instanceof PipelineAggregator) {
                aggregationBuilder.subAggregation(((PipelineAggregator) aggregator).toAggregationBuilder());
            }
        }
        searchRequestBuilder.addAggregation(aggregationBuilder);
        return searchRequestBuilder;
    }

}
