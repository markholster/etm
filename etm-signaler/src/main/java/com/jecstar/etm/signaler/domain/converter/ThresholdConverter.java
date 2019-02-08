package com.jecstar.etm.signaler.domain.converter;

import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;
import com.jecstar.etm.signaler.domain.Threshold;

public class ThresholdConverter extends NestedObjectConverter<Threshold> {

    public ThresholdConverter() {
        super(f -> new Threshold());
    }
}
