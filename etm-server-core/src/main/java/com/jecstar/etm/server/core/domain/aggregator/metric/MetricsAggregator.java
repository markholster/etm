package com.jecstar.etm.server.core.domain.aggregator.metric;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import org.elasticsearch.search.aggregations.AggregationBuilder;

public abstract class MetricsAggregator extends Aggregator {

    public static final String TYPE = "metrics";
    public static final String METRICS_AGGREGATOR_TYPE = "metrics_type";

    @JsonField(METRICS_AGGREGATOR_TYPE)
    private String metricsAggregatorType;

    public MetricsAggregator() {
        super();
        setAggregatorType(TYPE);
    }

    public String getMetricsAggregatorType() {
        return this.metricsAggregatorType;
    }

    protected MetricsAggregator setMetricsAggregatorType(String metricsAggregatorType) {
        this.metricsAggregatorType = metricsAggregatorType;
        return this;
    }

    @Override
    public MetricsAggregator clone() {
        throw new UnsupportedOperationException("Clone method must be implemented in '" + getClass().getName() + ".");
    }

    protected void clone(MetricsAggregator target) {
        super.clone(target);
        target.metricsAggregatorType = this.metricsAggregatorType;
    }

    @Override
    public abstract AggregationBuilder toAggregationBuilder();
}
