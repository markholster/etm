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

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.persisting.elastic.RequestUnitCalculatingStreamOutput;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.util.Map;

public class IndexRequestBuilder extends AbstractActionRequestBuilder<IndexRequest> {

    private final IndexRequest request;
    private final RequestUnitCalculatingStreamOutput requestUnitCalculatingStreamOutput;

    public IndexRequestBuilder() {
        this(null);
    }

    public IndexRequestBuilder(String index) {
        this(index, null);
    }

    public IndexRequestBuilder(String index, String id) {
        this.request = new IndexRequest(index).id(id);
        this.requestUnitCalculatingStreamOutput = new RequestUnitCalculatingStreamOutput();
    }

    public IndexRequestBuilder setIndex(String index) {
        this.request.index(index);
        return this;
    }

    public IndexRequestBuilder setId(String id) {
        this.request.id(id);
        return this;
    }

    public IndexRequestBuilder setSource(Map<String, Object> source) {
        this.request.source(source);
        return this;
    }

    public IndexRequestBuilder setSource(String source, XContentType contentType) {
        this.request.source(source, contentType);
        return this;
    }

    public IndexRequestBuilder setWaitForActiveShards(ActiveShardCount activeShardCount) {
        this.request.waitForActiveShards(activeShardCount);
        return this;
    }

    public IndexRequestBuilder setTimeout(TimeValue timeValue) {
        this.request.timeout(timeValue);
        return this;
    }

    @Override
    public IndexRequest build() {
        return this.request;
    }

    /**
     * Calculate the number of request units this request would consume.
     *
     * @return The number of request units.
     */
    public double calculateIndexRequestUnits() {
        try {
            this.requestUnitCalculatingStreamOutput.reset();
            this.request.writeTo(this.requestUnitCalculatingStreamOutput);
            return this.requestUnitCalculatingStreamOutput.getRequestUnits();
        } catch (IOException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }
}
