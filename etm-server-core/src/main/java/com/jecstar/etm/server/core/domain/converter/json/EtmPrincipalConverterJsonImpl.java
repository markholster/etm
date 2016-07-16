package com.jecstar.etm.server.core.domain.converter.json;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.jecstar.etm.server.core.domain.EtmPrincipal;
import com.jecstar.etm.server.core.domain.EtmPrincipal.PrincipalRole;
import com.jecstar.etm.server.core.domain.converter.EtmPrincipalConverter;
import com.jecstar.etm.server.core.domain.converter.EtmPrincipalTags;

public class EtmPrincipalConverterJsonImpl implements EtmPrincipalConverter<String> {

	private final EtmPrincipalTags tags = new EtmPrincipalTagsJsonImpl();
	private final JsonConverter converter = new JsonConverter();
	
	@Override
	public String write(EtmPrincipal etmPrincipal) {
		final StringBuilder sb = new StringBuilder();
		boolean added = false;
		sb.append("{");
		added = this.converter.addStringElementToJsonBuffer(this.tags.getIdTag(), etmPrincipal.getId(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getFilterQueryTag(), etmPrincipal.getFilterQuery(), true, sb, !added) || added;
		added = this.converter.addIntegerElementToJsonBuffer(this.tags.getQueryHistorySizeTag(), etmPrincipal.getHistorySize(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getLocaleTag(), etmPrincipal.getLocale().toLanguageTag(), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getNameTag(), etmPrincipal.getName(), true, sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getPasswordHashTag(), etmPrincipal.getPasswordHash(), sb, !added) || added;
		added = this.converter.addSetElementToJsonBuffer(this.tags.getRolesTag(), etmPrincipal.getRoles().stream().map(c -> c.getRoleName()).collect(Collectors.toSet()), sb, !added) || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getTimeZoneTag(), etmPrincipal.getTimeZone().getID(), sb, !added) || added;
		sb.append("}");
		return sb.toString();
	}

	@Override
	public EtmPrincipal read(String jsonContent) {
		Map<String, Object> valueMap = this.converter.toMap(jsonContent);
		EtmPrincipal principal = new EtmPrincipal(this.converter.getString(this.tags.getIdTag(), valueMap));
		principal.setPasswordHash(this.converter.getString(this.tags.getPasswordHashTag(), valueMap));
		principal.setName(this.converter.getString(this.tags.getNameTag(), valueMap));
		principal.setFilterQuery(this.converter.getString(this.tags.getFilterQueryTag(), valueMap));
		principal.setHistorySize(this.converter.getInteger(this.tags.getQueryHistorySizeTag(), valueMap, EtmPrincipal.DEFAULT_HISTORY_SIZE));
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
			principal.addRoles(roles.stream().map(c -> PrincipalRole.valueOf(c.toUpperCase())).collect(Collectors.toSet()));
		}
		return principal;
	}

	@Override
	public EtmPrincipalTags getTags() {
		return this.tags;
	}

}
