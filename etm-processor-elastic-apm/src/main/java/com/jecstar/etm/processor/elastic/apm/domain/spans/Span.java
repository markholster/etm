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
 * An event captured by an agent occurring in a monitored service
 */
public class Span {

    @JsonField("timestamp")
    private Long timestamp;
    @JsonField("type")
    private String type;
    @JsonField("subtype")
    private String subtype;
    @JsonField("id")
    private String id;
    @JsonField("transaction_id")
    private String transactionId;
    @JsonField("trace_id")
    private String traceId;
    @JsonField("parent_id")
    private String parentId;
    @JsonField("start")
    private Long start;
    @JsonField("action")
    private String action;
    @JsonField(value = "context", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.spans.ContextConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.spans.Context context;
    @JsonField("duration")
    private Long duration;
    @JsonField("name")
    private String name;
    @JsonField("sync")
    private Boolean sync;

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
     * Hex encoded 64 random bits ID of the span.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Hex encoded 64 random bits ID of the correlated transaction.
     */
    public String getTransactionId() {
        return this.transactionId;
    }

    /**
     * Hex encoded 128 random bits ID of the correlated trace.
     */
    public String getTraceId() {
        return this.traceId;
    }

    /**
     * Hex encoded 64 random bits ID of the parent transaction or span.
     */
    public String getParentId() {
        return this.parentId;
    }

    /**
     * Offset relative to the transaction's timestamp identifying the start of the span, in milliseconds
     */
    public Long getStart() {
        return this.start;
    }

    /**
     * The specific kind of event within the sub-type represented by the span (e.g. query, connect)
     */
    public String getAction() {
        return this.action;
    }

    /**
     * Any other arbitrary data captured by the agent, optionally provided by the user
     */
    public com.jecstar.etm.processor.elastic.apm.domain.spans.Context getContext() {
        return this.context;
    }

    /**
     * Duration of the span in milliseconds
     */
    public Long getDuration() {
        return this.duration;
    }

    /**
     * Generic designation of a span in the scope of a transaction
     */
    public String getName() {
        return this.name;
    }

    /**
     * Indicates whether the span was executed synchronously or asynchronously.
     */
    public Boolean isSync() {
        return this.sync;
    }
}