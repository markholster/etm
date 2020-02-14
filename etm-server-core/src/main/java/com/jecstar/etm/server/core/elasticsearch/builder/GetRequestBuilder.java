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

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;

public class GetRequestBuilder extends AbstractActionRequestBuilder<GetRequest> {

    private final GetRequest request;

    public GetRequestBuilder(String index, String id) {
        this.request = new GetRequest(index, id);
    }

    public GetRequestBuilder setFetchSource(boolean fetch) {
        this.request.fetchSourceContext(new FetchSourceContext(fetch));
        return this;
    }

    public GetRequestBuilder setFetchSource(String include, String exclude) {
        this.request.fetchSourceContext(new FetchSourceContext(true, new String[]{include}, new String[]{exclude}));
        return this;
    }

    public GetRequestBuilder setFetchSource(String[] includes, String[] excludes) {
        this.request.fetchSourceContext(new FetchSourceContext(true, includes, excludes));
        return this;
    }

    @Override
    public GetRequest build() {
        return this.request;
    }
}
