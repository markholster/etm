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
import com.jecstar.etm.server.core.domain.aggregator.converter.ScriptParameterListConverter;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.pipeline.BucketScriptPipelineAggregationBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ScriptedPipelineAggregator extends PipelineAggregator {

    public static final String TYPE = "scripted";
    private static final String PARAMETERS = "script_params";
    private static final String SCRIPT = "script";

    @JsonField(SCRIPT)
    private String script;
    @JsonField(value = PARAMETERS, converterClass = ScriptParameterListConverter.class)
    private List<ScriptParameter> parameters;

    public ScriptedPipelineAggregator() {
        super();
        setPipelineAggregatorType(TYPE);
    }

    public String getScript() {
        return this.script;
    }

    public List<ScriptParameter> getParameters() {
        return this.parameters;
    }

    @Override
    public ScriptedPipelineAggregator clone() {
        ScriptedPipelineAggregator clone = new ScriptedPipelineAggregator();
        clone.script = this.script;
        if (this.parameters != null) {
            clone.parameters = this.parameters.stream().map(d -> d.clone()).collect(Collectors.toList());
        }
        super.clone(clone);
        return clone;
    }

    @Override
    public BucketScriptPipelineAggregationBuilder toAggregationBuilder() {
        Map<String, String> params = new HashMap<>();
        Map<String, Object> params2 = new HashMap<>();
        for (ScriptParameter scriptParameter : getParameters()) {
            params.put(scriptParameter.getName(), scriptParameter.getAggregatorId());
            params2.put(scriptParameter.getName(), scriptParameter.getAggregatorId());
        }
        return PipelineAggregatorBuilders.bucketScript(getId(), params, new Script(ScriptType.INLINE, "painless", getScript(), params2)).setMetaData(getMetadata());
    }
}
