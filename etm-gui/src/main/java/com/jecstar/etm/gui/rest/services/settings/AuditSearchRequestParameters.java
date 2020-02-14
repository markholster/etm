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

package com.jecstar.etm.gui.rest.services.settings;

import com.jecstar.etm.server.core.domain.audit.AuditLog;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

import java.util.Map;

class AuditSearchRequestParameters {

    private static final Integer DEFAULT_START_IX = 0;
    private static final Integer DEFAULT_MAX_SIZE = 50;

    private final String queryString;
    private final Integer startIndex;
    private final Integer maxResults;
    private final String sortField;
    private final String sortOrder;
    private Long notAfterTimestamp;

    public AuditSearchRequestParameters(String query) {
        this.queryString = query;
        this.startIndex = 0;
        this.maxResults = 50;
        this.sortField = AuditLog.HANDLING_TIME;
        this.sortOrder = "desc";
    }


    AuditSearchRequestParameters(Map<String, Object> requestValues) {
        JsonConverter converter = new JsonConverter();
        this.queryString = converter.getString("query", requestValues);
        this.startIndex = converter.getInteger("start_ix", requestValues, DEFAULT_START_IX);
        this.maxResults = converter.getInteger("max_results", requestValues, DEFAULT_MAX_SIZE);
        this.sortField = converter.getString("sort_field", requestValues);
        this.sortOrder = converter.getString("sort_order", requestValues);
        this.notAfterTimestamp = converter.getLong("timestamp", requestValues);
        if (this.notAfterTimestamp == null) {
            this.notAfterTimestamp = System.currentTimeMillis();
        }
    }

    public String getQueryString() {
        return this.queryString;
    }

    public Integer getStartIndex() {
        return this.startIndex;
    }

    public Integer getMaxResults() {
        return this.maxResults;
    }

    public String getSortField() {
        return this.sortField;
    }

    public String getSortOrder() {
        return this.sortOrder;
    }

    public Long getNotAfterTimestamp() {
        return this.notAfterTimestamp;
    }


}
