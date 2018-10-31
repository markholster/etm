package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.XAxisConverter;
import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.YAxisConverter;
import com.jecstar.etm.server.core.converter.JsonField;

public class Axes {

    private static final String X_AXIS = "x_axis";
    private static final String Y_AXIS = "y_axis";

    @JsonField(value = X_AXIS, converterClass = XAxisConverter.class)
    private XAxis xAxis;

    @JsonField(value = Y_AXIS, converterClass = YAxisConverter.class)
    private YAxis yAxis;

    public XAxis getXAxis() {
        return this.xAxis;
    }

    public YAxis getYAxis() {
        return this.yAxis;
    }

}
