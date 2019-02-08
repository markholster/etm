package com.jecstar.etm.server.core.domain.aggregator.metric;

import com.jecstar.etm.server.core.converter.JsonField;

public abstract class FieldBasedMetricsAggregator extends MetricsAggregator {

    private static final String FIELD = "field";

    @JsonField(FIELD)
    private String field;

    public String getField() {
        return this.field;
    }

    public FieldBasedMetricsAggregator setField(String field) {
        this.field = field;
        return this;
    }

    @Override
    public FieldBasedMetricsAggregator clone() {
        throw new UnsupportedOperationException("Clone method must be implemented in '" + getClass().getName() + ".");
    }

    protected void clone(FieldBasedMetricsAggregator target) {
        super.clone(target);
        target.field = this.field;
    }
}
