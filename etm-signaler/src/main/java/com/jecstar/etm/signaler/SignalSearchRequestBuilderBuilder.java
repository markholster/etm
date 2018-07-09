package com.jecstar.etm.signaler;

import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.signaler.domain.Signal;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
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

    private final Client client;
    private final EtmConfiguration etmConfiguration;

    private Signal signal;

    public SignalSearchRequestBuilderBuilder(Client client, EtmConfiguration etmConfiguration) {
        this.client = client;
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
        SearchRequestBuilder searchRequest = client.prepareSearch(this.signal.getDataSource())
                .setFetchSource(false)
                .setTimeout(TimeValue.timeValueMillis(this.etmConfiguration.getQueryTimeout()))
                .setSize(0);
        QueryStringQueryBuilder queryStringBuilder = new QueryStringQueryBuilder(this.signal.getQuery())
                .allowLeadingWildcard(true)
                .analyzeWildcard(true);
        if (etmPrincipal != null) {
            queryStringBuilder.timeZone(DateTimeZone.forTimeZone(etmPrincipal.getTimeZone()));
        }
        queryStringBuilder.defaultField(ElasticsearchLayout.ETM_ALL_FIELDS_ATTRIBUTE_NAME);
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder().must(queryStringBuilder);
        RangeQueryBuilder timestampFilter = new RangeQueryBuilder("timestamp");
        timestampFilter.gte("now-" + this.signal.getTimespanExpression());
        timestampFilter.lte("now");
        boolQueryBuilder.filter(timestampFilter);
        QueryBuilder queryBuilder = enhanceCallback.apply(boolQueryBuilder);
        searchRequest.setQuery(queryBuilder);

        DateHistogramAggregationBuilder aggregationBuilder = AggregationBuilders.dateHistogram(CARDINALITY_AGGREGATION_KEY).field("timestamp");
        aggregationBuilder.dateHistogramInterval(new DateHistogramInterval(this.signal.getCardinalityExpression()));

        if (Signal.Operation.AVERAGE.equals(this.signal.getOperation())) {
            aggregationBuilder.subAggregation(AggregationBuilders.avg("Average of " + this.signal.getField())
                    .field(this.signal.getField()));
        } else if (Signal.Operation.CARDINALITY.equals(this.signal.getOperation())) {
            aggregationBuilder.subAggregation(AggregationBuilders.cardinality("Unique count of " + this.signal.getField())
                    .field(this.signal.getField()));
            // TODO beschrijven dat dit niet een precieze waarde is: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-metrics-cardinality-aggregation.html
        } else if (Signal.Operation.COUNT.equals(this.signal.getOperation())) {
            aggregationBuilder.subAggregation(AggregationBuilders.count("Count")
                    .field("_type"));
        } else if (Signal.Operation.MAX.equals(this.signal.getOperation())) {
            aggregationBuilder.subAggregation(AggregationBuilders.max("Max of " + this.signal.getField())
                    .field(this.signal.getField()));
        } else if (Signal.Operation.MEDIAN.equals(signal.getOperation())) {
            aggregationBuilder.subAggregation(AggregationBuilders.percentiles("Median of " + this.signal.getField())
                    .field(this.signal.getField()).percentiles(50));
        } else if (Signal.Operation.MIN.equals(this.signal.getOperation())) {
            aggregationBuilder.subAggregation(AggregationBuilders.min("Min of " + this.signal.getField())
                    .field(this.signal.getField()));
//        } else if ("percentile".equals(operation)) {
//            String internalLabel = this.label;
//            Double percentileData = this.jsonConverter.getDouble("percentile_data", this.jsonData);
//            if (internalLabel == null) {
//                if (percentileData == 1) {
//                    internalLabel = "1st percentile of " + this.field;
//                } else if (percentileData == 2) {
//                    internalLabel = "2nd percentile of " + this.field;
//                } else if (percentileData == 3) {
//                    internalLabel = "3rd percentile of " + this.field;
//                } else {
//                    internalLabel = etmPrincipal.getNumberFormat().format(percentileData) + "th percentile of " + this.field;
//                }
//            }
//            metadata.put("label", internalLabel);
//            builder = AggregationBuilders.percentiles(this.id).field(this.field).percentiles(percentileData);
//        } else if ("percentile_rank".equals(operation)) {
//            Double percentileData = this.jsonConverter.getDouble("percentile_data", this.jsonData);
//            metadata.put("label", this.label != null ? this.label : "Percentile rank " + etmPrincipal.getNumberFormat().format(percentileData) + " of " + this.field);
//            builder = AggregationBuilders.percentileRanks(this.id, new double[]{percentileData}).field(this.field);
        } else if (Signal.Operation.SUM.equals(this.signal.getOperation())) {
            aggregationBuilder.subAggregation(AggregationBuilders.sum(this.signal.getOperation().name()).field(this.signal.getField()));
        }
        searchRequest.addAggregation(aggregationBuilder);
        return searchRequest;
    }

}
