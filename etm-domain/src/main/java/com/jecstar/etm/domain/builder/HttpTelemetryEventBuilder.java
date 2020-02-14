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

import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.HttpTelemetryEvent.HttpEventType;

import java.time.Instant;

public class HttpTelemetryEventBuilder extends TelemetryEventBuilder<HttpTelemetryEvent, HttpTelemetryEventBuilder> {

    public HttpTelemetryEventBuilder() {
        super(new HttpTelemetryEvent());
    }

    public HttpTelemetryEventBuilder setExpiry(Instant expiry) {
        this.event.expiry = expiry;
        return this;
    }

    public HttpTelemetryEventBuilder setHttpEventType(HttpEventType httpEventType) {
        this.event.httpEventType = httpEventType;
        return this;
    }

    public HttpEventType getHttpEventType() {
        return this.event.httpEventType;
    }

    public HttpTelemetryEventBuilder setStatusCode(Integer statusCode) {
        this.event.statusCode = statusCode;
        return this;
    }
}
