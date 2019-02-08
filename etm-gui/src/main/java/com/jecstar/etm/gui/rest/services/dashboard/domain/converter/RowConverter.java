package com.jecstar.etm.gui.rest.services.dashboard.domain.converter;

import com.jecstar.etm.gui.rest.services.dashboard.domain.Row;
import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;

public class RowConverter extends NestedObjectConverter<Row> {

    public RowConverter() {
        super(f -> new Row());
    }

}
