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
import org.elasticsearch.search.aggregations.bucket.significant.SignificantTermsAggregationBuilder;

public class SignificantTermBucketAggregator extends BucketAggregator {

    public static final String TYPE = "significant_term";

    private static final String TOP = "top";
    private static final String MIN_DOC_COUNT = "min_doc_count";


    @JsonField(TOP)
    private Integer top;
    @JsonField(MIN_DOC_COUNT)
    private Long minDocCount;

    public SignificantTermBucketAggregator setTop(Integer top) {
        this.top = top;
        return this;
    }

    public SignificantTermBucketAggregator setMinDocCount(Long minDocCount) {
        this.minDocCount = minDocCount;
        return this;
    }

    public SignificantTermBucketAggregator() {
        super();
        setBucketAggregatorType(TYPE);
    }

    public Integer getTop() {
        return this.top != null ? this.top : 5;
    }

    public Long getMinDocCount() {
        return this.minDocCount;
    }

    @Override
    public SignificantTermBucketAggregator clone() {
        SignificantTermBucketAggregator clone = new SignificantTermBucketAggregator();
        clone.top = this.top;
        clone.minDocCount = this.minDocCount;
        super.clone(clone);
        return clone;
    }

    @Override
    protected SignificantTermsAggregationBuilder createAggregationBuilder() {
        SignificantTermsAggregationBuilder builder = AggregationBuilders.significantTerms(getId())
                .setMetaData(getMetadata())
                .field(getFieldOrKeyword()).size(getTop());
        if (getMinDocCount() != null) {
            builder.minDocCount(getMinDocCount());
        }
        return builder;
    }

    @Override
    protected boolean isBucketKeyNumeric() {
        return false;
    }
}
