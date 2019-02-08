package com.jecstar.etm.server.core.domain.aggregator.pipeline;

import org.elasticsearch.search.aggregations.pipeline.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.percentile.PercentilesBucketPipelineAggregationBuilder;

public class MedianPipelineAggregator extends PathBasedPipelineAggregator {

    public static final String TYPE = "median";

    public MedianPipelineAggregator() {
        super();
        setPipelineAggregatorType(TYPE);
    }

    @Override
    public MedianPipelineAggregator clone() {
        MedianPipelineAggregator clone = new MedianPipelineAggregator();
        super.clone(clone);
        return clone;
    }

    @Override
    public PercentilesBucketPipelineAggregationBuilder toAggregationBuilder() {
        return PipelineAggregatorBuilders.percentilesBucket(getId(), getPath()).setMetaData(getMetadata()).percents(new double[]{50});
    }
}
