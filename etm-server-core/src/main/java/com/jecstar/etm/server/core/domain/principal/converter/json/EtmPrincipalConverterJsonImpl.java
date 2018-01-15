package com.jecstar.etm.server.core.domain.principal.converter.json;

import com.jecstar.etm.server.core.domain.QueryOccurrence;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalConverter;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class EtmPrincipalConverterJsonImpl implements EtmPrincipalConverter<String> {

	private final EtmPrincipalTags tags = new EtmPrincipalTagsJsonImpl();
	private final JsonConverter converter = new JsonConverter();
	
	@Override
	public String writePrincipal(EtmPrincipal etmPrincipal) {
		final StringBuilder sb = new StringBuilder();
		sb.append("{");
		boolean added = this.converter.addStringElementToJsonBuffer(this.tags.getIdTag(), etmPrincipal.getId(), sb, true);
		added = this.converter.addStringElementToJsonBuffer(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, true, sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getEmailTag(), etmPrincipal.getEmailAddress(), true, sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getFilterQueryTag(), etmPrincipal.getFilterQuery(), true, sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getFilterQueryOccurrenceTag(), etmPrincipal.getFilterQueryOccurrence().name(), true, sb, !added) || added;
		added = this.converter.addBooleanElementToJsonBuffer(this.tags.getAlwaysShowCorrelatedEventsTag(), etmPrincipal.isAlwaysShowCorrelatedEvents(), sb, !added) || added;
		added = this.converter.addIntegerElementToJsonBuffer(this.tags.getSearchHistorySizeTag(), etmPrincipal.getHistorySize(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getLocaleTag(), etmPrincipal.getLocale().toLanguageTag(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getNameTag(), etmPrincipal.getName(), true, sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getPasswordHashTag(), etmPrincipal.getPasswordHash(), sb, !added) || added;
		added = this.converter.addBooleanElementToJsonBuffer(this.tags.getChangePasswordOnLogonTag(), etmPrincipal.isChangePasswordOnLogon(), sb, !added) || added;
		added = this.converter.addBooleanElementToJsonBuffer(this.tags.getLdapBaseTag(), etmPrincipal.isLdapBase(), sb, !added) || added;
		added = this.converter.addSetElementToJsonBuffer(this.tags.getRolesTag(), etmPrincipal.getRoles(), true, sb, !added) || added;
		added = this.converter.addSetElementToJsonBuffer(this.tags.getGroupsTag(), etmPrincipal.getGroups().stream().filter(g -> !g.isLdapBase()).map(EtmGroup::getName).collect(Collectors.toSet()), true, sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getTimeZoneTag(), etmPrincipal.getTimeZone().getID(), sb, !added) || added;
		sb.append("}");
		return sb.toString();
	}

	@Override
	public EtmPrincipal readPrincipal(String jsonContent) {
		return readPrincipal(this.converter.toMap(jsonContent));
	}
	
	@Override
	public String writeGroup(EtmGroup etmGroup) {
		final StringBuilder sb = new StringBuilder();
		sb.append("{");
		boolean added = this.converter.addStringElementToJsonBuffer(this.tags.getNameTag(), etmGroup.getName(), true, sb, true);
		added = this.converter.addStringElementToJsonBuffer(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP, true, sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getFilterQueryTag(), etmGroup.getFilterQuery(), true, sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getFilterQueryOccurrenceTag(), etmGroup.getFilterQueryOccurrence().name(), true, sb, !added) || added;
		added = this.converter.addBooleanElementToJsonBuffer(this.tags.getAlwaysShowCorrelatedEventsTag(), etmGroup.isAlwaysShowCorrelatedEvents(), sb, !added) || added;
		added = this.converter.addBooleanElementToJsonBuffer(this.tags.getLdapBaseTag(), etmGroup.isLdapBase(), sb, !added) || added;
		added = this.converter.addSetElementToJsonBuffer(this.tags.getRolesTag(), etmGroup.getRoles(), true, sb, !added) || added;
		sb.append("}");
		return sb.toString();
	}
	
	@Override
	public EtmGroup readGroup(String jsonContent) {
		return readGroup(this.converter.toMap(jsonContent));
	}
	
	public EtmPrincipal readPrincipal(Map<String, Object> valueMap) {
		EtmPrincipal principal = new EtmPrincipal(this.converter.getString(this.tags.getIdTag(), valueMap));
		principal.setPasswordHash(this.converter.getString(this.tags.getPasswordHashTag(), valueMap));
		principal.setName(this.converter.getString(this.tags.getNameTag(), valueMap));
		principal.setEmailAddress(this.converter.getString(this.tags.getEmailTag(), valueMap));
		principal.setFilterQuery(this.converter.getString(this.tags.getFilterQueryTag(), valueMap));
		principal.setFilterQueryOccurrence(QueryOccurrence.valueOf(this.converter.getString(this.tags.getFilterQueryOccurrenceTag(), valueMap)));
		principal.setAlwaysShowCorrelatedEvents(this.converter.getBoolean(this.tags.getAlwaysShowCorrelatedEventsTag(), valueMap));
		principal.setHistorySize(this.converter.getInteger(this.tags.getSearchHistorySizeTag(), valueMap, EtmPrincipal.DEFAULT_HISTORY_SIZE));
		principal.setChangePasswordOnLogon(this.converter.getBoolean(this.tags.getChangePasswordOnLogonTag(), valueMap, Boolean.FALSE));
		principal.setLdapBase(this.converter.getBoolean(this.tags.getLdapBaseTag(), valueMap, Boolean.FALSE));
		String value = this.converter.getString(this.tags.getLocaleTag(), valueMap);
		if (value != null) {
			principal.setLocale(Locale.forLanguageTag(value));
		}
		value = this.converter.getString(this.tags.getTimeZoneTag(), valueMap);
		if (value != null) {
			principal.setTimeZone(TimeZone.getTimeZone(value));
		}
		List<String> roles = this.converter.getArray(this.tags.getRolesTag(), valueMap);
		if (roles != null) {
			principal.addRoles(roles);
		}
		// Add the dashboard names. These are readonly properties added by the DashboardService.
		List<Map<String, Object>> dashboards = this.converter.getArray(this.tags.getDashboardsTag(), valueMap);
		if (dashboards != null) {
			for (Map<String, Object> dashboard : dashboards) {
				principal.addDashboard(this.converter.getString(this.tags.getNameTag(), dashboard));
			}
		}
		return principal;
	}

	public EtmGroup readGroup(Map<String, Object> valueMap) {
		EtmGroup group = new EtmGroup(this.converter.getString(this.tags.getNameTag(), valueMap));
		group.setFilterQuery(this.converter.getString(this.tags.getFilterQueryTag(), valueMap));
		group.setFilterQueryOccurrence(QueryOccurrence.valueOf(this.converter.getString(this.tags.getFilterQueryOccurrenceTag(), valueMap)));
		group.setAlwaysShowCorrelatedEvents(this.converter.getBoolean(this.tags.getAlwaysShowCorrelatedEventsTag(), valueMap));
		group.setLdapBase(this.converter.getBoolean(this.tags.getLdapBaseTag(), valueMap, Boolean.FALSE));
		List<String> roles = this.converter.getArray(this.tags.getRolesTag(), valueMap);
		if (roles != null) {
			group.addRoles(roles);
		}
        // Add the dashboard names. These are readonly properties added by the DashboardService.
        List<Map<String, Object>> dashboards = this.converter.getArray(this.tags.getDashboardsTag(), valueMap);
        if (dashboards != null) {
            for (Map<String, Object> dashboard : dashboards) {
                group.addDashboard(this.converter.getString(this.tags.getNameTag(), dashboard));
            }
        }
		return group;
	}

	@Override
	public EtmPrincipalTags getTags() {
		return this.tags;
	}

}
