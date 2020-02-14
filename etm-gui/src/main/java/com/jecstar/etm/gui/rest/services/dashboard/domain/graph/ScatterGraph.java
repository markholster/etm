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
 * A <code>AxesGraph</code> that shows one or more groups of data in a scatter plot.
 */
public class ScatterGraph extends AxesGraph<ScatterGraph> {

    public static final String TYPE = "scatter";
    public static final String SHOW_DATA_LABELS = "show_data_labels";

    @JsonField(SHOW_DATA_LABELS)
    private boolean showDataLabels;

    public ScatterGraph() {
        super();
        setType(TYPE);
    }

    public boolean isShowDataLabels() {
        return this.showDataLabels;
    }

    @Override
    public void appendHighchartsConfig(JsonBuilder builder) {
        super.appendHighchartsConfig(builder);
        boolean inverted = Orientation.HORIZONTAL.equals(getOrientation());
        builder.startObject("chart").field("type", "scatter").field("inverted", inverted).endObject();
        builder.startObject("plotOptions").startObject("scatter").startObject("dataLabels").field("enabled", isShowDataLabels()).endObject();
        builder.endObject().endObject();
    }

    @Override
    public void mergeFromColumn(ScatterGraph graph) {
        super.mergeFromColumn(graph);
        this.showDataLabels = graph.isShowDataLabels();
    }
}
