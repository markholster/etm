package com.jecstar.etm.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract superclass for all <code>TelemetryEvent</code> types.
 *
 * @param <T>
 */
public abstract class TelemetryEvent<T extends TelemetryEvent<T>> {

    /**
     * The unique ID of the event.
     */
    public String id;

    /**
     * The ID of the event this event is correlated to. This is mainly used match a response to a certain request.
     */
    public String correlationId;

    /**
     * Data to be used for correlating event's that aren't correlated by the correlation id.
     */
    public Map<String, Object> correlationData = new HashMap<>();

    /**
     * The endpoints this event was send to, and received from.
     */
    public final List<Endpoint> endpoints = new ArrayList<>();

    /**
     * Data to be used to query on.
     */
    public Map<String, Object> extractedData = new HashMap<>();

    /**
     * Metadata of the event. Not used by the application, but can be filled by the end user.
     */
    public Map<String, Object> metadata = new HashMap<>();

    /**
     * The name of the event.
     */
    public String name;

    /**
     * The payload of the event.
     */
    public String payload;

    /**
     * The encoding of the payload. Leave empty if the payload is in plain text.
     */
    public PayloadEncoding payloadEncoding;

    /**
     * The format of the payload. Generally speaking, this is a description of the {@link #payload}.
     */
    public PayloadFormat payloadFormat;


    // READ ONLY FIELDS
    /**
     * A list with event id's that correlate to this event. This is a read only
     * field and will only be filled when the even is read from the database.
     */
    public final List<String> correlations = new ArrayList<>();

    /**
     * Initialize this <code>TelemetryEvent</code> with the default data.
     *
     * @return A fully initialized <code>TelemetryEvent</code>.
     */
    public abstract T initialize();

    final void internalInitialize() {
        this.id = null;
        this.correlationId = null;
        this.correlationData.clear();
        this.endpoints.clear();
        this.extractedData.clear();
        this.metadata.clear();
        this.name = null;
        this.payload = null;
        this.payloadEncoding = null;
        this.payloadFormat = null;
        // Initialize read only fields.
        this.correlations.clear();
    }

    /**
     * Initialize this <code>TelemetryEvent</code> with the data of another
     * <code>TelemetryEvent</code>.
     *
     * @param copy The <code>TelemetryEvent</code> to copy the data from.
     * @return This <code>TelemetryEvent</code> initialized with the data of the
     * given copy.
     */
    public abstract T initialize(T copy);

    final void internalInitialize(TelemetryEvent<?> copy) {
        this.initialize();
        if (copy == null) {
            return;
        }
        this.id = copy.id;
        this.correlationId = copy.correlationId;
        this.correlationData.putAll(copy.correlationData);
        this.endpoints.clear();
        for (Endpoint endpoint : copy.endpoints) {
            Endpoint copyEndpoint = new Endpoint();
            copyEndpoint.initialize(endpoint);
            this.endpoints.add(copyEndpoint);
        }
        this.extractedData.putAll(copy.extractedData);
        this.metadata.putAll(copy.metadata);
        this.name = copy.name;
        this.payload = copy.payload;
        this.payloadEncoding = copy.payloadEncoding;
        this.payloadFormat = copy.payloadFormat;
        // Initialize read only fields.
        this.correlations.addAll(copy.correlations);
    }

    public long getCalculatedHash() {
        long hash = 7;
        if (this.id != null) {
            for (int i = 0; i < this.id.length(); i++) {
                hash = hash * 31 + this.id.charAt(i);
            }
        }
        for (Endpoint endpoint : this.endpoints) {
            hash = hash * 31 + endpoint.getCalculatedHash();
        }
        return hash;
    }
}
