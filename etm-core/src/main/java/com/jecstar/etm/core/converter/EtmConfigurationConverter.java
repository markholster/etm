package com.jecstar.etm.core.converter;

import com.jecstar.etm.core.configuration.EtmConfiguration;

public interface EtmConfigurationConverter<T> {

	T convert(EtmConfiguration nodeConfiguration, EtmConfiguration defaultConfiguration);
	EtmConfiguration convert(T nodeContent, T defaultContent, String nodeName, String component);
	
	EtmConfigurationConverterTags getTags();
}
