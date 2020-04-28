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

package com.jecstar.etm.processor.elastic.apm.domain;

import com.jecstar.etm.server.core.converter.JsonField;

/**
 * Any arbitrary contextual information regarding the event, captured by the agent, optionally provided by the user
 */
public class Context {

    @JsonField("custom")
    private java.util.Map<String, Object> custom;
    @JsonField(value = "response", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.ResponseConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Response response;
    @JsonField(value = "request", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.RequestConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Request request;
    @JsonField(value = "tags", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.TagsConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Tags tags;
    @JsonField(value = "user", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.UserConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.User user;
    @JsonField(value = "page", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.PageConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Page page;
    @JsonField(value = "service", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.ServiceConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Service service;
    @JsonField(value = "message", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.MessageConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Message message;

    /**
     * An arbitrary mapping of additional metadata to store with the event.
     */
    public java.util.Map<String, Object> getCustom() {
        return this.custom;
    }

    public com.jecstar.etm.processor.elastic.apm.domain.Response getResponse() {
        return this.response;
    }

    /**
     * If a log record was generated as a result of a http request, the http interface can be used to collect this information.
     */
    public com.jecstar.etm.processor.elastic.apm.domain.Request getRequest() {
        return this.request;
    }

    /**
     * A flat mapping of user-defined tags with string, boolean or number values.
     */
    public com.jecstar.etm.processor.elastic.apm.domain.Tags getTags() {
        return this.tags;
    }

    public com.jecstar.etm.processor.elastic.apm.domain.User getUser() {
        return this.user;
    }

    public com.jecstar.etm.processor.elastic.apm.domain.Page getPage() {
        return this.page;
    }

    public com.jecstar.etm.processor.elastic.apm.domain.Service getService() {
        return this.service;
    }

    /**
     * Details related to message receiving and publishing if the captured event integrates with a messaging system
     */
    public com.jecstar.etm.processor.elastic.apm.domain.Message getMessage() {
        return this.message;
    }
}