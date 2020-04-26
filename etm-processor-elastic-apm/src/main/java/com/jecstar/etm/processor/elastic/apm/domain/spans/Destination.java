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

package com.jecstar.etm.processor.elastic.apm.domain.spans;

import com.jecstar.etm.server.core.converter.JsonField;

/**
 * An object containing contextual data about the destination for spans
 */
public class Destination {

    @JsonField("address")
    private String address;
    @JsonField("port")
    private Long port;
    @JsonField(value = "service", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.spans.ServiceConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.spans.Service service;

    /**
     * Destination network address: hostname (e.g. 'localhost'), FQDN (e.g. 'elastic.co'), IPv4 (e.g. '127.0.0.1') or IPv6 (e.g. '::1')
     */
    public String getAddress() {
        return this.address;
    }

    /**
     * Destination network port (e.g. 443)
     */
    public Long getPort() {
        return this.port;
    }

    /**
     * Destination service context
     */
    public com.jecstar.etm.processor.elastic.apm.domain.spans.Service getService() {
        return this.service;
    }
}