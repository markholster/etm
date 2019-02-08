package com.jecstar.etm.server.core.domain.aggregator.metric;

import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.sum.SumAggregationBuilder;

public class SumMetricsAggregator extends FieldBasedMetricsAggregator {

    public static final String TYPE = "sum";

    public SumMetricsAggregator() {
        super();
        setMetricsAggregatorType(TYPE);
    }

    @Override
    public SumMetricsAggregator clone() {
        SumMetricsAggregator clone = new SumMetricsAggregator();
        super.clone(clone);
        return clone;
    }

    @Override
    public SumAggregationBuilder toAggregationBuilder() {
        return AggregationBuilders.sum(getId()).setMetaData(getMetadata()).field(getField());
    }
}
