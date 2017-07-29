package com.jecstar.etm.server.core.domain.audit.converter.json;

import java.util.Map;

import com.jecstar.etm.server.core.domain.audit.LogoutAuditLog;
import com.jecstar.etm.server.core.domain.audit.converter.AuditLogConverter;

public class LogoutAuditLogConverterJsonImpl extends AbstractAuditLogConverterJsonImpl<LogoutAuditLog>  implements AuditLogConverter<String, LogoutAuditLog>{

	@Override
	public LogoutAuditLog read(String content) {
		LogoutAuditLog auditLog = new LogoutAuditLog();
		Map<String, Object> valueMap = read(content, auditLog);
		auditLog.expired = getBoolean(getTags().getExpiredTag(), valueMap);
		return auditLog;
	}

	@Override
	public String write(LogoutAuditLog audit) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("{");
		boolean added = write(buffer, audit, true);
		added = addBooleanElementToJsonBuffer(getTags().getExpiredTag(), audit.expired, buffer, !added) || added;
		buffer.append("}");
		return buffer.toString();
	}


}
