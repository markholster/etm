package com.jecstar.etm.server.core.domain.aggregator.pipeline;

import org.elasticsearch.search.aggregations.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.pipeline.AvgBucketPipelineAggregationBuilder;

public class AveragePipelineAggregator extends PathBasedPipelineAggregator {

    public static final String TYPE = "average";

    public AveragePipelineAggregator() {
        super();
        setPipelineAggregatorType(TYPE);
    }

    @Override
    public AveragePipelineAggregator clone() {
        AveragePipelineAggregator clone = new AveragePipelineAggregator();
        super.clone(clone);
        return clone;
    }

    @Override
    public AvgBucketPipelineAggregationBuilder toAggregationBuilder() {
        return PipelineAggregatorBuilders.avgBucket(getId(), getPath()).setMetaData(getMetadata());
    }
}
