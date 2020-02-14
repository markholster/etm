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

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import org.elasticsearch.search.aggregations.PipelineAggregationBuilder;

public abstract class PipelineAggregator extends Aggregator {

    public static final String TYPE = "pipeline";
    public static final String PIPELINE_AGGREGATOR_TYPE = "pipeline_type";

    @JsonField(PIPELINE_AGGREGATOR_TYPE)
    private String pipelineAggregatorType;

    public PipelineAggregator() {
        super();
        setAggregatorType(TYPE);
    }

    public String getPipelineAggregatorType() {
        return this.pipelineAggregatorType;
    }

    protected PipelineAggregator setPipelineAggregatorType(String pipelineAggregatorType) {
        this.pipelineAggregatorType = pipelineAggregatorType;
        return this;
    }

    @Override
    public PipelineAggregator clone() {
        throw new UnsupportedOperationException("Clone method must be implemented in '" + getClass().getName() + ".");
    }

    protected void clone(PipelineAggregator target) {
        super.clone(target);
        target.pipelineAggregatorType = this.pipelineAggregatorType;
    }

    @Override
    public abstract PipelineAggregationBuilder toAggregationBuilder();
}
