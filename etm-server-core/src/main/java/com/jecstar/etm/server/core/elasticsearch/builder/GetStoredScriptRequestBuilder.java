package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.admin.cluster.storedscripts.GetStoredScriptRequest;

public class GetStoredScriptRequestBuilder extends AbstractActionRequestBuilder<GetStoredScriptRequest> {

    private final GetStoredScriptRequest request;

    public GetStoredScriptRequestBuilder(String id) {
        this.request = new GetStoredScriptRequest(id);
    }


    @Override
    public GetStoredScriptRequest build() {
        return this.request;
    }
}
