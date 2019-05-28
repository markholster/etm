package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.script.Script;

import java.util.Map;

public class UpdateRequestBuilder extends AbstractActionRequestBuilder<UpdateRequest> {

    private final UpdateRequest request;

    public UpdateRequestBuilder() {
        this.request = new UpdateRequest();
    }

    public UpdateRequestBuilder(String index, String id) {
        this.request = new UpdateRequest(index, id);
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
}
