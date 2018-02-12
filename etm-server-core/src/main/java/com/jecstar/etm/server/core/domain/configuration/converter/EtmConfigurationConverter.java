package com.jecstar.etm.server.core.domain.configuration.converter;

import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;

public interface EtmConfigurationConverter<T> {

    T write(EtmConfiguration nodeConfiguration, EtmConfiguration defaultConfiguration);

    EtmConfiguration read(T nodeContent, T defaultContent, String nodeName);

    EtmConfigurationTags getTags();
}
