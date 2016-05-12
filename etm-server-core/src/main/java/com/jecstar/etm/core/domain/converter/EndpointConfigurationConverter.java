package com.jecstar.etm.core.domain.converter;

import com.jecstar.etm.core.domain.EndpointConfiguration;

public interface EndpointConfigurationConverter<T> {

	EndpointConfiguration convert(T content);
	
	EndpointConfigurationConverterTags getTags();
}
