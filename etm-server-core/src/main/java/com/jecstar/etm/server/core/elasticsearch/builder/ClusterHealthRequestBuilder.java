package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.common.unit.TimeValue;

public class ClusterHealthRequestBuilder extends AbstractBuilder<ClusterHealthRequest> {

    private final ClusterHealthRequest request = new ClusterHealthRequest();

    public ClusterHealthRequestBuilder setTimeout(TimeValue timeValue) {
        this.request.timeout(timeValue);
        return this;
    }

    @Override
    public ClusterHealthRequest build() {
        return this.request;
    }
}
