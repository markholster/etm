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

package com.jecstar.etm.server.core.domain.aggregator.bucket;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.domain.aggregator.DateInterval;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.metrics.Max;
import org.elasticsearch.search.aggregations.metrics.Min;

public class DateHistogramBucketAggregator extends BucketAggregator {

    public static final String TYPE = "date_histogram";

    private static final String INTERVAL = "interval";
    private static final String MIN_DOC_COUNT = "min_doc_count";
    private static final String KEY_AUTO_INTERVAL = "auto";

    @JsonField(INTERVAL)
    private String interval;
    @JsonField(MIN_DOC_COUNT)
    private Long minDocCount;

    private DateInterval dateInterval;
    private DateHistogramInterval autoInterval;

    public DateHistogramBucketAggregator() {
        super();
        setBucketAggregatorType(TYPE);
    }

    public Long getMinDocCount() {
        return this.minDocCount;
    }

    public String getInterval() {
        return this.interval;
    }

    public DateHistogramBucketAggregator setInterval(String interval) {
        this.interval = interval;
        return this;
    }

    public DateHistogramBucketAggregator setMinDocCount(Long minDocCount) {
        this.minDocCount = minDocCount;
        return this;
    }


    public DateInterval getDateInterval() {
        if (getInterval() == null || isAutoInterval()) {
            return null;
        }
        if (this.dateInterval == null) {
            this.dateInterval = DateInterval.safeValueOf(getInterval());
        }
        return this.dateInterval;
    }

    public DateHistogramInterval getDateHistogramInterval() {
        if (getInterval() == null || isAutoInterval()) {
            return null;
        }
        var dateInterval = getDateInterval();
        if (dateInterval != null) {
            return dateInterval.getDateHistogramInterval();
        }
        return new DateHistogramInterval(getInterval());
    }

    private boolean isAutoInterval() {
        return KEY_AUTO_INTERVAL.equals(getInterval());
    }

    @Override
    public DateHistogramBucketAggregator clone() {
        DateHistogramBucketAggregator clone = new DateHistogramBucketAggregator();
        clone.interval = this.interval;
        clone.minDocCount = this.minDocCount;
        clone.autoInterval = this.autoInterval;
        super.clone(clone);
        return clone;
    }

    @Override
    protected AggregationBuilder createAggregationBuilder() {
        DateHistogramInterval interval;
        if (getDateInterval() != null) {
            interval = getDateInterval().getDateHistogramInterval();
        } else if (getDateHistogramInterval() != null) {
            interval = getDateHistogramInterval();
        } else {
            interval = this.autoInterval;
        }
        DateHistogramAggregationBuilder builder = AggregationBuilders.dateHistogram(getId())
                .setMetaData(getMetadata())
                .field(getField())
                .dateHistogramInterval(interval);
        if (getMinDocCount() != null) {
            builder.minDocCount(getMinDocCount());
        }
        return builder;
    }

    @Override
    public void prepareForSearch(DataRepository dataRepository, SearchRequestBuilder searchRequestBuilder) {
        // This method can be removed as soon as the AutoDateHistogramBucketAggregator of elasticsearch supports minDocCount.
        super.prepareForSearch(dataRepository, searchRequestBuilder);
        searchRequestBuilder
                .addAggregation(AggregationBuilders.min(getId() + "-min").field(getField()))
                .addAggregation(AggregationBuilders.max(getId() + "-max").field(getField()));
        SearchResponse searchResponse = dataRepository.search(searchRequestBuilder);
        long min = Double.valueOf(((Min) searchResponse.getAggregations().get(getId() + "-min")).getValue()).longValue();
        long max = Double.valueOf(((Max) searchResponse.getAggregations().get(getId() + "-max")).getValue()).longValue();
        long diffInSeconds = (max - min) / 1000;
        long partsInSeconds = diffInSeconds / 50;
        if (partsInSeconds == 0) {
            partsInSeconds = 1;
        }
        this.autoInterval = new DateHistogramInterval(partsInSeconds + "s");
    }
}
