package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;

public class GetMappingsRequestBuilder extends AbstractBuilder<GetMappingsRequest> {

    private final GetMappingsRequest request = new GetMappingsRequest();

    public GetMappingsRequestBuilder setIndices(String... indices) {
        this.request.indices(indices);
        return this;
    }

    public GetMappingsRequestBuilder addTypes(String... types) {
        this.request.types(types);
        return this;
    }

    @Override
    public GetMappingsRequest build() {
        return this.request;
    }
}
