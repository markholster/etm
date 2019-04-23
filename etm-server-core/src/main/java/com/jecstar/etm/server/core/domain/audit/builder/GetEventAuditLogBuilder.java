package com.jecstar.etm.server.core.domain.audit.builder;

import com.jecstar.etm.server.core.domain.audit.GetEventAuditLog;

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

    public GetEventAuditLogBuilder setPayloadVisible(boolean payloadVisible) {
        this.audit.payloadVisible = payloadVisible;
        return this;
    }

    public GetEventAuditLogBuilder setDownloaded(boolean downloaded) {
        this.audit.downloaded = downloaded;
        return this;
    }

}
