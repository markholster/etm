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

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation;
import org.elasticsearch.search.aggregations.metrics.ParsedPercentiles;
import org.elasticsearch.search.aggregations.metrics.ParsedTDigestPercentileRanks;

public class MetricValue {

    private final String jsonValue;
    private final String name;
    private final double value;

    public MetricValue(Aggregation aggregation) {
        if (aggregation instanceof ParsedTDigestPercentileRanks) {
            ParsedTDigestPercentileRanks percentileRanks = (ParsedTDigestPercentileRanks) aggregation;
            this.value = percentileRanks.iterator().next().getPercent();
        } else if (aggregation instanceof ParsedPercentiles) {
            ParsedPercentiles percentiles = (ParsedPercentiles) aggregation;
            this.value = percentiles.iterator().next().getValue();
        } else if (aggregation instanceof NumericMetricsAggregation.SingleValue) {
            NumericMetricsAggregation.SingleValue singleValue = (NumericMetricsAggregation.SingleValue) aggregation;
            this.value = singleValue.value();
        } else {
            throw new IllegalArgumentException("'" + aggregation.getClass().getName() + "' is an invalid metric aggregator.");
        }
        this.name = (String) aggregation.getMetaData().get("name");
        this.jsonValue = formatToDoubleValue(this.value);
    }

    public MetricValue(String name, double value) {
        this.name = name;
        this.value = value;
        this.jsonValue = formatToDoubleValue(this.value);
    }

    private String formatToDoubleValue(double value) {
        if (!hasValidValue()) {
            return "null";
        }
        return Double.toString(value);
    }

    public String getJsonValue() {
        return this.jsonValue;
    }

    public String getName() {
        return this.name;
    }

    public Double getValue() {
        return this.value;
    }

    public boolean hasValidValue() {
        return !(Double.isNaN(this.value) || Double.isInfinite(this.value));
    }
}
