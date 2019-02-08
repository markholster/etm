package com.jecstar.etm.gui.rest.services.dashboard.domain.graph;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.converter.custom.EnumConverter;

/**
 * A <code>AxesGraph</code> that shows one or more groups of data in an area chart.
 */
public class AreaGraph extends AxesGraph<AreaGraph> {

    public static final String TYPE = "area";
    public static final String SUB_TYPE = "sub_type";
    public static final String SHOW_MARKERS = "show_markers";
    public static final String SHOW_DATA_LABELS = "show_data_labels";
    public static final String LINE_TYPE = "line_type";

    @JsonField(SUB_TYPE)
    private String subType;
    @JsonField(value = LINE_TYPE, converterClass = EnumConverter.class)
    private LineType lineType;
    @JsonField(SHOW_MARKERS)
    private boolean showMarkers;
    @JsonField(SHOW_DATA_LABELS)
    private boolean showDataLabels;

    public AreaGraph() {
        super();
        setType(TYPE);
    }

    public String getSubType() {
        return this.subType;
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

    public AreaGraph setSubType(String subType) {
        this.subType = subType;
        return this;
    }

    public AreaGraph setLineType(LineType lineType) {
        this.lineType = lineType;
        return this;
    }

    public AreaGraph setShowMarkers(boolean showMarkers) {
        this.showMarkers = showMarkers;
        return this;
    }

    public AreaGraph setShowDataLabels(boolean showDataLabels) {
        this.showDataLabels = showDataLabels;
        return this;
    }

    @Override
    public void appendHighchartsConfig(StringBuilder config) {
        super.appendHighchartsConfig(config);
        boolean inverted = Orientation.HORIZONTAL.equals(getOrientation());
        config.append(", \"chart\": {\"type\": \"" + getChartType() + "\", \"inverted\": " + inverted + "}");

        config.append(", \"plotOptions\": {\"" + getChartType() + "\": {");
        config.append("\"marker\": {\"enabled\": " + isShowMarkers() + "}");
        if ("percentage".equals(getSubType())) {
            config.append(", \"stacking\": \"percent\"");
        } else if ("stacked".equals(getSubType())) {
            config.append(", \"stacking\": \"normal\"");
        }
        config.append(", \"dataLabels\": { \"enabled\": " + isShowDataLabels() + "}");
        config.append(getLineType().getHighchartsPlotOptions(false));
        config.append("}}");
    }

    private String getChartType() {
        if (this.lineType == null) {
            return LineType.SMOOTH.getHighchartsChartType(this);
        }
        return getLineType().getHighchartsChartType(this);
    }

    @Override
    public void mergeFromColumn(AreaGraph graph) {
        super.mergeFromColumn(graph);
        this.subType = graph.getSubType();
        this.lineType = graph.getLineType();
        this.showMarkers = graph.isShowMarkers();
        this.showDataLabels = graph.isShowDataLabels();
    }
}
