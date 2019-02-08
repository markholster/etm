package com.jecstar.etm.server.core.domain.aggregator.pipeline;

import org.elasticsearch.search.aggregations.pipeline.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.max.MaxBucketPipelineAggregationBuilder;

public class MaxPipelineAggregator extends PathBasedPipelineAggregator {

    public static final String TYPE = "max";

    public MaxPipelineAggregator() {
        super();
        setPipelineAggregatorType(TYPE);
    }

    @Override
    public MaxPipelineAggregator clone() {
        MaxPipelineAggregator clone = new MaxPipelineAggregator();
        super.clone(clone);
        return clone;
    }

    @Override
    public MaxBucketPipelineAggregationBuilder toAggregationBuilder() {
        return PipelineAggregatorBuilders.maxBucket(getId(), getPath()).setMetaData(getMetadata());
    }
}
