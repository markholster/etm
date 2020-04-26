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

/**
 * Metadata concerning the other objects in the stream.
 */
public class Metadata {

    @JsonField(value = "service", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.ServiceConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Service service;
    @JsonField(value = "process", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.ProcessConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Process process;
    @JsonField(value = "system", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.SystemConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.System system;
    @JsonField(value = "user", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.UserConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.User user;
    @JsonField(value = "labels", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.TagsConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Tags labels;

    public com.jecstar.etm.processor.elastic.apm.domain.Service getService() {
        return this.service;
    }

    public com.jecstar.etm.processor.elastic.apm.domain.Process getProcess() {
        return this.process;
    }

    public com.jecstar.etm.processor.elastic.apm.domain.System getSystem() {
        return this.system;
    }

    public com.jecstar.etm.processor.elastic.apm.domain.User getUser() {
        return this.user;
    }

    /**
     * A flat mapping of user-defined tags with string, boolean or number values.
     */
    public com.jecstar.etm.processor.elastic.apm.domain.Tags getLabels() {
        return this.labels;
    }
}