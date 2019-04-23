package com.jecstar.etm.server.core.elasticsearch.builder;


import org.elasticsearch.client.indices.GetIndexRequest;

public class GetIndexRequestBuilder extends AbstractTimedRequestBuilder<GetIndexRequest> {

    private final GetIndexRequest request;

    public GetIndexRequestBuilder(String... indices) {
        this.request = new GetIndexRequest(indices);
    }

    @Override
    public GetIndexRequest build() {
        return this.request;
    }
}
