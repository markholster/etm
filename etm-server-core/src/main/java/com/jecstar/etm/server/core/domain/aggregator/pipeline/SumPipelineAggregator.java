package com.jecstar.etm.server.core.domain.aggregator.pipeline;

import org.elasticsearch.search.aggregations.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.pipeline.SumBucketPipelineAggregationBuilder;

public class SumPipelineAggregator extends PathBasedPipelineAggregator {

    public static final String TYPE = "sum";

    public SumPipelineAggregator() {
        super();
        setPipelineAggregatorType(TYPE);
    }

    @Override
    public SumPipelineAggregator clone() {
        SumPipelineAggregator clone = new SumPipelineAggregator();
        super.clone(clone);
        return clone;
    }

    @Override
    public SumBucketPipelineAggregationBuilder toAggregationBuilder() {
        return PipelineAggregatorBuilders.sumBucket(getId(), getPath()).setMetaData(getMetadata());
    }
}
