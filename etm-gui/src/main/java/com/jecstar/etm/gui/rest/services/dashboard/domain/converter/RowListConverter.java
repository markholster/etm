package com.jecstar.etm.gui.rest.services.dashboard.domain.converter;

import com.jecstar.etm.gui.rest.services.dashboard.domain.Row;
import com.jecstar.etm.server.core.converter.custom.NestedListObjectConverter;

import java.util.List;

public class RowListConverter extends NestedListObjectConverter<Row, List<Row>> {

    public RowListConverter() {
        super(new RowConverter());
    }
}
