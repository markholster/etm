package com.jecstar.etm.core.domain.converter.json;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.jecstar.etm.core.domain.EtmPrincipal;
import com.jecstar.etm.core.domain.EtmPrincipal.PrincipalRole;
import com.jecstar.etm.core.domain.converter.EtmPrincipalConverter;
import com.jecstar.etm.core.domain.converter.EtmPrincipalConverterTags;

public class EtmPrincipalConverterJsonImpl extends AbstractJsonConverter implements EtmPrincipalConverter<String> {

	private final EtmPrincipalConverterTags tags = new EtmPrincipalConverterTagsJsonImpl();
	
	@Override
	public String convert(EtmPrincipal etmPrincipal) {
		final StringBuilder sb = new StringBuilder();
		boolean added = false;
		sb.append("{");
		added = addStringElementToJsonBuffer(this.tags.getIdTag(), etmPrincipal.getId(), sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getFilterQueryTag(), etmPrincipal.getFilterQuery(), sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getLocaleTag(), etmPrincipal.getLocale().toLanguageTag(), sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getNameTag(), etmPrincipal.getName(), sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getPasswordHashTag(), etmPrincipal.getPasswordHash(), sb, !added) || added;
		added = addSetElementToJsonBuffer(this.tags.getRolesTag(), etmPrincipal.getRoles().stream().map(c -> c.getRoleName()).collect(Collectors.toSet()), sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getTimeZoneTag(), etmPrincipal.getTimeZone().getID(), sb, !added) || added;
		sb.append("}");
		return sb.toString();
	}

	@Override
	public EtmPrincipal convert(String jsonContent) {
		Map<String, Object> valueMap = toMap(jsonContent);
		EtmPrincipal principal = new EtmPrincipal(getString(this.tags.getIdTag(), valueMap),getString(this.tags.getPasswordHashTag(), valueMap));
		principal.setName(getString(this.tags.getNameTag(), valueMap));
		principal.setFilterQuery(getString(this.tags.getFilterQueryTag(), valueMap));
		String value = getString(this.tags.getLocaleTag(), valueMap);
		if (value != null) {
			principal.setLocale(Locale.forLanguageTag(value));
		}
		value = getString(this.tags.getTimeZoneTag(), valueMap);
		if (value != null) {
			principal.setTimeZone(TimeZone.getTimeZone(value));
		}
		List<String> roles = getArray(this.tags.getRolesTag(), valueMap);
		
		principal.addRoles(roles.stream().map(c -> PrincipalRole.valueOf(c.toUpperCase())).collect(Collectors.toSet()));
		return principal;
	}

	@Override
	public EtmPrincipalConverterTags getTags() {
		return this.tags;
	}

}
