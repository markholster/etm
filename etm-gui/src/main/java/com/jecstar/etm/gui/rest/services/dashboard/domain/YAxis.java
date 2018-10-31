package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.MetricAggregatorListConverter;
import com.jecstar.etm.server.core.converter.JsonField;

import java.util.List;

public class YAxis {

    private static final String AGGREGATORS = "aggregators";
    private static final String FORMAT = "format";

    @JsonField(value = AGGREGATORS, converterClass = MetricAggregatorListConverter.class)
    private List<MetricAggregator> metricAggregators;
    @JsonField(FORMAT)
    private String format;

    public List<MetricAggregator> getMetricAggregators() {
        return this.metricAggregators;
    }

    public String getFormat() {
        return this.format;
    }
}
