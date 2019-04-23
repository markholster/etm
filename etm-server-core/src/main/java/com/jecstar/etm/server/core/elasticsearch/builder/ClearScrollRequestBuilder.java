package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.search.ClearScrollRequest;

public class ClearScrollRequestBuilder extends AbstractActionRequestBuilder<ClearScrollRequest> {

    private final ClearScrollRequest request = new ClearScrollRequest();

    public ClearScrollRequestBuilder addScrollId(String scrollId) {
        this.request.addScrollId(scrollId);
        return this;
    }

    @Override
    public ClearScrollRequest build() {
        return this.request;
    }
}
