package com.jecstar.etm.gui.rest.services.dashboard.domain.converter;

import com.jecstar.etm.gui.rest.services.dashboard.domain.graph.YAxis;
import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;

public class YAxisConverter extends NestedObjectConverter<YAxis> {

    public YAxisConverter() {
        super(f -> new YAxis());
    }

}
