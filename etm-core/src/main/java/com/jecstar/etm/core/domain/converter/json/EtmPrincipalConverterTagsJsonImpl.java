package com.jecstar.etm.core.domain.converter.json;

import com.jecstar.etm.core.domain.converter.EtmPrincipalConverterTags;

public class EtmPrincipalConverterTagsJsonImpl implements EtmPrincipalConverterTags {
	
	@Override
	public String getIdTag() {
		return "id";
	}
	
	@Override
	public String getLocaleTag() {
		return "locale";
	}
	
	@Override
	public String getNameTag() {
		return "name";
	}

	@Override
	public String getPasswordHashTag() {
		return "password_hash";
	}
	
	@Override
	public String getRolesTag() {
		return "roles";
	}

	@Override
	public String getTimeZoneTag() {
		return "time_zone";
	}

}
