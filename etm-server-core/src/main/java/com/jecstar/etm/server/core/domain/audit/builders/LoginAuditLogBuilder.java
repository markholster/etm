package com.jecstar.etm.server.core.domain.audit.builders;

import com.jecstar.etm.server.core.domain.audit.LoginAuditLog;

public class LoginAuditLogBuilder extends AbstractAuditLogBuilder<LoginAuditLog, LoginAuditLogBuilder>{

	public LoginAuditLogBuilder() {
		super(new LoginAuditLog());
	}

	public LoginAuditLogBuilder setSuccess(boolean success) {
		this.audit.success = success;
		return this;
	}

}
