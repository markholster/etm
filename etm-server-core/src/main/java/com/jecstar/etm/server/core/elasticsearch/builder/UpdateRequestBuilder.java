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
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.script.Script;

import java.io.IOException;
import java.util.Map;

public class UpdateRequestBuilder extends AbstractActionRequestBuilder<UpdateRequest> {

    private final UpdateRequest request;
    private final RequestUnitCalculatingStreamOutput requestUnitCalculatingStreamOutput;


    public UpdateRequestBuilder() {
        this(null, null);
    }

    public UpdateRequestBuilder(String index, String id) {
        this.request = new UpdateRequest(index, id);
        this.requestUnitCalculatingStreamOutput = new RequestUnitCalculatingStreamOutput();
    }

    public UpdateRequestBuilder setIndex(String index) {
        this.request.index(index);
        return this;
    }

    public UpdateRequestBuilder setId(String id) {
        this.request.id(id);
        return this;
    }

    public UpdateRequestBuilder setDoc(Map<String, Object> source) {
        this.request.doc(source);
        return this;
    }

    public UpdateRequestBuilder setDoc(String source, XContentType contentType) {
        this.request.doc(source, contentType);
        return this;
    }

    public UpdateRequestBuilder setDocAsUpsert(boolean shouldUpsertDoc) {
        this.request.docAsUpsert(shouldUpsertDoc);
        return this;
    }

    public UpdateRequestBuilder setWaitForActiveShards(ActiveShardCount activeShardCount) {
        this.request.waitForActiveShards(activeShardCount);
        return this;
    }

    public UpdateRequestBuilder setTimeout(TimeValue timeout) {
        this.request.timeout(timeout);
        return this;
    }

    public UpdateRequestBuilder setRetryOnConflict(int retryOnConflict) {
        this.request.retryOnConflict(retryOnConflict);
        return this;
    }

    public UpdateRequestBuilder setDetectNoop(boolean detectNoop) {
        this.request.detectNoop(detectNoop);
        return this;
    }

    public UpdateRequestBuilder setScript(Script script) {
        this.request.script(script);
        return this;
    }

    public UpdateRequestBuilder setScriptedUpsert(boolean scriptedUpsert) {
        this.request.scriptedUpsert(scriptedUpsert);
        return this;
    }

    public UpdateRequestBuilder setUpsert(String source, XContentType xContentType) {
        this.request.upsert(source, xContentType);
        return this;
    }

    public UpdateRequestBuilder setUpsert(IndexRequestBuilder indexRequestBuilder) {
        this.request.upsert(indexRequestBuilder.build());
        return this;
    }

    public UpdateRequestBuilder setIfSeqNo(long seqNo) {
        this.request.setIfSeqNo(seqNo);
        return this;
    }

    public UpdateRequestBuilder setIfPrimaryTerm(long primaryTerm) {
        this.request.setIfPrimaryTerm(primaryTerm);
        return this;
    }

    @Override
    public UpdateRequest build() {
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
