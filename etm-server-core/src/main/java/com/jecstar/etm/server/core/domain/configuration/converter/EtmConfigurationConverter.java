package com.jecstar.etm.server.core.domain.configuration.converter;

import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;

public interface EtmConfigurationConverter<T> {

    T write(EtmConfiguration nodeConfiguration, EtmConfiguration defaultConfiguration);

    EtmConfiguration read(T nodeContent, T defaultContent, String nodeName);

    /**
     * Gives the active number of nodes of a json string.
     *
     * @param json The json string that contains the node configuration
     * @return The number of active nodes that are broadcasted by the <code>InstanceBroadcaster</code>
     */
    int getActiveNodeCount(String json);

    EtmConfigurationTags getTags();
}
