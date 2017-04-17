package com.jecstar.etm.server.core.domain.converter.json;

import java.util.Map;

import com.jecstar.etm.server.core.domain.audit.GetEventAuditLog;
import com.jecstar.etm.server.core.domain.converter.AuditLogConverter;

public class GetEventAuditLogConverterJsonImpl extends AbstractAuditLogConverterJsonImpl<GetEventAuditLog>  implements AuditLogConverter<String, GetEventAuditLog>{

	@Override
	public GetEventAuditLog read(String content) {
		GetEventAuditLog auditLog = new GetEventAuditLog();
		Map<String, Object> valueMap = read(content, auditLog);
		auditLog.eventId = getString(getTags().getEventIdTag(), valueMap);
		auditLog.eventType = getString(getTags().getEventTypeTag(), valueMap);
		auditLog.eventName = getString(getTags().getEventNameTag(), valueMap);
		auditLog.found = getBoolean(getTags().getFoundTag(), valueMap);
		return auditLog;
	}

	@Override
	public String write(GetEventAuditLog audit) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("{");
		boolean added = write(buffer, audit, true);
		added = addStringElementToJsonBuffer(this.tags.getEventIdTag(), audit.eventId, buffer, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getEventTypeTag(), audit.eventType, buffer, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getEventNameTag(), audit.eventName, buffer, !added) || added;
		added = addBooleanElementToJsonBuffer(this.tags.getFoundTag(), audit.found, buffer, !added) || added;
		buffer.append("}");
		return buffer.toString();
	}


}
