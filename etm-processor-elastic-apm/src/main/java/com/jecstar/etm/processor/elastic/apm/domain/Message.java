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
 * Details related to message receiving and publishing if the captured event integrates with a messaging system
 */
public class Message {

    @JsonField(value = "queue", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.QueueConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Queue queue;
    @JsonField(value = "age", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.AgeConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Age age;
    @JsonField("body")
    private String body;

    public com.jecstar.etm.processor.elastic.apm.domain.Queue getQueue() {
        return this.queue;
    }

    public com.jecstar.etm.processor.elastic.apm.domain.Age getAge() {
        return this.age;
    }

    /**
     * messsage body, similar to an http request body
     */
    public String getBody() {
        return this.body;
    }
}