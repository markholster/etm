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
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.ScriptedMetricAggregationBuilder;

public class ScriptedMetricsAggregator extends MetricsAggregator {

    public static final String TYPE = "scripted";

    private static final String INIT_SCRIPT = "init_script";
    private static final String MAP_SCRIPT = "map_script";
    private static final String COMBINE_SCRIPT = "combine_script";
    private static final String REDUCE_SCRIPT = "reduce_script";

    @JsonField(INIT_SCRIPT)
    private String initScript;
    @JsonField(MAP_SCRIPT)
    private String mapScript;
    @JsonField(COMBINE_SCRIPT)
    private String combineScript;
    @JsonField(REDUCE_SCRIPT)
    private String reduceScript;

    public ScriptedMetricsAggregator() {
        super();
        setMetricsAggregatorType(TYPE);
    }

    public String getInitScript() {
        return this.initScript;
    }

    public String getMapScript() {
        return this.mapScript;
    }

    public String getCombineScript() {
        return this.combineScript;
    }

    public String getReduceScript() {
        return this.reduceScript;
    }

    @Override
    public ScriptedMetricsAggregator clone() {
        ScriptedMetricsAggregator clone = new ScriptedMetricsAggregator();
        clone.initScript = this.initScript;
        clone.mapScript = this.mapScript;
        clone.combineScript = this.combineScript;
        clone.reduceScript = this.reduceScript;
        super.clone(clone);
        return clone;
    }

    @Override
    public ScriptedMetricAggregationBuilder toAggregationBuilder() {
        ScriptedMetricAggregationBuilder builder = AggregationBuilders.scriptedMetric(getId())
                .setMetaData(getMetadata())
                .mapScript(new Script(getMapScript()));
        if (getInitScript() != null) {
            builder.initScript(new Script(getInitScript()));
        }
        if (getCombineScript() != null) {
            builder.combineScript(new Script(getCombineScript()));
        }
        if (getReduceScript() != null) {
            builder.reduceScript(new Script(getReduceScript()));
        }
        return builder;

    }

}
