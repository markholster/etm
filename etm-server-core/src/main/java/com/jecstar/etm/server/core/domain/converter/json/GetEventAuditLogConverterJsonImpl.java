package com.jecstar.etm.server.core.domain.converter.json;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
		List<Map<String, Object>> correlatedEvents = getArray(getTags().getCorrelatedEventsTag(), valueMap);
		if (correlatedEvents != null && correlatedEvents.size() > 0) {
			for (Map<String, Object> correlatedEvent : correlatedEvents) {
				auditLog.correlatedEvents.put(getString(getTags().getEventIdTag(), correlatedEvent), getString(getTags().getEventTypeTag(), valueMap));
			}
		}
		return auditLog;
	}

	@Override
	public String write(GetEventAuditLog audit) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("{");
		boolean added = write(buffer, audit, true);
		added = addStringElementToJsonBuffer(getTags().getEventIdTag(), audit.eventId, buffer, !added) || added;
		added = addStringElementToJsonBuffer(getTags().getEventTypeTag(), audit.eventType, buffer, !added) || added;
		added = addStringElementToJsonBuffer(getTags().getEventNameTag(), audit.eventName, buffer, !added) || added;
		added = addBooleanElementToJsonBuffer(getTags().getFoundTag(), audit.found, buffer, !added) || added;
		if (audit.correlatedEvents.size() > 0) {
			if (added) {
				buffer.append(",");
			}
			buffer.append(escapeToJson(getTags().getCorrelatedEventsTag(), true)).append(": [");
			buffer.append(audit.correlatedEvents.entrySet().stream()
				.map(c -> "{" + escapeObjectToJsonNameValuePair(getTags().getEventIdTag(), c.getKey()) + "," + escapeObjectToJsonNameValuePair(getTags().getEventTypeTag(), c.getValue()) + "}")
				.collect(Collectors.joining(","))
			);
			buffer.append("]");
		}
		buffer.append("}");
		return buffer.toString();
	}


}
