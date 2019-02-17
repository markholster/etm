package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.common.unit.TimeValue;

public class DeleteRequestBuilder extends AbstractBuilder<DeleteRequest> {

    private final DeleteRequest request;

    public DeleteRequestBuilder(String index, String type, String id) {
        this.request = new DeleteRequest(index, type, id);
    }

    public DeleteRequestBuilder setWaitForActiveShards(ActiveShardCount activeShardCount) {
        this.request.waitForActiveShards(activeShardCount);
        return this;
    }

    public DeleteRequestBuilder setTimeout(TimeValue timeValue) {
        this.request.timeout(timeValue);
        return this;
    }

    @Override
    public DeleteRequest build() {
        return this.request;
    }
}
