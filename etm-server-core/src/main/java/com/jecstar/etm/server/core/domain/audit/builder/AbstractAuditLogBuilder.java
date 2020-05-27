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

package com.jecstar.etm.server.core.domain.audit.builder;

import com.jecstar.etm.server.core.domain.audit.AuditLog;

import java.time.Instant;

abstract class AbstractAuditLogBuilder<Audit extends AuditLog, Builder extends AbstractAuditLogBuilder<Audit, Builder>> {

    final Audit audit;

    AbstractAuditLogBuilder(Audit audit) {
        this.audit = audit;
    }

    public Audit build() {
        return this.audit;
    }

    @SuppressWarnings("unchecked")
    public Builder setId(String id) {
        this.audit.id = id;
        return (Builder) this;
    }

    public String getId() {
        return this.audit.id;
    }

    @SuppressWarnings("unchecked")
    public Builder setTimestamp(Instant timestamp) {
        this.audit.timestamp = timestamp;
        return (Builder) this;
    }

    public Instant getTimestamp() {
        return this.audit.timestamp;
    }

    @SuppressWarnings("unchecked")
    public Builder setHandlingTime(Instant handlingTime) {
        this.audit.handlingTime = handlingTime;
        return (Builder) this;
    }

    @SuppressWarnings("unchecked")
    public Builder setPrincipalId(String principalId) {
        this.audit.principalId = principalId;
        return (Builder) this;
    }


}
