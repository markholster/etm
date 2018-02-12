package com.jecstar.etm.server.core.domain.configuration.converter;

import com.jecstar.etm.server.core.domain.configuration.LdapConfiguration;

public interface LdapConfigurationConverter<T> {

    LdapConfiguration read(T content);

    T write(LdapConfiguration directory);

    LdapConfigurationTags getTags();

}
