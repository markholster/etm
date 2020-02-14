package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsRequest;

public class ClusterGetSettingsRequestBuilder extends AbstractActionRequestBuilder<ClusterGetSettingsRequest> {

    private final ClusterGetSettingsRequest request = new ClusterGetSettingsRequest();

    @Override
    public ClusterGetSettingsRequest build() {
        return this.request;
    }

}
