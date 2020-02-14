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

package com.jecstar.etm.gui.rest.services.dashboard.domain.graph;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.server.core.converter.JsonField;

/**
 * A <code>AxesGraph</code> that shows one or more groups of data in a bar chart.
 */
public class BarGraph extends AxesGraph<BarGraph> {

    public static final String TYPE = "bar";
    public static final String SUB_TYPE = "sub_type";

    @JsonField(SUB_TYPE)
    private String subType;

    public BarGraph() {
        super();
        setType(TYPE);
    }

    public String getSubType() {
        return this.subType;
    }

    public BarGraph setSubType(String subType) {
        this.subType = subType;
        return this;
    }

    @Override
    public void appendHighchartsConfig(JsonBuilder builder) {
        super.appendHighchartsConfig(builder);
        String chartType = Orientation.HORIZONTAL.equals(getOrientation()) ? "bar" : "column";
        builder.startObject("chart").field("type", chartType).endObject();
        builder.startObject("plotOptions").startObject(chartType);
        if ("percentage".equals(getSubType())) {
            builder.field("stacking", "percent");
        } else if ("stacked".equals(getSubType())) {
            builder.field("stacking", "normal");
        }
        builder.endObject().endObject();
    }

    @Override
    public void mergeFromColumn(BarGraph graph) {
        super.mergeFromColumn(graph);
        this.subType = graph.getSubType();
    }
}
