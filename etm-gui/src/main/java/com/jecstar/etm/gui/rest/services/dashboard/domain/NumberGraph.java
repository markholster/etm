package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.MetricAggregatorConverter;
import com.jecstar.etm.server.core.converter.JsonField;

public class NumberGraph extends Graph {


    public static final String TYPE = "number";
    private static final String METRIC_AGGREGATOR = "number";

    @JsonField(value = METRIC_AGGREGATOR, converterClass = MetricAggregatorConverter.class)
    private MetricAggregator metricAggregator;

    public MetricAggregator getMetricAggregator() {
        return this.metricAggregator;
    }
}
