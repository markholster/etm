package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.admin.indices.get.GetIndexRequest;

public class GetIndexRequestBuilder extends AbstractBuilder<GetIndexRequest> {

    private final GetIndexRequest request = new GetIndexRequest();

    public GetIndexRequestBuilder setIndices(String... indices) {
        this.request.indices(indices);
        return this;
    }

    @Override
    public GetIndexRequest build() {
        return this.request;
    }
}
