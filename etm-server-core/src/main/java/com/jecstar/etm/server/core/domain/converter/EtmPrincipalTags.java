package com.jecstar.etm.server.core.domain.converter;

public interface EtmPrincipalTags {

	String getIdTag();
	String getFilterQueryTag();
	String getFilterQueryOccurrenceTag();
	String getAlwaysShowCorrelatedEventsTag();
	String getSearchHistorySizeTag();
	String getLocaleTag();
	String getNameTag();
	String getPasswordHashTag();
	String getChangePasswordOnLogonTag();
	String getRolesTag();
	String getGroupsTag();
	String getTimeZoneTag();
	
	String getSearchHistoryTag();
	String getSearchTemplatesTag();
	
}
