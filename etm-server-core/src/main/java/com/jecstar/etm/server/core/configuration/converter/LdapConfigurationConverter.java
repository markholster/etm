package com.jecstar.etm.server.core.configuration.converter;

import com.jecstar.etm.server.core.configuration.LdapConfiguration;

public interface LdapConfigurationConverter<T> {

	LdapConfiguration read(T content);
	T write(LdapConfiguration directory);
	
	LdapConfigurationTags getTags();

}
