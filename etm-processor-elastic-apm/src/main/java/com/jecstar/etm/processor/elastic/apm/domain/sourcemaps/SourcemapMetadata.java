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

package com.jecstar.etm.processor.elastic.apm.domain.sourcemaps;

import com.jecstar.etm.server.core.converter.JsonField;

/**
 * Sourcemap Metadata
 */
public class SourcemapMetadata {

    @JsonField("bundle_filepath")
    private String bundleFilepath;
    @JsonField("service_version")
    private String serviceVersion;
    @JsonField("service_name")
    private String serviceName;

    /**
     * relative path of the minified bundle file
     */
    public String getBundleFilepath() {
        return this.bundleFilepath;
    }

    /**
     * Version of the service emitting this event
     */
    public String getServiceVersion() {
        return this.serviceVersion;
    }

    /**
     * Immutable name of the service emitting this event
     */
    public String getServiceName() {
        return this.serviceName;
    }
}