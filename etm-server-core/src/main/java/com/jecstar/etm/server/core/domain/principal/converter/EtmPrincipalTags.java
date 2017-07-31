package com.jecstar.etm.server.core.domain.principal.converter;

public interface EtmPrincipalTags {

	String getIdTag();
	String getEmailTag();
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
	String getLdapBaseTag();
	
	String getSearchHistoryTag();
	String getSearchTemplatesTag();
	
}
