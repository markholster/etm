package com.jecstar.etm.server.core.domain.aggregator.metric;

import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.MedianAbsoluteDeviationAggregationBuilder;

public class MedianAbsoluteDeviationMetricsAggregator extends FieldBasedMetricsAggregator {

    public static final String TYPE = "median_absolute_deviation";

    public MedianAbsoluteDeviationMetricsAggregator() {
        super();
        setMetricsAggregatorType(TYPE);
    }

    @Override
    public MedianAbsoluteDeviationMetricsAggregator clone() {
        MedianAbsoluteDeviationMetricsAggregator clone = new MedianAbsoluteDeviationMetricsAggregator();
        super.clone(clone);
        return clone;
    }

    @Override
    public MedianAbsoluteDeviationAggregationBuilder toAggregationBuilder() {
        return AggregationBuilders.medianAbsoluteDeviation(getId()).setMetaData(getMetadata()).field(getField());
    }
}
