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

package com.jecstar.etm.processor.elastic.apm.domain;

import com.jecstar.etm.server.core.converter.JsonField;

public class System {

    @JsonField("architecture")
    private String architecture;
    @JsonField("hostname")
    private String hostname;
    @JsonField("detected_hostname")
    private String detectedHostname;
    @JsonField("configured_hostname")
    private String configuredHostname;
    @JsonField("platform")
    private String platform;

    /**
     * Architecture of the system the agent is running on.
     */
    public String getArchitecture() {
        return this.architecture;
    }

    /**
     * Deprecated. Hostname of the system the agent is running on. Will be ignored if kubernetes information is set.
     */
    public String getHostname() {
        return this.hostname;
    }

    /**
     * Hostname of the host the monitored service is running on. It normally contains what the hostname command returns on the host machine. Will be ignored if kubernetes information is set, otherwise should always be set.
     */
    public String getDetectedHostname() {
        return this.detectedHostname;
    }

    /**
     * Name of the host the monitored service is running on. It should only be set when configured by the user. If empty, will be set to detected_hostname or derived from kubernetes information if provided.
     */
    public String getConfiguredHostname() {
        return this.configuredHostname;
    }

    /**
     * Name of the system platform the agent is running on.
     */
    public String getPlatform() {
        return this.platform;
    }
}