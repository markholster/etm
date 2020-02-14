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

import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;

import java.util.List;

public class PutIndexTemplateRequestBuilder extends AbstractActionRequestBuilder<PutIndexTemplateRequest> {

    private final PutIndexTemplateRequest request;

    public PutIndexTemplateRequestBuilder(String name) {
        this.request = new PutIndexTemplateRequest(name);
    }

    public PutIndexTemplateRequestBuilder setCreate(boolean create) {
        this.request.create(create);
        return this;
    }

    public PutIndexTemplateRequestBuilder setPatterns(List<String> indexPatterns) {
        this.request.patterns(indexPatterns);
        return this;
    }

    public PutIndexTemplateRequestBuilder setSettings(Settings.Builder settingsBuilder) {
        this.request.settings(settingsBuilder);
        return this;
    }

    public PutIndexTemplateRequestBuilder setMapping(String source, XContentType contentType) {
        this.request.mapping(source, contentType);
        return this;
    }

    public PutIndexTemplateRequestBuilder setAlias(Alias alias) {
        this.request.alias(alias);
        return this;
    }

    public PutIndexTemplateRequestBuilder setOrder(int order) {
        this.request.order(order);
        return this;
    }

    @Override
    public PutIndexTemplateRequest build() {
        return this.request;
    }
}
