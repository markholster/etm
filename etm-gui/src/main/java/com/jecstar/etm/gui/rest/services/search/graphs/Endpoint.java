package com.jecstar.etm.gui.rest.services.search.graphs;

import com.jecstar.etm.domain.writer.json.JsonBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Endpoint extends AbstractVertex {

    private final String name;
    private final com.jecstar.etm.domain.Endpoint.ProtocolType protocolType;
    private String eventId;
    private Instant writeTime;
    private List<Instant> readTimes = new ArrayList<>();

    public Endpoint(String vertexId, String name) {
        this(vertexId, name, guessProtocolType(name));
    }

    public Endpoint(String vertexId, String name, com.jecstar.etm.domain.Endpoint.ProtocolType protocolType) {
        super(vertexId);
        this.name = name;
        this.protocolType = protocolType;
    }

    private static com.jecstar.etm.domain.Endpoint.ProtocolType guessProtocolType(String name) {
        if (name == null) {
            return null;
        }
        if (name.toLowerCase().startsWith("https://")) {
            return com.jecstar.etm.domain.Endpoint.ProtocolType.HTTPS;
        } else if (name.toLowerCase().startsWith("http://")) {
            return com.jecstar.etm.domain.Endpoint.ProtocolType.HTTP;
        } else if (name.toLowerCase().startsWith("wmq://") || name.toLowerCase().startsWith("jms://")) {
            return com.jecstar.etm.domain.Endpoint.ProtocolType.MQ;
        }
        return null;
    }

    public String getName() {
        return this.name;
    }

    public com.jecstar.etm.domain.Endpoint.ProtocolType getProtocolType() {
        return this.protocolType;
    }

    @Override
    protected String getType() {
        return "endpoint";
    }

    public Endpoint setEventId(String eventId) {
        this.eventId = eventId;
        return this;
    }

    public String getEventId() {
        return this.eventId;
    }

    public Endpoint setWriteTime(Instant writeTime) {
        this.writeTime = writeTime;
        return this;
    }

    public Instant getWriteTime() {
        return this.writeTime;
    }

    public Endpoint addReadTime(Instant readTime) {
        if (readTime != null) {
            this.readTimes.add(readTime);
        }
        return this;
    }

    public Instant getFirstReadTime() {
        if (this.readTimes.isEmpty()) {
            return null;
        }
        Collections.sort(this.readTimes);
        return this.readTimes.get(0);
    }

    @Override
    protected void doWriteToJson(JsonBuilder builder) {
        builder.field("name", getName());
        builder.field("event_id", getEventId());
        builder.field("write_time", getWriteTime());
        if (!this.readTimes.isEmpty()) {
            Collections.sort(this.readTimes);
            builder.field("first_read_time", this.readTimes.get(0));
        }
        if (getProtocolType() != null) {
            builder.field("protocol", getProtocolType().name());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Endpoint endpoint = (Endpoint) o;
        return Objects.equals(name, endpoint.name) &&
                Objects.equals(eventId, endpoint.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, eventId);
    }
}
