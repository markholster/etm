package com.jecstar.etm.server.core.domain.aggregator.metric;

import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentilesAggregationBuilder;

public class MedianMetricsAggregator extends FieldBasedMetricsAggregator {

    public static final String TYPE = "median";

    public MedianMetricsAggregator() {
        super();
        setMetricsAggregatorType(TYPE);
    }

    @Override
    public MedianMetricsAggregator clone() {
        MedianMetricsAggregator clone = new MedianMetricsAggregator();
        super.clone(clone);
        return clone;
    }

    @Override
    public PercentilesAggregationBuilder toAggregationBuilder() {
        return AggregationBuilders.percentiles(getId()).setMetaData(getMetadata()).field(getField()).percentiles(50);
    }
}
