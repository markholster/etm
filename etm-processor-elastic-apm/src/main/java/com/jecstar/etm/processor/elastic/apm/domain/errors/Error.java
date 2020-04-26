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

package com.jecstar.etm.processor.elastic.apm.domain.errors;

import com.jecstar.etm.server.core.converter.JsonField;

/**
 * An error or a logged error message captured by an agent occurring in a monitored service
 */
public class Error {

    @JsonField("timestamp")
    private Long timestamp;
    @JsonField("id")
    private String id;
    @JsonField("trace_id")
    private String traceId;
    @JsonField("transaction_id")
    private String transactionId;
    @JsonField("parent_id")
    private String parentId;
    @JsonField(value = "transaction", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.errors.TransactionConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.errors.Transaction transaction;
    @JsonField(value = "context", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.ContextConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Context context;
    @JsonField("culprit")
    private String culprit;
    @JsonField(value = "exception", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.errors.ExceptionConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.errors.Exception exception;
    @JsonField(value = "log", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.errors.LogConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.errors.Log log;

    /**
     * Recorded time of the event, UTC based and formatted as microseconds since Unix epoch
     */
    public Long getTimestamp() {
        return this.timestamp;
    }

    /**
     * Hex encoded 128 random bits ID of the error.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Hex encoded 128 random bits ID of the correlated trace. Must be present if transaction_id and parent_id are set.
     */
    public String getTraceId() {
        return this.traceId;
    }

    /**
     * Hex encoded 64 random bits ID of the correlated transaction. Must be present if trace_id and parent_id are set.
     */
    public String getTransactionId() {
        return this.transactionId;
    }

    /**
     * Hex encoded 64 random bits ID of the parent transaction or span. Must be present if trace_id and transaction_id are set.
     */
    public String getParentId() {
        return this.parentId;
    }

    /**
     * Data for correlating errors with transactions
     */
    public com.jecstar.etm.processor.elastic.apm.domain.errors.Transaction getTransaction() {
        return this.transaction;
    }

    /**
     * Any arbitrary contextual information regarding the event, captured by the agent, optionally provided by the user
     */
    public com.jecstar.etm.processor.elastic.apm.domain.Context getContext() {
        return this.context;
    }

    /**
     * Function call which was the primary perpetrator of this event.
     */
    public String getCulprit() {
        return this.culprit;
    }

    /**
     * Information about the originally thrown error.
     */
    public com.jecstar.etm.processor.elastic.apm.domain.errors.Exception getException() {
        return this.exception;
    }

    /**
     * Additional information added when logging the error.
     */
    public com.jecstar.etm.processor.elastic.apm.domain.errors.Log getLog() {
        return this.log;
    }
}