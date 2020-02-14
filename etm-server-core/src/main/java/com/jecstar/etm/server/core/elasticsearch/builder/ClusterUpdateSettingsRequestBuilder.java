package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.common.settings.Settings;

public class ClusterUpdateSettingsRequestBuilder extends AbstractActionRequestBuilder<ClusterUpdateSettingsRequest> {

    private final ClusterUpdateSettingsRequest request = new ClusterUpdateSettingsRequest();

    @Override
    public ClusterUpdateSettingsRequest build() {
        return this.request;
    }

    public ClusterUpdateSettingsRequestBuilder setPersistentSettings(Settings.Builder builder) {
        this.request.persistentSettings(builder);
        return this;
    }

    public ClusterUpdateSettingsRequestBuilder setTransientSettings(Settings.Builder builder) {
        this.request.transientSettings(builder);
        return this;
    }

}
