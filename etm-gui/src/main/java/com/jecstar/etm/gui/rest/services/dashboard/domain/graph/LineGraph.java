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
import com.jecstar.etm.server.core.converter.custom.EnumConverter;

/**
 * A <code>AxesGraph</code> that shows one or more groups of data in a line chart.
 */
public class LineGraph extends AxesGraph<LineGraph> {

    public static final String TYPE = "line";
    public static final String SHOW_MARKERS = "show_markers";
    public static final String SHOW_DATA_LABELS = "show_data_labels";
    public static final String LINE_TYPE = "line_type";

    @JsonField(value = LINE_TYPE, converterClass = EnumConverter.class)
    private LineType lineType;
    @JsonField(SHOW_MARKERS)
    private boolean showMarkers;
    @JsonField(SHOW_DATA_LABELS)
    private boolean showDataLabels;

    public LineGraph() {
        super();
        setType(TYPE);
    }

    public LineType getLineType() {
        return this.lineType;
    }

    public boolean isShowMarkers() {
        return this.showMarkers;
    }

    public boolean isShowDataLabels() {
        return this.showDataLabels;
    }

    public LineGraph setLineType(LineType lineType) {
        this.lineType = lineType;
        return this;
    }

    public LineGraph setShowDataLabels(boolean showDataLabels) {
        this.showDataLabels = showDataLabels;
        return this;
    }

    public LineGraph setShowMarkers(boolean showMarkers) {
        this.showMarkers = showMarkers;
        return this;
    }

    @Override
    public void appendHighchartsConfig(JsonBuilder builder) {
        super.appendHighchartsConfig(builder);
        var inverted = Orientation.HORIZONTAL.equals(getOrientation());
        builder.startObject("chart").field("type", getChartType()).field("inverted", inverted).endObject();
        builder.startObject("plotOptions").startObject(getChartType()).startObject("marker").field("enabled", isShowMarkers()).endObject();
        builder.startObject("dataLabels").field("enabled", isShowDataLabels()).endObject();
        getLineType().addHighchartsPlotOptions(builder);
        builder.endObject().endObject();
    }

    private String getChartType() {
        return getLineType().getHighchartsChartType(this);
    }

    @Override
    public LineGraph mergeFromColumn(Graph<?> graph) {
        super.mergeFromColumn(graph);
        if (graph instanceof LineGraph) {
            var other = (LineGraph) graph;
            this.lineType = other.getLineType();
            this.showMarkers = other.isShowMarkers();
            this.showDataLabels = other.isShowDataLabels();
        }
        return this;
    }
}
