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

package com.jecstar.etm.processor.elastic.apm.domain.metricsets;

import com.jecstar.etm.server.core.converter.JsonField;

/**
 * Data captured by an agent representing an event occurring in a monitored service
 */
public class Metricset {

    @JsonField("timestamp")
    private Long timestamp;
    @JsonField("type")
    private String type;
    @JsonField("subtype")
    private String subtype;
    @JsonField("name")
    private String name;
    @JsonField("samples")
    private java.util.Map<String, Object> samples;
    @JsonField(value = "tags", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.TagsConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Tags tags;

    /**
     * Recorded time of the event, UTC based and formatted as microseconds since Unix epoch
     */
    public Long getTimestamp() {
        return this.timestamp;
    }

    /**
     * Keyword of specific relevance in the service's domain (eg: 'db.postgresql.query', 'template.erb', etc)
     */
    public String getType() {
        return this.type;
    }

    /**
     * A further sub-division of the type (e.g. postgresql, elasticsearch)
     */
    public String getSubtype() {
        return this.subtype;
    }

    /**
     * Generic designation of a transaction in the scope of a single service (eg: 'GET /users/:id')
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sampled application metrics collected from the agent.
     */
    public java.util.Map<String, Object> getSamples() {
        return this.samples;
    }

    /**
     * A flat mapping of user-defined tags with string, boolean or number values.
     */
    public com.jecstar.etm.processor.elastic.apm.domain.Tags getTags() {
        return this.tags;
    }
}