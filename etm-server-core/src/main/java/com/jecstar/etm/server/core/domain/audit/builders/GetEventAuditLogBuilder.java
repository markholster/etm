package com.jecstar.etm.server.core.domain.audit.builders;

import com.jecstar.etm.server.core.domain.audit.GetEventAuditLog;

public class GetEventAuditLogBuilder extends AbstractAuditLogBuilder<GetEventAuditLog, GetEventAuditLogBuilder>{

	public GetEventAuditLogBuilder() {
		super(new GetEventAuditLog());
	}

	public GetEventAuditLogBuilder setEventId(String eventId) {
		this.audit.eventId = eventId;
		return this;
	}
	
	public GetEventAuditLogBuilder setEventType(String eventType) {
		this.audit.eventType = eventType;
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

}
