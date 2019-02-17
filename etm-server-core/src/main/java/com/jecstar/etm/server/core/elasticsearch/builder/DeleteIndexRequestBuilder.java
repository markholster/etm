package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;

public class DeleteIndexRequestBuilder extends AbstractBuilder<DeleteIndexRequest> {

    private final DeleteIndexRequest request = new DeleteIndexRequest();

    public DeleteIndexRequestBuilder setIndices(String... indices) {
        this.request.indices(indices);
        return this;
    }

    @Override
    public DeleteIndexRequest build() {
        return this.request;
    }
}
