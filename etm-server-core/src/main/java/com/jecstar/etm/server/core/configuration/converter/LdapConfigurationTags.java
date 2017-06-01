package com.jecstar.etm.server.core.configuration.converter;

public interface LdapConfigurationTags {

	String getHostTag();
	String getPortTag();
	String getConnectionSecurityTag();
	String getBindDnTag();
	String getBindPasswordTag();
	
	String getMinPoolSizeTag();
	String getMaxPoolSizeTag();
	String getConnectionTestBaseDnTag();
	String getConnectionTestSearchFilterTag();
	
	String getUserBaseDnTag();
	String getUserSearchFilterTag();
	String getUserSearchInSubtreeTag();
	String getUserIdentifierAttributeTag();
	String getUserFullNameAttributeTag();
	String getUserEmailAttributeTag();
	String getUserMemberOfGroupsAttributeTag();
	String getUserGroupsQueryBaseDnTag();
	String getUserGroupsQueryFilterTag();
	
	String getGroupBaseDnTag();
	String getGroupSearchFilterTag();

}
