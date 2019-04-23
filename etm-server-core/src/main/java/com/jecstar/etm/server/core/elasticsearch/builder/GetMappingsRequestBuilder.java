package com.jecstar.etm.server.core.elasticsearch.builder;


import org.elasticsearch.client.indices.GetMappingsRequest;

public class GetMappingsRequestBuilder extends AbstractTimedRequestBuilder<GetMappingsRequest> {

    private final GetMappingsRequest request = new GetMappingsRequest();

    public GetMappingsRequestBuilder setIndices(String... indices) {
        this.request.indices(indices);
        return this;
    }

    @Override
    public GetMappingsRequest build() {
        return this.request;
    }
}
