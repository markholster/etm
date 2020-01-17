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
