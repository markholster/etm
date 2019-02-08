package com.jecstar.etm.server.core.domain.aggregator.metric;

import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.max.MaxAggregationBuilder;

public class MaxMetricsAggregator extends FieldBasedMetricsAggregator {

    public static final String TYPE = "max";

    public MaxMetricsAggregator() {
        super();
        setMetricsAggregatorType(TYPE);
    }

    @Override
    public MaxMetricsAggregator clone() {
        MaxMetricsAggregator clone = new MaxMetricsAggregator();
        super.clone(clone);
        return clone;
    }

    @Override
    public MaxAggregationBuilder toAggregationBuilder() {
        return AggregationBuilders.max(getId()).setMetaData(getMetadata()).field(getField());
    }
}
