package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.common.xcontent.XContentType;

public class PutMappingRequestBuilder extends AbstractBuilder<PutMappingRequest> {

    private final PutMappingRequest request = new PutMappingRequest();

    public PutMappingRequestBuilder setIndices(String... indices) {
        this.request.indices(indices);
        return this;
    }

    public PutMappingRequestBuilder setType(String type) {
        this.request.type(type);
        return this;
    }

    public PutMappingRequestBuilder setSource(String content, XContentType contentType) {
        this.request.source(content, contentType);
        return this;
    }

    @Override
    public PutMappingRequest build() {
        return this.request;
    }
}
