package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.common.settings.Settings;

public class CreateIndexRequestBuilder extends AbstractBuilder<CreateIndexRequest> {

    private final CreateIndexRequest request = new CreateIndexRequest();

    public CreateIndexRequestBuilder setIndex(String index) {
        this.request.index(index);
        return this;
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
