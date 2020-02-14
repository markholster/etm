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

package com.jecstar.etm.domain.builder;

import com.jecstar.etm.domain.Application;
import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.Location;

import java.time.Instant;
import java.util.Map;

public class EndpointHandlerBuilder {

    private final EndpointHandler endpointHandler;

    public EndpointHandlerBuilder() {
        this.endpointHandler = new EndpointHandler();
    }

    public EndpointHandler build() {
        return this.endpointHandler;
    }

    public EndpointHandlerBuilder setType(EndpointHandler.EndpointHandlerType type) {
        this.endpointHandler.type = type;
        return this;
    }

    public EndpointHandlerBuilder setHandlingTime(Instant handlingTime) {
        this.endpointHandler.handlingTime = handlingTime;
        return this;
    }

    public EndpointHandlerBuilder setTransactionId(String transactionId) {
        this.endpointHandler.transactionId = transactionId;
        return this;
    }

    public EndpointHandlerBuilder setSequenceNumber(Integer sequenceNumber) {
        this.endpointHandler.sequenceNumber = sequenceNumber;
        return this;
    }

    public EndpointHandlerBuilder setApplication(Application application) {
        this.endpointHandler.application = application;
        return this;
    }

    public EndpointHandlerBuilder setApplication(ApplicationBuilder applicationBuilder) {
        this.endpointHandler.application = applicationBuilder.build();
        return this;
    }

    public EndpointHandlerBuilder setLocation(Location location) {
        this.endpointHandler.location = location;
        return this;
    }

    public EndpointHandlerBuilder setLocation(LocationBuilder locationBuilder) {
        this.endpointHandler.location = locationBuilder.build();
        return this;
    }

    public EndpointHandlerBuilder setMetadata(Map<String, Object> metadata) {
        this.endpointHandler.metadata = metadata;
        return this;
    }

    public EndpointHandlerBuilder addMetadata(Map<String, Object> metadata) {
        this.endpointHandler.metadata.putAll(metadata);
        return this;
    }

    public EndpointHandlerBuilder addMetadata(String key, Object value) {
        this.endpointHandler.metadata.put(key, value);
        return this;
    }

    public Map<String, Object> getMetadata() {
        return this.endpointHandler.metadata;
    }


}
