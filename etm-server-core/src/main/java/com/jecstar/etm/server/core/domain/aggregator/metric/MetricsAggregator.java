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

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import org.elasticsearch.search.aggregations.AggregationBuilder;

public abstract class MetricsAggregator extends Aggregator {

    public static final String TYPE = "metrics";
    public static final String METRICS_AGGREGATOR_TYPE = "metrics_type";

    @JsonField(METRICS_AGGREGATOR_TYPE)
    private String metricsAggregatorType;

    public MetricsAggregator() {
        super();
        setAggregatorType(TYPE);
    }

    public String getMetricsAggregatorType() {
        return this.metricsAggregatorType;
    }

    protected MetricsAggregator setMetricsAggregatorType(String metricsAggregatorType) {
        this.metricsAggregatorType = metricsAggregatorType;
        return this;
    }

    @Override
    public MetricsAggregator clone() {
        throw new UnsupportedOperationException("Clone method must be implemented in '" + getClass().getName() + ".");
    }

    protected void clone(MetricsAggregator target) {
        super.clone(target);
        target.metricsAggregatorType = this.metricsAggregatorType;
    }

    @Override
    public abstract AggregationBuilder toAggregationBuilder();
}
