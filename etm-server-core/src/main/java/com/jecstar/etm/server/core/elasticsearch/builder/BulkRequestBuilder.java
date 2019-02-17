package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.update.UpdateRequest;

public class BulkRequestBuilder extends AbstractBuilder<BulkRequest> {

    private final BulkRequest request = new BulkRequest();

    public BulkRequestBuilder setWaitForActiveShards(ActiveShardCount activeShardCount) {
        this.request.waitForActiveShards(activeShardCount);
        return this;
    }

    public BulkRequestBuilder add(DeleteRequest deleteRequest) {
        this.request.add(deleteRequest);
        return this;
    }

    public BulkRequestBuilder add(UpdateRequest updateRequest) {
        this.request.add(updateRequest);
        return this;
    }

    public int getNumberOfActions() {
        return this.request.numberOfActions();
    }

    @Override
    public BulkRequest build() {
        return this.request;
    }
}
