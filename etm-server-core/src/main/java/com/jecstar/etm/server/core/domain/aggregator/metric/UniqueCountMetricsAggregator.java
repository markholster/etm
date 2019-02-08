package com.jecstar.etm.server.core.domain.aggregator.metric;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

public class UniqueCountMetricsAggregator extends FieldBasedMetricsAggregator {

    public static final String TYPE = "cardinality";

    public UniqueCountMetricsAggregator() {
        super();
        setMetricsAggregatorType(TYPE);
    }

    @Override
    public UniqueCountMetricsAggregator clone() {
        UniqueCountMetricsAggregator clone = new UniqueCountMetricsAggregator();
        super.clone(clone);
        return clone;
    }

    @Override
    public AggregationBuilder toAggregationBuilder() {
        return AggregationBuilders.cardinality(getId()).setMetaData(getMetadata()).field(getField());
    }
}
