package com.jecstar.etm.server.core.domain.converter.json;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.jecstar.etm.server.core.domain.EtmGroup;
import com.jecstar.etm.server.core.domain.EtmPrincipal;
import com.jecstar.etm.server.core.domain.EtmPrincipalRole;
import com.jecstar.etm.server.core.domain.QueryOccurrence;
import com.jecstar.etm.server.core.domain.converter.EtmPrincipalConverter;
import com.jecstar.etm.server.core.domain.converter.EtmPrincipalTags;

public class EtmPrincipalConverterJsonImpl implements EtmPrincipalConverter<String> {

	private final EtmPrincipalTags tags = new EtmPrincipalTagsJsonImpl();
	private final JsonConverter converter = new JsonConverter();
	
	@Override
	public String writePrincipal(EtmPrincipal etmPrincipal) {
		final StringBuilder sb = new StringBuilder();
		boolean added = false;
		sb.append("{");
		added = this.converter.addStringElementToJsonBuffer(this.tags.getIdTag(), etmPrincipal.getId(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getFilterQueryTag(), etmPrincipal.getFilterQuery(), true, sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getFilterQueryTag(), etmPrincipal.getFilterQuery(), true, sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getFilterQueryOccurrenceTag(), etmPrincipal.getFilterQueryOccurrence().name(), true, sb, !added) || added;
		added = this.converter.addBooleanElementToJsonBuffer(this.tags.getAlwaysShowCorrelatedEventsTag(), etmPrincipal.isAlwaysShowCorrelatedEvents(), sb, !added) || added;
		added = this.converter.addIntegerElementToJsonBuffer(this.tags.getSearchHistorySizeTag(), etmPrincipal.getHistorySize(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getLocaleTag(), etmPrincipal.getLocale().toLanguageTag(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getNameTag(), etmPrincipal.getName(), true, sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getPasswordHashTag(), etmPrincipal.getPasswordHash(), sb, !added) || added;
		added = this.converter.addBooleanElementToJsonBuffer(this.tags.getChangePasswordOnLogonTag(), etmPrincipal.isChangePasswordOnLogon(), sb, !added) || added;
		added = this.converter.addSetElementToJsonBuffer(this.tags.getRolesTag(), etmPrincipal.getRoles().stream().map(c -> c.getRoleName()).collect(Collectors.toSet()), true, sb, !added) || added;
		added = this.converter.addSetElementToJsonBuffer(this.tags.getGroupsTag(), etmPrincipal.getGroups().stream().map(c -> c.getName()).collect(Collectors.toSet()), true, sb, !added) || added;
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
		boolean added = false;
		sb.append("{");
		added = this.converter.addStringElementToJsonBuffer(this.tags.getNameTag(), etmGroup.getName(), true, sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getFilterQueryTag(), etmGroup.getFilterQuery(), true, sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getFilterQueryOccurrenceTag(), etmGroup.getFilterQueryOccurrence().name(), true, sb, !added) || added;
		added = this.converter.addBooleanElementToJsonBuffer(this.tags.getAlwaysShowCorrelatedEventsTag(), etmGroup.isAlwaysShowCorrelatedEvents(), sb, !added) || added;
		added = this.converter.addSetElementToJsonBuffer(this.tags.getRolesTag(), etmGroup.getRoles().stream().map(c -> c.getRoleName()).collect(Collectors.toSet()), true, sb, !added) || added;
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
		principal.setFilterQuery(this.converter.getString(this.tags.getFilterQueryTag(), valueMap));
		principal.setFilterQueryOccurrence(QueryOccurrence.valueOf(this.converter.getString(this.tags.getFilterQueryOccurrenceTag(), valueMap)));
		principal.setAlwaysShowCorrelatedEvents(this.converter.getBoolean(this.tags.getAlwaysShowCorrelatedEventsTag(), valueMap));
		principal.setHistorySize(this.converter.getInteger(this.tags.getSearchHistorySizeTag(), valueMap, EtmPrincipal.DEFAULT_HISTORY_SIZE));
		principal.setChangePasswordOnLogon(this.converter.getBoolean(this.tags.getChangePasswordOnLogonTag(), valueMap, Boolean.FALSE));
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
			principal.addRoles(roles.stream().map(c -> EtmPrincipalRole.fromRoleName(c)).collect(Collectors.toSet()));
		}
		return principal;
	}

	public EtmGroup readGroup(Map<String, Object> valueMap) {
		EtmGroup group = new EtmGroup(this.converter.getString(this.tags.getNameTag(), valueMap));
		group.setFilterQuery(this.converter.getString(this.tags.getFilterQueryTag(), valueMap));
		group.setFilterQueryOccurrence(QueryOccurrence.valueOf(this.converter.getString(this.tags.getFilterQueryOccurrenceTag(), valueMap)));
		group.setAlwaysShowCorrelatedEvents(this.converter.getBoolean(this.tags.getAlwaysShowCorrelatedEventsTag(), valueMap));
		List<String> roles = this.converter.getArray(this.tags.getRolesTag(), valueMap);
		if (roles != null) {
			group.addRoles(roles.stream().map(c -> EtmPrincipalRole.fromRoleName(c)).collect(Collectors.toSet()));
		}
		return group;
	}

	@Override
	public EtmPrincipalTags getTags() {
		return this.tags;
	}

}
