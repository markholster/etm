package com.jecstar.etm.gui.rest.services.dashboard.domain.graph;

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
    public void appendHighchartsConfig(StringBuilder config) {
        super.appendHighchartsConfig(config);
        boolean inverted = Orientation.HORIZONTAL.equals(getOrientation());
        config.append(", \"chart\": {\"type\": \"scatter\", \"inverted\": " + inverted + "}");
        config.append(", \"plotOptions\": {\"scatter\": {\"dataLabels\": { \"enabled\": " + isShowDataLabels() + "}");
        config.append("}}");
    }

    @Override
    public void mergeFromColumn(ScatterGraph graph) {
        super.mergeFromColumn(graph);
        this.showDataLabels = graph.isShowDataLabels();
    }
}
