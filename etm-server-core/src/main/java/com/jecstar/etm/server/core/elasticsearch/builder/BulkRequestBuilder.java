/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.update.UpdateRequest;

public class BulkRequestBuilder extends AbstractActionRequestBuilder<BulkRequest> {

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
