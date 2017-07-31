package com.jecstar.etm.server.core.domain.audit.converter.json;

import com.jecstar.etm.server.core.domain.audit.converter.AuditLogTags;

public class AuditLogTagsJsonImpl implements AuditLogTags {

	@Override
	public String getTimestampTag() {
		return "timestamp";
	}
	
	@Override
	public String getHandlingTimeTag() {
		return "handling_time";
	}

	@Override
	public String getPrincipalIdTag() {
		return "principal_id";
	}

	@Override
	public String getEventIdTag() {
		return "event_id";
	}

	@Override
	public String getEventTypeTag() {
		return "event_type";
	}

	@Override
	public String getEventNameTag() {
		return "event_name";
	}

	@Override
	public String getFoundTag() {
		return "found";
	}
	
	@Override
	public String getCorrelatedEventsTag() {
		return "correlated_events";
	}

	@Override
	public String getSuccessTag() {
		return "success";
	}
	
	@Override
	public String getExpiredTag() {
		return "expired";
	}

	@Override
	public String getUserQueryTag() {
		return "user_query";
	}

	@Override
	public String getExecutedQueryTag() {
		return "executed_query";
	}

	@Override
	public String getNumberOfResultsTag() {
		return "number_of_results";
	}
	
}
