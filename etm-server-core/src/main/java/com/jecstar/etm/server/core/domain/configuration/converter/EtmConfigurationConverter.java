/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

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
