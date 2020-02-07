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
