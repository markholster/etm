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
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;

public class TermBucketAggregator extends BucketAggregator {

    public static final String TYPE = "term";

    private static final String TOP = "top";
    private static final String ORDER = "order";
    private static final String ORDER_BY = "order_by";
    private static final String MIN_DOC_COUNT = "min_doc_count";

    @JsonField(TOP)
    private Integer top;
    @JsonField(ORDER)
    private String order;
    @JsonField(ORDER_BY)
    private String orderBy;
    @JsonField(MIN_DOC_COUNT)
    private Long minDocCount;

    public TermBucketAggregator() {
        super();
        setBucketAggregatorType(TYPE);
    }

    public Integer getTop() {
        return this.top != null ? this.top : 5;
    }

    public String getOrder() {
        return this.order;
    }

    public String getOrderBy() {
        return this.orderBy;
    }

    public Long getMinDocCount() {
        return this.minDocCount;
    }

    public TermBucketAggregator setTop(Integer top) {
        this.top = top;
        return this;
    }

    public TermBucketAggregator setOrder(String order) {
        this.order = order;
        return this;
    }

    public TermBucketAggregator setOrderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public TermBucketAggregator setMinDocCount(Long minDocCount) {
        this.minDocCount = minDocCount;
        return this;
    }

    @Override
    public TermBucketAggregator clone() {
        TermBucketAggregator clone = new TermBucketAggregator();
        clone.top = this.top;
        clone.order = this.order;
        clone.orderBy = this.orderBy;
        clone.minDocCount = this.minDocCount;
        super.clone(clone);
        return clone;
    }

    @Override
    protected TermsAggregationBuilder createAggregationBuilder() {
        TermsAggregationBuilder builder = AggregationBuilders.terms(getId())
                .setMetaData(getMetadata())
                .field(getFieldOrKeyword())
                .size(getTop());
        if ("_key".equalsIgnoreCase(getOrderBy())) {
            builder.order(BucketOrder.key("asc".equalsIgnoreCase(getOrder())));
        } else if ("_count".equalsIgnoreCase(getOrderBy())) {
            builder.order(BucketOrder.count("asc".equalsIgnoreCase(getOrder())));
        } else {
            builder.order(BucketOrder.aggregation(getOrderBy(), "asc".equalsIgnoreCase(getOrder())));
        }
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
