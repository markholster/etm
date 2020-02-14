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
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;

public class FilterBucketAggregator extends BucketAggregator {

    public static final String TYPE = "filter";

    private static final String VALUE = "value";

    @JsonField(VALUE)
    private String value;

    public FilterBucketAggregator() {
        super();
        setBucketAggregatorType(TYPE);
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public FilterBucketAggregator clone() {
        FilterBucketAggregator clone = new FilterBucketAggregator();
        clone.value = this.value;
        super.clone(clone);
        return clone;
    }

    @Override
    protected FilterAggregationBuilder createAggregationBuilder() {
        return AggregationBuilders.filter(getId(), QueryBuilders.termQuery(getFieldOrKeyword(), getValue()))
                .setMetaData(getMetadata());
    }

}
