package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.admin.indices.flush.FlushRequest;

public class FlushIndexRequestBuilder extends AbstractBuilder<FlushRequest> {

    private final FlushRequest request = new FlushRequest();

    public FlushIndexRequestBuilder setIndices(String... indices) {
        this.request.indices(indices);
        return this;
    }

    public FlushIndexRequestBuilder setForce(boolean force) {
        this.request.force(force);
        return this;
    }

    @Override
    public FlushRequest build() {
        return this.request;
    }
}
