package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.client.indices.GetIndexTemplatesRequest;

public class GetIndexTemplateRequestBuilder {

    private final GetIndexTemplatesRequest request;

    public GetIndexTemplateRequestBuilder(String... names) {
        this.request = new GetIndexTemplatesRequest(names);
    }

    public GetIndexTemplatesRequest build() {
        return this.request;
    }
}
