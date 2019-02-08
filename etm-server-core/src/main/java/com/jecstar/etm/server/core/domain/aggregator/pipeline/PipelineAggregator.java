package com.jecstar.etm.server.core.domain.aggregator.pipeline;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import org.elasticsearch.search.aggregations.PipelineAggregationBuilder;

public abstract class PipelineAggregator extends Aggregator {

    public static final String TYPE = "pipeline";
    public static final String PIPELINE_AGGREGATOR_TYPE = "pipeline_type";

    @JsonField(PIPELINE_AGGREGATOR_TYPE)
    private String pipelineAggregatorType;

    public PipelineAggregator() {
        super();
        setAggregatorType(TYPE);
    }

    public String getPipelineAggregatorType() {
        return this.pipelineAggregatorType;
    }

    protected PipelineAggregator setPipelineAggregatorType(String pipelineAggregatorType) {
        this.pipelineAggregatorType = pipelineAggregatorType;
        return this;
    }

    @Override
    public PipelineAggregator clone() {
        throw new UnsupportedOperationException("Clone method must be implemented in '" + getClass().getName() + ".");
    }

    protected void clone(PipelineAggregator target) {
        super.clone(target);
        target.pipelineAggregatorType = this.pipelineAggregatorType;
    }

    @Override
    public abstract PipelineAggregationBuilder toAggregationBuilder();
}
