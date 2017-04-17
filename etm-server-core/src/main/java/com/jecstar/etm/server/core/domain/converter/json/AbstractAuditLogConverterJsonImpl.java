package com.jecstar.etm.server.core.domain.converter.json;

import java.util.Map;

import com.jecstar.etm.server.core.domain.audit.AuditLog;
import com.jecstar.etm.server.core.domain.converter.AuditLogConverter;
import com.jecstar.etm.server.core.domain.converter.AuditLogTags;

public abstract class AbstractAuditLogConverterJsonImpl<Audit extends AuditLog<Audit>> extends JsonConverter implements AuditLogConverter<String, Audit> {
	
	AuditLogTags tags = new AuditLogTagsJsonImpl();

	Map<String, Object> read(String content, Audit audit) {
		Map<String, Object> valueMap = toMap(content);
		audit.handlingTime = getZonedDateTime(this.tags.getHandlingTimeTag(), valueMap);
		audit.principalId = getString(this.tags.getPrincipalIdTag(), valueMap);
		return valueMap;
	}
	
	boolean write(StringBuilder buffer, Audit audit, boolean firstElement) {
		boolean added = !firstElement;
		added = addLongElementToJsonBuffer(this.tags.getHandlingTimeTag(), audit.handlingTime.toInstant().toEpochMilli(), buffer, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getPrincipalIdTag(), audit.principalId, buffer, !added) || added;
		return added;
	}
	
	@Override
	public AuditLogTags getTags() {
		return this.tags;
	}

}
