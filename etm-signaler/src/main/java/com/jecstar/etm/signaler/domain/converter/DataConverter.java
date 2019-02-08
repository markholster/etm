package com.jecstar.etm.signaler.domain.converter;

import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;
import com.jecstar.etm.signaler.domain.Data;

public class DataConverter extends NestedObjectConverter<Data> {

    public DataConverter() {
        super(f -> new Data());
    }
}
