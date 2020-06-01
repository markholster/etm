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

package com.jecstar.etm.server.core.domain.aggregator.metric;

import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.ValueCountAggregationBuilder;

public class CountMetricsAggregator extends MetricsAggregator {

    public static final String TYPE = "count";

    public CountMetricsAggregator() {
        super();
        setMetricsAggregatorType(TYPE);
    }

    @Override
    public CountMetricsAggregator clone() {
        CountMetricsAggregator clone = new CountMetricsAggregator();
        super.clone(clone);
        return clone;
    }

    @Override
    public ValueCountAggregationBuilder toAggregationBuilder() {
        return AggregationBuilders.count(getId()).setMetaData(getMetadata()).field("id.keyword");
    }
}
