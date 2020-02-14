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

import org.elasticsearch.action.admin.cluster.storedscripts.PutStoredScriptRequest;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentType;

public class PutStoredScriptRequestBuilder extends AbstractActionRequestBuilder<PutStoredScriptRequest> {

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
