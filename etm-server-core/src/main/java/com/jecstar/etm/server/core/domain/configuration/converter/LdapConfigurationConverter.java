package com.jecstar.etm.server.core.domain.configuration.converter;

import com.jecstar.etm.server.core.converter.JsonEntityConverter;
import com.jecstar.etm.server.core.domain.configuration.LdapConfiguration;

public class LdapConfigurationConverter extends JsonEntityConverter<LdapConfiguration> {

    public LdapConfigurationConverter() {
        super(f -> new LdapConfiguration());
    }
}
