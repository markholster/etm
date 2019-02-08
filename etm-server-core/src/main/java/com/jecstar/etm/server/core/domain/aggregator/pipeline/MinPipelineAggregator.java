package com.jecstar.etm.server.core.domain.aggregator.pipeline;

import org.elasticsearch.search.aggregations.pipeline.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.min.MinBucketPipelineAggregationBuilder;

public class MinPipelineAggregator extends PathBasedPipelineAggregator {

    public static final String TYPE = "min";

    public MinPipelineAggregator() {
        super();
        setPipelineAggregatorType(TYPE);
    }

    @Override
    public MinPipelineAggregator clone() {
        MinPipelineAggregator clone = new MinPipelineAggregator();
        super.clone(clone);
        return clone;
    }

    @Override
    public MinBucketPipelineAggregationBuilder toAggregationBuilder() {
        return PipelineAggregatorBuilders.minBucket(getId(), getPath()).setMetaData(getMetadata());
    }
}
