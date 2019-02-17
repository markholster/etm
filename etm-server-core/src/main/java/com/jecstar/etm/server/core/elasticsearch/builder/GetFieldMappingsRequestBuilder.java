package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsRequest;

public class GetFieldMappingsRequestBuilder extends AbstractBuilder<GetFieldMappingsRequest> {

    private final GetFieldMappingsRequest request = new GetFieldMappingsRequest();

    public GetFieldMappingsRequestBuilder setIndices(String... indices) {
        this.request.indices(indices);
        return this;
    }

    public GetFieldMappingsRequestBuilder setFields(String... fields) {
        this.request.fields(fields);
        return this;
    }

    @Override
    public GetFieldMappingsRequest build() {
        return this.request;
    }
}
