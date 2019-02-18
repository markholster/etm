package com.jecstar.etm.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Endpoint {

    /**
     * The name of the endpoint
     */
    public String name;

    /**
     * The handlers that were reading or writing the event. Each event can have a single writing event handler at most.
     */
    private final List<EndpointHandler> endpointHandlers = new ArrayList<>();


    public void initialize() {
        this.name = null;
        this.endpointHandlers.clear();
    }

    public void initialize(Endpoint copy) {
        this.name = copy.name;
        this.endpointHandlers.clear();
        for (EndpointHandler endpointHandler : copy.endpointHandlers) {
            EndpointHandler copyEndpointHandler = new EndpointHandler();
            copyEndpointHandler.initialize(endpointHandler);
            this.endpointHandlers.add(copyEndpointHandler);
        }
    }

    /**
     * Add an <code>EndpointHandler</code> to this <code>Endpoint</code>.
     * <p>
     * When the <code>EndpointHandler</code> has the {@link EndpointHandler#type} set to {@link EndpointHandler.EndpointHandlerType#WRITER} and
     * this <code>Endpoint</code> already has a writer in it's handler list the <code>EndpointHandler</code> will not
     * be added to the list.
     * <p>
     * If the given <code>EndpointHandler</code> has an empty {@link EndpointHandler#type} this method will do nothing.
     *
     * @param endpointHandler The <code>EndpointHandler</code> to add.
     */
    public void addEndpointHandler(EndpointHandler endpointHandler) {
        if (endpointHandler.type == null) {
            return;
        }
        if (!endpointHandler.isSet()) {
            return;
        }
        if (this.endpointHandlers.contains(endpointHandler)) {
            return;
        }
        if (EndpointHandler.EndpointHandlerType.WRITER.equals(endpointHandler.type) && getWritingEndpointHandler() != null) {
            return;
        }
        this.endpointHandlers.add(endpointHandler);
    }

    public EndpointHandler getWritingEndpointHandler() {
        Optional<EndpointHandler> optionalHandler = this.endpointHandlers.stream().filter(p -> EndpointHandler.EndpointHandlerType.WRITER.equals(p.type)).findFirst();
        if (optionalHandler.isPresent()) {
            return optionalHandler.get();
        }
        return null;
    }

    public List<EndpointHandler> getReadingEndpointHandlers() {
        return this.endpointHandlers.stream().filter(p -> EndpointHandler.EndpointHandlerType.READER.equals(p.type)).collect(Collectors.toList());
    }

    public List<EndpointHandler> getEndpointHandlers() {
        return this.endpointHandlers;
    }

    public Instant getEarliestHandlingTime() {
        Optional<EndpointHandler> optionalHandler = this.endpointHandlers.stream().filter(p -> EndpointHandler.EndpointHandlerType.WRITER.equals(p.type)).findFirst();
        if (optionalHandler.isPresent()) {
            return optionalHandler.get().handlingTime;
        }
        return getEarliestReadTime();
    }

    public Instant getEarliestReadTime() {
        Instant earliest = null;
        for (EndpointHandler endpointHandler : this.endpointHandlers) {
            if (earliest == null || (EndpointHandler.EndpointHandlerType.READER.equals(endpointHandler.type) && endpointHandler.handlingTime != null && endpointHandler.handlingTime.isBefore(earliest))) {
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
        for (EndpointHandler endpointHandler : this.endpointHandlers) {
            hash = hash * 31 + endpointHandler.getCalculatedHash();
        }
        return hash;

    }
}
