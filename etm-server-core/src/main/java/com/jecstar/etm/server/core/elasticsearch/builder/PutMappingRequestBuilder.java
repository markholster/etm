package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.common.xcontent.XContentType;

public class PutMappingRequestBuilder extends AbstractTimedRequestBuilder<PutMappingRequest> {

    private final PutMappingRequest request;

    public PutMappingRequestBuilder(String... indices) {
        this.request = new PutMappingRequest(indices);
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
