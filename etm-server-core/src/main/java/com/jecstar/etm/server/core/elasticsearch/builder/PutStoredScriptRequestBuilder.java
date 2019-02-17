package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.admin.cluster.storedscripts.PutStoredScriptRequest;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentType;

public class PutStoredScriptRequestBuilder extends AbstractBuilder<PutStoredScriptRequest> {

    private final PutStoredScriptRequest request = new PutStoredScriptRequest();

    public PutStoredScriptRequestBuilder setId(String id) {
        this.request.id(id);
        return this;
    }

    public PutStoredScriptRequestBuilder setContent(BytesReference content, XContentType contentType) {
        this.request.content(content, contentType);
        return this;
    }

    @Override
    public PutStoredScriptRequest build() {
        return this.request;
    }
}
