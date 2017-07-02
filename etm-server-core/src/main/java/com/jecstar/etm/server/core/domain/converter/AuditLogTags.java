package com.jecstar.etm.server.core.domain.converter;

public interface AuditLogTags {

	String getTimestampTag();
	String getHandlingTimeTag();
	String getPrincipalIdTag();
	
	//GetEvent tags
	String getEventIdTag();
	String getEventTypeTag();
	String getEventNameTag();
	String getFoundTag();
	String getCorrelatedEventsTag();
	
	//Login tags
	String getSuccessTag();
	
	//Logout tags
	String getExpiredTag();
	
	//Query tags
	String getUserQueryTag();
	String getExecutedQueryTag();
	String getNumberOfResultsTag();
	

	
	
	
}
