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
 * Destination service context
 */
public class Service {

    @JsonField("type")
    private String type;
    @JsonField("name")
    private String name;
    @JsonField("resource")
    private String resource;

    /**
     * Type of the destination service (e.g. 'db', 'elasticsearch'). Should typically be the same as span.type.
     */
    public String getType() {
        return this.type;
    }

    /**
     * Identifier for the destination service (e.g. 'http://elastic.co', 'elasticsearch', 'rabbitmq')
     */
    public String getName() {
        return this.name;
    }

    /**
     * Identifier for the destination service resource being operated on (e.g. 'http://elastic.co:80', 'elasticsearch', 'rabbitmq/queue_name')
     */
    public String getResource() {
        return this.resource;
    }
}