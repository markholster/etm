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

public class Service {

    @JsonField(value = "agent", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.AgentConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Agent agent;
    @JsonField(value = "framework", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.FrameworkConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Framework framework;
    @JsonField(value = "language", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.LanguageConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Language language;
    @JsonField("name")
    private String name;
    @JsonField("environment")
    private String environment;
    @JsonField(value = "runtime", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.RuntimeConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Runtime runtime;
    @JsonField("version")
    private String version;
    @JsonField(value = "node", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.NodeConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Node node;

    /**
     * Name and version of the Elastic APM agent
     */
    public com.jecstar.etm.processor.elastic.apm.domain.Agent getAgent() {
        return this.agent;
    }

    /**
     * Name and version of the web framework used
     */
    public com.jecstar.etm.processor.elastic.apm.domain.Framework getFramework() {
        return this.framework;
    }

    /**
     * Name and version of the programming language used
     */
    public com.jecstar.etm.processor.elastic.apm.domain.Language getLanguage() {
        return this.language;
    }

    /**
     * Immutable name of the service emitting this event
     */
    public String getName() {
        return this.name;
    }

    /**
     * Environment name of the service, e.g. "production" or "staging"
     */
    public String getEnvironment() {
        return this.environment;
    }

    /**
     * Name and version of the language runtime running this service
     */
    public com.jecstar.etm.processor.elastic.apm.domain.Runtime getRuntime() {
        return this.runtime;
    }

    /**
     * Version of the service emitting this event
     */
    public String getVersion() {
        return this.version;
    }

    /**
     * Unique meaningful name of the service node.
     */
    public com.jecstar.etm.processor.elastic.apm.domain.Node getNode() {
        return this.node;
    }
}