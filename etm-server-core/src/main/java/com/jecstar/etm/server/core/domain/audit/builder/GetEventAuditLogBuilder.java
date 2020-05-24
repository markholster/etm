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

import com.jecstar.etm.server.core.domain.audit.GetEventAuditLog;

import java.util.Set;

public class GetEventAuditLogBuilder extends AbstractAuditLogBuilder<GetEventAuditLog, GetEventAuditLogBuilder> {

    public GetEventAuditLogBuilder() {
        super(new GetEventAuditLog());
    }

    public GetEventAuditLogBuilder setEventId(String eventId) {
        this.audit.eventId = eventId;
        return this;
    }

    public GetEventAuditLogBuilder setFound(boolean found) {
        this.audit.found = found;
        return this;
    }

    public GetEventAuditLogBuilder setEventName(String eventName) {
        this.audit.eventName = eventName;
        return this;
    }

    public GetEventAuditLogBuilder addCorrelatedEvent(String correlatedEventId) {
        this.audit.correlatedEvents.add(correlatedEventId);
        return this;
    }

    public GetEventAuditLogBuilder setRedactedFields(Set<String> redactedFields) {
        this.audit.redactedFields = redactedFields;
        return this;
    }

    public GetEventAuditLogBuilder setDownloaded(boolean downloaded) {
        this.audit.downloaded = downloaded;
        return this;
    }


}
