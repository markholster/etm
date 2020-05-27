/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.signaler;

import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.BucketAggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.DateHistogramBucketAggregator;
import com.jecstar.etm.server.core.domain.aggregator.metric.MetricsAggregator;
import com.jecstar.etm.server.core.domain.aggregator.pipeline.PipelineAggregator;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.server.core.persisting.EtmQueryBuilder;
import com.jecstar.etm.server.core.persisting.RequestEnhancer;
import com.jecstar.etm.server.core.util.DateUtils;
import com.jecstar.etm.signaler.domain.Signal;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;

import java.time.Instant;
import java.util.TimeZone;
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
        final var data = this.signal.getData();
        final var requestEnhancer = new RequestEnhancer(this.etmConfiguration);
        SearchRequestBuilder searchRequestBuilder = requestEnhancer.enhance(new SearchRequestBuilder())
                .setIndices(etmConfiguration.mergeRemoteIndices(data.getDataSource()))
                .setFetchSource(false)
                .setTimeout(TimeValue.timeValueMillis(this.etmConfiguration.getQueryTimeout()))
                .setSize(0);

        var etmQueryBuilder = new EtmQueryBuilder(data.getQuery(), dataRepository, requestEnhancer);

        var timeZone = TimeZone.getDefault();
        if (etmPrincipal != null) {
            timeZone = etmPrincipal.getTimeZone();
        }
        etmQueryBuilder.setTimeZone(timeZone.getID());
        if (data.getFrom() != null || data.getTill() != null) {
            RangeQueryBuilder timestampFilter = new RangeQueryBuilder(data.getTimeFilterField());
            if (data.getFrom() != null) {
                Instant instant = DateUtils.parseDateString(data.getFrom(), timeZone.toZoneId(), true);
                if (instant != null) {
                    timestampFilter.gte("" + instant.toEpochMilli());
                } else {
                    timestampFilter.gte(data.getFrom());
                    timestampFilter.timeZone(timeZone.getID());
                }
            }
            if (data.getTill() != null) {
                Instant instant = DateUtils.parseDateString(data.getTill(), timeZone.toZoneId(), false);
                if (instant != null) {
                    timestampFilter.lte("" + instant.toEpochMilli());
                } else {
                    timestampFilter.lte(data.getTill());
                    timestampFilter.timeZone(timeZone.getID());
                }
            }
            etmQueryBuilder.filterRoot(timestampFilter).filterJoin(timestampFilter);
        }
        QueryBuilder queryBuilder = enhanceCallback.apply(etmQueryBuilder.buildRootQuery());
        searchRequestBuilder.setQuery(queryBuilder);

        var aggregationBuilder = (DateHistogramAggregationBuilder) new DateHistogramBucketAggregator()
                .setInterval(this.signal.getThreshold().getCardinalityUnit().toTimestampExpression(this.signal.getThreshold().getCardinality()))
                .setField(data.getTimeFilterField())
                .setName(CARDINALITY_AGGREGATION_KEY)
                .setId(CARDINALITY_AGGREGATION_KEY)
                .setShowOnGraph(Boolean.TRUE)
                .toAggregationBuilder();

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
