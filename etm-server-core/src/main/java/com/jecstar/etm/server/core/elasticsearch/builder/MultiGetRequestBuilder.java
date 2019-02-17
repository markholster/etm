package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.get.MultiGetRequest;

public class MultiGetRequestBuilder extends AbstractBuilder<MultiGetRequest> {

    private final MultiGetRequest request = new MultiGetRequest();

    public MultiGetRequestBuilder add(String index, String type, String id) {
        this.request.add(index, type, id);
        return this;
    }

    @Override
    public MultiGetRequest build() {
        return this.request;
    }
}
