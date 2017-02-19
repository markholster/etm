package com.jecstar.etm.server.core.configuration.converter;

import com.jecstar.etm.server.core.configuration.EtmConfiguration;

public interface EtmConfigurationConverter<T> {

	T write(EtmConfiguration nodeConfiguration, EtmConfiguration defaultConfiguration);

	EtmConfiguration read(T nodeContent, T defaultContent, String nodeName);
	
	EtmConfigurationTags getTags();
}
