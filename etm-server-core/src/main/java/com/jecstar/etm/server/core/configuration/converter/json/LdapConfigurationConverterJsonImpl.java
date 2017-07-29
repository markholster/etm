package com.jecstar.etm.server.core.configuration.converter.json;

import java.util.Map;

import com.jecstar.etm.server.core.configuration.LdapConfiguration;
import com.jecstar.etm.server.core.configuration.LdapConfiguration.ConnectionSecurity;
import com.jecstar.etm.server.core.configuration.converter.LdapConfigurationConverter;
import com.jecstar.etm.server.core.configuration.converter.LdapConfigurationTags;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

public class LdapConfigurationConverterJsonImpl implements LdapConfigurationConverter<String> {

	private final LdapConfigurationTags tags = new LdapConfigurationTagsJsonImpl();
	private final JsonConverter converter = new JsonConverter();
	
	@Override
	public LdapConfiguration read(String jsonContent) {
		return read(this.converter.toMap(jsonContent));
	}
	
	private LdapConfiguration read(Map<String, Object> valueMap) {
		LdapConfiguration ldapConfiguration = new LdapConfiguration();
		ldapConfiguration.setHost(this.converter.getString(this.tags.getHostTag(), valueMap));
		ldapConfiguration.setPort(this.converter.getInteger(this.tags.getPortTag(), valueMap));
		ldapConfiguration.setConnectionSecurity(ConnectionSecurity.safeValueOf(this.converter.getString(this.tags.getConnectionSecurityTag(), valueMap)));
		ldapConfiguration.setBindDn(this.converter.getString(this.tags.getBindDnTag(), valueMap));
		ldapConfiguration.setBindPassword(this.converter.decodeBase64(this.converter.getString(this.tags.getBindPasswordTag(), valueMap), 7));
		
		ldapConfiguration.setMinPoolSize(this.converter.getInteger(this.tags.getMinPoolSizeTag(), valueMap));
		ldapConfiguration.setMaxPoolSize(this.converter.getInteger(this.tags.getMaxPoolSizeTag(), valueMap));
		ldapConfiguration.setConnectionTestBaseDn(this.converter.getString(this.tags.getConnectionTestBaseDnTag(), valueMap));
		ldapConfiguration.setConnectionTestSearchFilter(this.converter.getString(this.tags.getConnectionTestSearchFilterTag(), valueMap));
		
		ldapConfiguration.setUserBaseDn(this.converter.getString(this.tags.getUserBaseDnTag(), valueMap));
		ldapConfiguration.setUserSearchFilter(this.converter.getString(this.tags.getUserSearchFilterTag(), valueMap));
		ldapConfiguration.setUserSearchInSubtree(this.converter.getBoolean(this.tags.getUserSearchInSubtreeTag(), valueMap, false));
		ldapConfiguration.setUserIdentifierAttribute(this.converter.getString(this.tags.getUserIdentifierAttributeTag(), valueMap));
		ldapConfiguration.setUserFullNameAttribute(this.converter.getString(this.tags.getUserFullNameAttributeTag(), valueMap));
		ldapConfiguration.setUserEmailAttribute(this.converter.getString(this.tags.getUserEmailAttributeTag(), valueMap));
		ldapConfiguration.setUserMemberOfGroupsAttribute(this.converter.getString(this.tags.getUserMemberOfGroupsAttributeTag(), valueMap));
		ldapConfiguration.setUserGroupsQueryBaseDn(this.converter.getString(this.tags.getUserGroupsQueryBaseDnTag(), valueMap));
		ldapConfiguration.setUserGroupsQueryFilter(this.converter.getString(this.tags.getUserGroupsQueryFilterTag(), valueMap));
		
		ldapConfiguration.setGroupBaseDn(this.converter.getString(this.tags.getGroupBaseDnTag(), valueMap));
		ldapConfiguration.setGroupSearchFilter(this.converter.getString(this.tags.getGroupSearchFilterTag(), valueMap));
		return ldapConfiguration;
	}


	@Override
	public String write(LdapConfiguration ldapConfiguration) {
		final StringBuilder sb = new StringBuilder();
		sb.append("{");
		boolean added = this.converter.addStringElementToJsonBuffer(this.tags.getHostTag(), ldapConfiguration.getHost(), sb, true);
		added = this.converter.addIntegerElementToJsonBuffer(this.tags.getPortTag(), ldapConfiguration.getPort(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getConnectionSecurityTag(), ldapConfiguration.getConnectionSecurity() != null ? ldapConfiguration.getConnectionSecurity().name() : null, sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getBindDnTag(), ldapConfiguration.getBindDn(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getBindPasswordTag(), this.converter.encodeBase64(ldapConfiguration.getBindPassword(), 7), sb, !added) || added;
		
		added = this.converter.addIntegerElementToJsonBuffer(this.tags.getMinPoolSizeTag(), ldapConfiguration.getMinPoolSize(), sb, !added) || added;
		added = this.converter.addIntegerElementToJsonBuffer(this.tags.getMaxPoolSizeTag(), ldapConfiguration.getMaxPoolSize(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getConnectionTestBaseDnTag(), ldapConfiguration.getConnectionTestBaseDn(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getConnectionTestSearchFilterTag(), ldapConfiguration.getConnectionTestSearchFilter(), sb, !added) || added;

		added = this.converter.addStringElementToJsonBuffer(this.tags.getUserBaseDnTag(), ldapConfiguration.getUserBaseDn(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getUserSearchFilterTag(), ldapConfiguration.getUserSearchFilter(), sb, !added) || added;
		added = this.converter.addBooleanElementToJsonBuffer(this.tags.getUserSearchInSubtreeTag(), ldapConfiguration.isUserSearchInSubtree(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getUserIdentifierAttributeTag(), ldapConfiguration.getUserIdentifierAttribute(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getUserFullNameAttributeTag(), ldapConfiguration.getUserFullNameAttribute(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getUserEmailAttributeTag(), ldapConfiguration.getUserEmailAttribute(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getUserMemberOfGroupsAttributeTag(), ldapConfiguration.getUserMemberOfGroupsAttribute(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getUserGroupsQueryBaseDnTag(), ldapConfiguration.getUserGroupsQueryBaseDn(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getUserGroupsQueryFilterTag(), ldapConfiguration.getUserGroupsQueryFilter(), sb, !added) || added;
		
		added = this.converter.addStringElementToJsonBuffer(this.tags.getGroupBaseDnTag(), ldapConfiguration.getGroupBaseDn(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getGroupSearchFilterTag(), ldapConfiguration.getGroupSearchFilter(), sb, !added) || added;
		
		sb.append("}");
		return sb.toString();
	}

	@Override
	public LdapConfigurationTags getTags() {
		return this.tags;
	}
}
