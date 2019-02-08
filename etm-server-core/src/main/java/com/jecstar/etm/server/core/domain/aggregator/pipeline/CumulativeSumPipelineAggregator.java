package com.jecstar.etm.server.core.domain.aggregator.pipeline;


import org.elasticsearch.search.aggregations.pipeline.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.pipeline.cumulativesum.CumulativeSumPipelineAggregationBuilder;

public class CumulativeSumPipelineAggregator extends PathBasedPipelineAggregator {

    public static final String TYPE = "cumulative_sum";

    public CumulativeSumPipelineAggregator() {
        super();
        setPipelineAggregatorType(TYPE);
    }

    @Override
    public CumulativeSumPipelineAggregator clone() {
        CumulativeSumPipelineAggregator clone = new CumulativeSumPipelineAggregator();
        super.clone(clone);
        return clone;
    }

    @Override
    public CumulativeSumPipelineAggregationBuilder toAggregationBuilder() {
        return PipelineAggregatorBuilders.cumulativeSum(getId(), getPath()).setMetaData(getMetadata());
    }
}
