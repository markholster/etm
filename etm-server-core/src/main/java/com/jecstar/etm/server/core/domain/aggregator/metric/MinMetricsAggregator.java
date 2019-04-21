package com.jecstar.etm.server.core.domain.aggregator.metric;

import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.MinAggregationBuilder;

public class MinMetricsAggregator extends FieldBasedMetricsAggregator {

    public static final String TYPE = "min";

    public MinMetricsAggregator() {
        super();
        setMetricsAggregatorType(TYPE);
    }

    @Override
    public MinMetricsAggregator clone() {
        MinMetricsAggregator clone = new MinMetricsAggregator();
        super.clone(clone);
        return clone;
    }

    @Override
    public MinAggregationBuilder toAggregationBuilder() {
        return AggregationBuilders.min(getId()).setMetaData(getMetadata()).field(getField());
    }

}
