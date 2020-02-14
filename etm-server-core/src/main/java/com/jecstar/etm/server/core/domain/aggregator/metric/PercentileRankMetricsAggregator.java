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
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.PercentileRanksAggregationBuilder;

public class PercentileRankMetricsAggregator extends FieldBasedMetricsAggregator {

    public static final String TYPE = "percentile_rank";
    private static final String RANK = "rank";

    @JsonField(RANK)
    private Double rank;

    public PercentileRankMetricsAggregator() {
        super();
        setMetricsAggregatorType(TYPE);
    }

    public Double getRank() {
        return this.rank;
    }

    public PercentileRankMetricsAggregator setRank(Double rank) {
        this.rank = rank;
        return this;
    }

    @Override
    public PercentileRankMetricsAggregator clone() {
        PercentileRankMetricsAggregator clone = new PercentileRankMetricsAggregator();
        clone.rank = this.rank;
        super.clone(clone);
        return clone;
    }

    @Override
    public PercentileRanksAggregationBuilder toAggregationBuilder() {
        return AggregationBuilders.percentileRanks(getId(), new double[]{getRank()}).setMetaData(getMetadata()).field(getField());
    }
}
