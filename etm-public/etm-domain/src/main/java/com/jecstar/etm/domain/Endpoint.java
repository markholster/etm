package com.jecstar.etm.domain;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class Endpoint {

    /**
     * The name of the endpoint
     */
    public String name;

    /**
     * The handlers that were reading the event.
     */
    public final List<EndpointHandler> readingEndpointHandlers = new ArrayList<>();

    /**
     * The handler that was writing the event.
     */
    public EndpointHandler writingEndpointHandler = new EndpointHandler();

    public void initialize() {
        this.name = null;
        this.readingEndpointHandlers.clear();
        this.writingEndpointHandler.initialize();
    }

    public void initialize(Endpoint copy) {
        this.name = copy.name;
        this.readingEndpointHandlers.clear();
        for (EndpointHandler endpointHandler : copy.readingEndpointHandlers) {
            EndpointHandler copyEndpointHandler = new EndpointHandler();
            copyEndpointHandler.initialize(endpointHandler);
            this.readingEndpointHandlers.add(copyEndpointHandler);
        }
        this.writingEndpointHandler.initialize(copy.writingEndpointHandler);
    }

    public ZonedDateTime getEarliestHandlingTime() {
        if (this.writingEndpointHandler.handlingTime != null) {
            return this.writingEndpointHandler.handlingTime;
        }
        return getEarliestReadTime();
    }

    public ZonedDateTime getEarliestReadTime() {
        ZonedDateTime earliest = null;
        for (EndpointHandler endpointHandler : this.readingEndpointHandlers) {
            if (earliest == null || (endpointHandler.handlingTime != null && endpointHandler.handlingTime.isBefore(earliest))) {
                earliest = endpointHandler.handlingTime;
            }
        }
        return earliest;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Endpoint) {
            Endpoint other = (Endpoint) obj;
            if (this.name == null ^ other.name == null) {
                return false;
            } else if (this.name == null && other.name == null) {
                return true;
            }
            return this.name.equals(other.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.name == null) {
            return 1;
        }
        return this.name.hashCode();
    }

    public long getCalculatedHash() {
        long hash = 7;
        if (this.name != null) {
            for (int i = 0; i < this.name.length(); i++) {
                hash = hash * 31 + this.name.charAt(i);
            }
        }
        hash = hash * 31 + this.writingEndpointHandler.getCalculatedHash();
        for (EndpointHandler endpointHandler : this.readingEndpointHandlers) {
            hash = hash * 31 + endpointHandler.getCalculatedHash();
        }
        return hash;

    }
}
