package com.jecstar.etm.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class EndpointHandler {

    /**
     * Enum representing the endpoint handler type. An endpointhandler can read or writer from/to an Endpoint.
     */
    public enum EndpointHandlerType {
        READER, WRITER;

        public static EndpointHandlerType safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return EndpointHandlerType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /**
     * The <code>EndpointHandlerType</code> determines if this handler was reading from an <code>Endpoint</code>,
     * or writing to an <code>Endpoint</code>.
     */
    public EndpointHandlerType type;

    /**
     * The <code>Application</code> that is handling the endpoint.
     */
    public Application application = new Application();

    /**
     * The <code>Location</code> the handling took place.
     */
    public Location location = new Location();

    /**
     * The time the handling took place.
     */
    public Instant handlingTime;

    /**
     * The ID of the transaction this event belongs to. Events with the same
     * transactionId form and end-to-end chain within the application.
     */
    public String transactionId;

    /**
     * The sequenceNumber within this transactionId. When 2 events occur at the exact same <code>handlingTime</code>
     * the secuenceNumber can be used to determine which one happened first.
     */
    public Integer sequenceNumber;

    /**
     * Metadata of the endpoint handler. Not used by the application, but can be filled by the end user.
     */
    public Map<String, Object> metadata = new HashMap<>();

    //READ ONLY FIELDS
    /**
     * The time between the write and read of the event. This value is only
     * filled when this <code>EndpointHandler</code> is a reading endpoint
     * handler on an <code>Endpoint</code>. Calculation of this value is done in
     * the persister, or in the elastic update script and will only be filled
     * here when the event is read from the database.
     */
    public Long latency;

    /**
     * The time between the handling time of a request and response. This value
     * is only filled when the event is a request (for example a messaging
     * request). The value is calculated by the elastic update script and will
     * only be filled here when the event is read from the database.
     */
    public Long responseTime;

    public EndpointHandler initialize() {
        this.type = null;
        this.application.initialize();
        this.location.initialize();
        this.handlingTime = null;
        this.transactionId = null;
        this.sequenceNumber = null;
        this.metadata.clear();
        // Initialize read only fields.
        this.latency = null;
        this.responseTime = null;
        return this;
    }

    public EndpointHandler initialize(EndpointHandler copy) {
        initialize();
        if (copy == null) {
            return this;
        }
        this.type = copy.type;
        this.application.initialize(copy.application);
        this.location.initialize(copy.location);
        this.handlingTime = copy.handlingTime;
        this.transactionId = copy.transactionId;
        this.sequenceNumber = copy.sequenceNumber;
        this.metadata.putAll(copy.metadata);
        // Initialize read only fields.
        this.latency = copy.latency;
        this.responseTime = copy.responseTime;
        return this;
    }

    public boolean isSet() {
        return this.type != null && (this.handlingTime != null || this.transactionId != null || this.location.isSet() || this.application.isSet());
    }

    public long getCalculatedHash() {
        long hash = 7;
        if (this.type != null) {
            hash = hash * 31 + this.type.name().hashCode();
        }
        if (this.handlingTime != null) {
            hash = hash * 31 + this.handlingTime.toEpochMilli();
        }
        if (this.transactionId != null) {
            for (int i = 0; i < this.transactionId.length(); i++) {
                hash = hash * 31 + this.transactionId.charAt(i);
            }
        }
        if (this.sequenceNumber != null) {
            hash = hash * 31 + sequenceNumber;
        }
        if (this.application.name != null) {
            for (int i = 0; i < this.application.name.length(); i++) {
                hash = hash * 31 + this.application.name.charAt(i);
            }
        }
        return hash;
    }

}
