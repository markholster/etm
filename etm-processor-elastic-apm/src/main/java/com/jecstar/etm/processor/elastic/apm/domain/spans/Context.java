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

package com.jecstar.etm.processor.elastic.apm.domain.spans;

import com.jecstar.etm.server.core.converter.JsonField;

/**
 * Any other arbitrary data captured by the agent, optionally provided by the user
 */
public class Context {

    @JsonField(value = "destination", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.spans.DestinationConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.spans.Destination destination;
    @JsonField(value = "db", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.spans.DbConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.spans.Db db;
    @JsonField(value = "http", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.spans.HttpConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.spans.Http http;
    @JsonField(value = "tags", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.TagsConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Tags tags;
    @JsonField(value = "message", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.MessageConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Message message;

    /**
     * An object containing contextual data about the destination for spans
     */
    public com.jecstar.etm.processor.elastic.apm.domain.spans.Destination getDestination() {
        return this.destination;
    }

    /**
     * An object containing contextual data for database spans
     */
    public com.jecstar.etm.processor.elastic.apm.domain.spans.Db getDb() {
        return this.db;
    }

    /**
     * An object containing contextual data of the related http request.
     */
    public com.jecstar.etm.processor.elastic.apm.domain.spans.Http getHttp() {
        return this.http;
    }

    /**
     * A flat mapping of user-defined tags with string, boolean or number values.
     */
    public com.jecstar.etm.processor.elastic.apm.domain.Tags getTags() {
        return this.tags;
    }

    /**
     * Details related to message receiving and publishing if the captured event integrates with a messaging system
     */
    public com.jecstar.etm.processor.elastic.apm.domain.Message getMessage() {
        return this.message;
    }
}