package com.jecstar.etm.server.core.domain.converter;

import com.jecstar.etm.server.core.domain.EndpointConfiguration;

public interface EndpointConfigurationConverter<T> {

    EndpointConfiguration read(T content);

    T write(EndpointConfiguration endpointConfiguration);

    EndpointConfigurationTags getTags();
}
