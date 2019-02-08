package com.jecstar.etm.gui.rest.services.dashboard.domain.converter;

import com.jecstar.etm.gui.rest.services.dashboard.domain.Data;
import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;

public class DataConverter extends NestedObjectConverter<Data> {

    public DataConverter() {
        super(f -> new Data());
    }
}
