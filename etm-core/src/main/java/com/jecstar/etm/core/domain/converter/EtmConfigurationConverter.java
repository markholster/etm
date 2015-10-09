package com.jecstar.etm.core.domain.converter;

import com.jecstar.etm.core.configuration.EtmConfiguration;

public interface EtmConfigurationConverter<T> {

	T convert(EtmConfiguration etmConfiguration, EtmConfiguration defaultConfiguration);
	
	EtmConfigurationConverterTags getTags();
}
