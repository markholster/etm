package com.jecstar.etm.server.core.domain.aggregator.pipeline;

import org.elasticsearch.search.aggregations.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.pipeline.DerivativePipelineAggregationBuilder;

public class DerivativePipelineAggregator extends PathBasedPipelineAggregator {

    public static final String TYPE = "derivative";

    public DerivativePipelineAggregator() {
        super();
        setPipelineAggregatorType(TYPE);
    }

    @Override
    public DerivativePipelineAggregator clone() {
        DerivativePipelineAggregator clone = new DerivativePipelineAggregator();
        super.clone(clone);
        return clone;
    }

    @Override
    public DerivativePipelineAggregationBuilder toAggregationBuilder() {
        return PipelineAggregatorBuilders.derivative(getId(), getPath()).setMetaData(getMetadata());
    }
}
