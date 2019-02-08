package com.jecstar.etm.gui.rest.services.dashboard.domain.converter;

import com.jecstar.etm.gui.rest.services.dashboard.domain.Column;
import com.jecstar.etm.server.core.converter.custom.NestedListObjectConverter;

import java.util.List;

public class ColumnListConverter extends NestedListObjectConverter<Column, List<Column>> {

    public ColumnListConverter() {
        super(new ColumnConverter());
    }
}
