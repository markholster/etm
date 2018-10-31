package com.jecstar.etm.gui.rest.services.dashboard.domain.converter;

import com.jecstar.etm.gui.rest.services.dashboard.domain.MetricAggregator;
import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;

public class MetricAggregatorConverter extends NestedObjectConverter<MetricAggregator> {

    public MetricAggregatorConverter() {
        super(f -> new MetricAggregator());
    }

}
