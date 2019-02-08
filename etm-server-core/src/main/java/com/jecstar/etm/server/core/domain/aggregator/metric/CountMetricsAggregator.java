package com.jecstar.etm.server.core.domain.aggregator.metric;

import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.valuecount.ValueCountAggregationBuilder;

public class CountMetricsAggregator extends MetricsAggregator {

    public static final String TYPE = "count";

    public CountMetricsAggregator() {
        super();
        setMetricsAggregatorType(TYPE);
    }

    @Override
    public CountMetricsAggregator clone() {
        CountMetricsAggregator clone = new CountMetricsAggregator();
        super.clone(clone);
        return clone;
    }

    @Override
    public ValueCountAggregationBuilder toAggregationBuilder() {
        return AggregationBuilders.count(getId()).setMetaData(getMetadata()).field("_id");
    }
}
