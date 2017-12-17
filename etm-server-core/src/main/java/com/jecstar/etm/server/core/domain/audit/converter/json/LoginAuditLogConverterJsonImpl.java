package com.jecstar.etm.server.core.domain.audit.converter.json;

import com.jecstar.etm.server.core.domain.audit.LoginAuditLog;
import com.jecstar.etm.server.core.domain.audit.converter.AuditLogConverter;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;

import java.util.Map;

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
		boolean added = write(buffer, audit, true, ElasticsearchLayout.AUDIT_LOG_OBJECT_TYPE_LOGIN);
		added = addBooleanElementToJsonBuffer(getTags().getSuccessTag(), audit.success, buffer, !added) || added;
		buffer.append("}");
		return buffer.toString();
	}


}
