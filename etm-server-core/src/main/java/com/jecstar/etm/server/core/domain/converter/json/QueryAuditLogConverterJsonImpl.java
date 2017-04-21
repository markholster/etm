package com.jecstar.etm.server.core.domain.converter.json;

import java.util.Map;

import com.jecstar.etm.server.core.domain.audit.QueryAuditLog;
import com.jecstar.etm.server.core.domain.converter.AuditLogConverter;

public class QueryAuditLogConverterJsonImpl extends AbstractAuditLogConverterJsonImpl<QueryAuditLog>  implements AuditLogConverter<String, QueryAuditLog>{

	@Override
	public QueryAuditLog read(String content) {
		QueryAuditLog auditLog = new QueryAuditLog();
		Map<String, Object> valueMap = read(content, auditLog);
		auditLog.userQuery = getString(getTags().getUserQueryTag(), valueMap);
		auditLog.executedQuery = getString(getTags().getExecutedQueryTag(), valueMap);
		auditLog.numberOfResults = getLong(getTags().getNumberOfResultsTag(), valueMap);
		return auditLog;
	}

	@Override
	public String write(QueryAuditLog audit) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("{");
		boolean added = write(buffer, audit, true);
		added = addStringElementToJsonBuffer(getTags().getUserQueryTag(), audit.userQuery, buffer, !added) || added;
		added = addStringElementToJsonBuffer(getTags().getExecutedQueryTag(), audit.executedQuery, buffer, !added) || added;
		added = addLongElementToJsonBuffer(getTags().getNumberOfResultsTag(), audit.numberOfResults, buffer, !added) || added;
		buffer.append("}");
		return buffer.toString();
	}


}
