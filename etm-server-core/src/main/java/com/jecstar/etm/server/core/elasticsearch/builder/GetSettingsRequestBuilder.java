package com.jecstar.etm.server.core.elasticsearch.builder;


import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;

public class GetSettingsRequestBuilder extends AbstractActionRequestBuilder<GetSettingsRequest> {

    private final GetSettingsRequest request = new GetSettingsRequest();

    public GetSettingsRequestBuilder setIndices(String... indices) {
        this.request.indices(indices);
        return this;
    }

    @Override
    public GetSettingsRequest build() {
        return this.request;
    }
}
