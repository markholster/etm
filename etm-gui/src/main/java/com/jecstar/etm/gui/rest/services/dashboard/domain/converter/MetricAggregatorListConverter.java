package com.jecstar.etm.gui.rest.services.dashboard.domain.converter;

import com.jecstar.etm.gui.rest.services.dashboard.domain.MetricAggregator;
import com.jecstar.etm.server.core.converter.custom.NestedListObjectConverter;

import java.util.List;

public class MetricAggregatorListConverter extends NestedListObjectConverter<MetricAggregator, List<MetricAggregator>> {

    public MetricAggregatorListConverter() {
        super(new MetricAggregatorConverter());
    }
}
