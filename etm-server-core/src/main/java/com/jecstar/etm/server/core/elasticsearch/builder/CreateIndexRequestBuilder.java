package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.settings.Settings;

public class CreateIndexRequestBuilder extends AbstractTimedRequestBuilder<CreateIndexRequest> {

    private final CreateIndexRequest request;

    public CreateIndexRequestBuilder(String index) {
        this.request = new CreateIndexRequest(index);
    }

    public CreateIndexRequestBuilder setSettings(Settings.Builder settingsBuilder) {
        this.request.settings(settingsBuilder);
        return this;
    }

    @Override
    public CreateIndexRequest build() {
        return this.request;
    }
}
