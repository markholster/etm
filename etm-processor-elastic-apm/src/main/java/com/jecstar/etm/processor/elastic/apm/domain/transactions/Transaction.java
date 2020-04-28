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

package com.jecstar.etm.processor.elastic.apm.domain.transactions;

import com.jecstar.etm.server.core.converter.JsonField;

/**
 * An event corresponding to an incoming request or similar task occurring in a monitored service
 */
public class Transaction {

    @JsonField("timestamp")
    private Long timestamp;
    @JsonField("name")
    private String name;
    @JsonField("type")
    private String type;
    @JsonField("id")
    private String id;
    @JsonField("trace_id")
    private String traceId;
    @JsonField("parent_id")
    private String parentId;
    @JsonField(value = "span_count", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.transactions.SpanCountConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.transactions.SpanCount spanCount;
    @JsonField(value = "context", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.ContextConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Context context;
    @JsonField("duration")
    private Long duration;
    @JsonField("result")
    private String result;
    @JsonField("marks")
    private java.util.Map<String, Object> marks;
    @JsonField("sampled")
    private Boolean sampled;

    /**
     * Recorded time of the event, UTC based and formatted as microseconds since Unix epoch
     */
    public Long getTimestamp() {
        return this.timestamp;
    }

    /**
     * Generic designation of a transaction in the scope of a single service (eg: 'GET /users/:id')
     */
    public String getName() {
        return this.name;
    }

    /**
     * Keyword of specific relevance in the service's domain (eg: 'request', 'backgroundjob', etc)
     */
    public String getType() {
        return this.type;
    }

    /**
     * Hex encoded 64 random bits ID of the transaction.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Hex encoded 128 random bits ID of the correlated trace.
     */
    public String getTraceId() {
        return this.traceId;
    }

    /**
     * Hex encoded 64 random bits ID of the parent transaction or span. Only root transactions of a trace do not have a parent_id, otherwise it needs to be set.
     */
    public String getParentId() {
        return this.parentId;
    }

    public com.jecstar.etm.processor.elastic.apm.domain.transactions.SpanCount getSpanCount() {
        return this.spanCount;
    }

    /**
     * Any arbitrary contextual information regarding the event, captured by the agent, optionally provided by the user
     */
    public com.jecstar.etm.processor.elastic.apm.domain.Context getContext() {
        return this.context;
    }

    /**
     * How long the transaction took to complete, in ms with 3 decimal points
     */
    public Long getDuration() {
        return this.duration;
    }

    /**
     * The result of the transaction. For HTTP-related transactions, this should be the status code formatted like 'HTTP 2xx'.
     */
    public String getResult() {
        return this.result;
    }

    /**
     * A mark captures the timing of a significant event during the lifetime of a transaction. Marks are organized into groups and can be set by the user or the agent.
     */
    public java.util.Map<String, Object> getMarks() {
        return this.marks;
    }

    /**
     * Transactions that are 'sampled' will include all available information. Transactions that are not sampled will not have 'spans' or 'context'. Defaults to true.
     */
    public Boolean isSampled() {
        return this.sampled;
    }
}