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

package com.jecstar.etm.server.core.domain.audit;

import com.jecstar.etm.server.core.converter.JsonField;

import java.time.Instant;

public abstract class AuditLog {

    public static final String ID = "id";
    public static final String TIMESTAMP = "timestamp";
    public static final String HANDLING_TIME = "handling_time";
    public static final String PRINCIPAL_ID = "principal_id";

    /**
     * The unique id of the audit log.
     */
    @JsonField(ID)
    public String id;

    /**
     * The time the audit was written to the database.
     */
    @JsonField(TIMESTAMP)
    public Instant timestamp;

    /**
     * The principalId that caused this log.
     */
    @JsonField(PRINCIPAL_ID)
    public String principalId;

    /**
     * The time the audit event took place.
     */
    @JsonField(HANDLING_TIME)
    public Instant handlingTime;

}
