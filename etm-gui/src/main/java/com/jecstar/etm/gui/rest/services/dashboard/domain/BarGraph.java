package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.AxesConverter;
import com.jecstar.etm.server.core.converter.JsonField;

/**
 * A <code>MultiBucketGraph</code> that shows one or more groups of data in a bar chart.
 */
public class BarGraph extends MultiBucketGraph {


    public static final String TYPE = "bar";
    private static final String AXES = "bar";

    @JsonField(value = AXES, converterClass = AxesConverter.class)
    private Axes axes;

    public Axes getAxes() {
        return this.axes;
    }
}
