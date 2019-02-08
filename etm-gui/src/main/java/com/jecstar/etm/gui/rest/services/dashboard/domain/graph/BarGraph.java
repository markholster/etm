package com.jecstar.etm.gui.rest.services.dashboard.domain.graph;

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
    public void appendHighchartsConfig(StringBuilder config) {
        super.appendHighchartsConfig(config);
        String chartType = Orientation.HORIZONTAL.equals(getOrientation()) ? "bar" : "column";
        config.append(", \"chart\": {\"type\": \"" + chartType + "\"}");

        config.append(", \"plotOptions\": {\"" + chartType + "\": {");
        if ("percentage".equals(getSubType())) {
            config.append("\"stacking\": \"percent\"");
        } else if ("stacked".equals(getSubType())) {
            config.append("\"stacking\": \"normal\"");
        }
        config.append("}}");
    }

    @Override
    public void mergeFromColumn(BarGraph graph) {
        super.mergeFromColumn(graph);
        this.subType = graph.getSubType();
    }
}
