package com.jecstar.etm.server.core.domain.audit.builder;

import com.jecstar.etm.server.core.domain.audit.LogoutAuditLog;

public class LogoutAuditLogBuilder extends AbstractAuditLogBuilder<LogoutAuditLog, LogoutAuditLogBuilder>{

	public LogoutAuditLogBuilder() {
		super(new LogoutAuditLog());
	}

	public LogoutAuditLogBuilder setExpired(boolean expired) {
		this.audit.expired = expired;
		return this;
	}

}
