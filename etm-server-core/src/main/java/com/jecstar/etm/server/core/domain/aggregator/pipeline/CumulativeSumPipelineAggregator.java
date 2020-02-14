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

package com.jecstar.etm.server.core.domain.aggregator.pipeline;

import org.elasticsearch.search.aggregations.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.pipeline.CumulativeSumPipelineAggregationBuilder;

public class CumulativeSumPipelineAggregator extends PathBasedPipelineAggregator {

    public static final String TYPE = "cumulative_sum";

    public CumulativeSumPipelineAggregator() {
        super();
        setPipelineAggregatorType(TYPE);
    }

    @Override
    public CumulativeSumPipelineAggregator clone() {
        CumulativeSumPipelineAggregator clone = new CumulativeSumPipelineAggregator();
        super.clone(clone);
        return clone;
    }

    @Override
    public CumulativeSumPipelineAggregationBuilder toAggregationBuilder() {
        return PipelineAggregatorBuilders.cumulativeSum(getId(), getPath()).setMetaData(getMetadata());
    }
}
