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
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder;

public class HistogramBucketAggregator extends BucketAggregator {

    public static final String TYPE = "histogram";

    private static final String INTERVAL = "interval";
    private static final String MIN_DOC_COUNT = "min_doc_count";

    @JsonField(INTERVAL)
    private Double interval;
    @JsonField(MIN_DOC_COUNT)
    private Long minDocCount;

    public HistogramBucketAggregator() {
        super();
        setBucketAggregatorType(TYPE);
    }

    public Double getInterval() {
        return this.interval != null ? this.interval : 1D;
    }

    public Long getMinDocCount() {
        return this.minDocCount;
    }

    public HistogramBucketAggregator setInterval(Double interval) {
        this.interval = interval;
        return this;
    }

    public HistogramBucketAggregator setMinDocCount(Long minDocCount) {
        this.minDocCount = minDocCount;
        return this;
    }

    @Override
    public HistogramBucketAggregator clone() {
        HistogramBucketAggregator clone = new HistogramBucketAggregator();
        clone.interval = this.interval;
        clone.minDocCount = this.minDocCount;
        super.clone(clone);
        return clone;
    }

    @Override
    protected HistogramAggregationBuilder createAggregationBuilder() {
        HistogramAggregationBuilder builder = AggregationBuilders.histogram(getId())
                .setMetaData(getMetadata())
                .field(getField())
                .interval(getInterval());
        if (getMinDocCount() != null) {
            builder.minDocCount(getMinDocCount());
        }
        return builder;
    }
}
