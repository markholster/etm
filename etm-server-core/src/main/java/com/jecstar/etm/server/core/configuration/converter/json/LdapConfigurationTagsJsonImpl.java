package com.jecstar.etm.server.core.configuration.converter.json;

import com.jecstar.etm.server.core.configuration.converter.LdapConfigurationTags;

public class LdapConfigurationTagsJsonImpl implements LdapConfigurationTags {

	@Override
	public String getHostTag() {
		return "host";
	}

	@Override
	public String getPortTag() {
		return "port";
	}

	@Override
	public String getConnectionSecurityTag() {
		return "connection_security";
	}

	@Override
	public String getBindDnTag() {
		return "bind_dn";
	}

	@Override
	public String getBindPasswordTag() {
		return "bind_password";
	}

	@Override
	public String getMinPoolSizeTag() {
		return "min_pool_size";
	}

	@Override
	public String getMaxPoolSizeTag() {
		return "max_pool_size";
	}

	@Override
	public String getConnectionTestBaseDnTag() {
		return "connection_test_base_dn";
	}

	@Override
	public String getConnectionTestSearchFilterTag() {
		return "connection_test_search_filter";
	}

	@Override
	public String getUserBaseDnTag() {
		return "user_base_dn";
	}

	@Override
	public String getUserSearchFilterTag() {
		return "user_search_filter";
	}

	@Override
	public String getUserIdentifierAttributeTag() {
		return "user_identifier_attribute";
	}

	@Override
	public String getUserFullNameAttributeTag() {
		return "user_full_name_attribute";
	}

	@Override
	public String getUserEmailAttributeTag() {
		return "user_email_attribute";
	}

	@Override
	public String getUserMemberOfGroupsAttributeTag() {
		return "user_member_of_groups_attribute";
	}

	@Override
	public String getUserGroupsQueryBaseDnTag() {
		return "user_groups_query_base_dn";
	}

	@Override
	public String getUserGroupsQueryFilterTag() {
		return "user_groups_query_filter";
	}

	@Override
	public String getGroupBaseDnTag() {
		return "group_base_dn";
	}

	@Override
	public String getGroupSearchFilterTag() {
		return "group_search_filter";
	}

}
