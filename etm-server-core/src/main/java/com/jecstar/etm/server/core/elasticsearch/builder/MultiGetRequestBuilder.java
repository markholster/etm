package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.get.MultiGetRequest;

public class MultiGetRequestBuilder extends AbstractActionRequestBuilder<MultiGetRequest> {

    private final MultiGetRequest request = new MultiGetRequest();

    public MultiGetRequestBuilder add(String index, String id) {
        this.request.add(index, id);
        return this;
    }

    @Override
    public MultiGetRequest build() {
        return this.request;
    }
}
