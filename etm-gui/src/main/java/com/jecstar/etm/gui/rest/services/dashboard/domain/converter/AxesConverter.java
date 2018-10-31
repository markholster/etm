package com.jecstar.etm.gui.rest.services.dashboard.domain.converter;

import com.jecstar.etm.gui.rest.services.dashboard.domain.Axes;
import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;

public class AxesConverter extends NestedObjectConverter<Axes> {

    public AxesConverter() {
        super(f -> new Axes());
    }

}
