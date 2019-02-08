package com.jecstar.etm.server.core.domain.aggregator.metric;

import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.avg.AvgAggregationBuilder;

public class AverageMetricsAggregator extends FieldBasedMetricsAggregator {

    public static final String TYPE = "average";

    public AverageMetricsAggregator() {
        super();
        setMetricsAggregatorType(TYPE);
    }

    @Override
    public AverageMetricsAggregator clone() {
        AverageMetricsAggregator clone = new AverageMetricsAggregator();
        super.clone(clone);
        return clone;
    }

    @Override
    public AvgAggregationBuilder toAggregationBuilder() {
        return AggregationBuilders.avg(getId()).setMetaData(getMetadata()).field(getField());
    }

}
