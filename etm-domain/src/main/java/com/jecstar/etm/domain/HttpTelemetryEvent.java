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

package com.jecstar.etm.domain;

import java.time.Instant;

/**
 * A <code>TelemetryEvent</code> that describes an http request or response sent or received from a web server.
 */
public class HttpTelemetryEvent extends TelemetryEvent<HttpTelemetryEvent> {

    public enum HttpEventType {
        CONNECT, DELETE, GET, HEAD, OPTIONS, POST, PUT, TRACE, RESPONSE;

        public static HttpEventType safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return HttpEventType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /**
     * The http type that is represented by this event.
     */
    public HttpEventType httpEventType;

    /**
     * The moment this event expires, in case of a request. This is the time the
     * even occurred added with the request timeout. This value should usually
     * be calculated by the client that is sending the http request.
     */
    public Instant expiry;

    /**
     * The http status codes returned by the server. This is only relevant when the {@link #httpEventType} is set to RESPONSE.
     */
    public Integer statusCode;


    @Override
    public HttpTelemetryEvent initialize() {
        super.internalInitialize();
        this.expiry = null;
        this.httpEventType = null;
        this.statusCode = null;
        return this;
    }

    @Override
    public HttpTelemetryEvent initialize(HttpTelemetryEvent copy) {
        super.internalInitialize(copy);
        this.expiry = copy.expiry;
        this.httpEventType = copy.httpEventType;
        this.statusCode = copy.statusCode;
        return this;
    }
}
