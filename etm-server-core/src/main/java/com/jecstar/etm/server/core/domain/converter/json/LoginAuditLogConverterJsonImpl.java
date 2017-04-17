package com.jecstar.etm.server.core.domain.converter.json;

import java.util.Map;

import com.jecstar.etm.server.core.domain.audit.LoginAuditLog;
import com.jecstar.etm.server.core.domain.converter.AuditLogConverter;

public class LoginAuditLogConverterJsonImpl extends AbstractAuditLogConverterJsonImpl<LoginAuditLog>  implements AuditLogConverter<String, LoginAuditLog>{

	@Override
	public LoginAuditLog read(String content) {
		LoginAuditLog auditLog = new LoginAuditLog();
		Map<String, Object> valueMap = read(content, auditLog);
		auditLog.success = getBoolean(getTags().getSuccessTag(), valueMap);
		return auditLog;
	}

	@Override
	public String write(LoginAuditLog audit) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("{");
		boolean added = write(buffer, audit, true);
		added = addBooleanElementToJsonBuffer(this.tags.getSuccessTag(), audit.success, buffer, !added) || added;
		buffer.append("}");
		return buffer.toString();
	}


}
