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
 * Data for correlating errors with transactions
 */
public class Transaction {

    @JsonField("sampled")
    private Boolean sampled;
    @JsonField("type")
    private String type;

    /**
     * Transactions that are 'sampled' will include all available information. Transactions that are not sampled will not have 'spans' or 'context'. Defaults to true.
     */
    public Boolean isSampled() {
        return this.sampled;
    }

    /**
     * Keyword of specific relevance in the service's domain (eg: 'request', 'backgroundjob', etc)
     */
    public String getType() {
        return this.type;
    }
}