package com.jecstar.etm.gui.rest.services.dashboard.domain.converter;

import com.jecstar.etm.gui.rest.services.dashboard.domain.Column;
import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;

public class ColumnConverter extends NestedObjectConverter<Column> {

    public ColumnConverter() {
        super(f -> new Column());
    }

}
