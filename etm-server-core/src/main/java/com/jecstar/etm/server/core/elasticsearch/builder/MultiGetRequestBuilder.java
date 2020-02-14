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

import org.elasticsearch.action.get.MultiGetRequest;

public class MultiGetRequestBuilder extends AbstractActionRequestBuilder<MultiGetRequest> {

    private final MultiGetRequest request = new MultiGetRequest();

    public MultiGetRequestBuilder add(String index, String id) {
        this.request.add(index, id);
        return this;
    }

    @Override
    public MultiGetRequest build() {
        return this.request;
    }
}
