package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.common.settings.Settings;

public class UpdateIndexSettingsRequestBuilder extends AbstractBuilder<UpdateSettingsRequest> {

    private final UpdateSettingsRequest request = new UpdateSettingsRequest();

    public UpdateIndexSettingsRequestBuilder setIndices(String... indices) {
        this.request.indices(indices);
        return this;
    }

    public UpdateIndexSettingsRequestBuilder setSettings(Settings.Builder settingsBuilder) {
        this.request.settings(settingsBuilder);
        return this;
    }

    @Override
    public UpdateSettingsRequest build() {
        return this.request;
    }
}
