package com.jecstar.etm.server.core.domain.aggregator.pipeline;

import com.jecstar.etm.server.core.converter.JsonField;

public abstract class PathBasedPipelineAggregator extends PipelineAggregator {

    private static final String PATH = "path";

    @JsonField(PATH)
    private String path;

    public String getPath() {
        return this.path;
    }

    public PathBasedPipelineAggregator setPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public PathBasedPipelineAggregator clone() {
        throw new UnsupportedOperationException("Clone method must be implemented in '" + getClass().getName() + ".");
    }

    protected void clone(PathBasedPipelineAggregator target) {
        super.clone(target);
        target.path = this.path;
    }
}
