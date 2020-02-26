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

package com.jecstar.etm.gui.rest.services.search.graphs;

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
    protected void doWriteToJson(StringBuilder buffer) {
        jsonWriter.addStringElementToJsonBuffer("name", getName(), buffer, false);
        jsonWriter.addStringElementToJsonBuffer("event_id", getEventId(), buffer, false);
        jsonWriter.addInstantElementToJsonBuffer("write_time", getWriteTime(), buffer, false);
        if (!this.readTimes.isEmpty()) {
            Collections.sort(this.readTimes);
            jsonWriter.addInstantElementToJsonBuffer("first_read_time", this.readTimes.get(0), buffer, false);
        }
        if (getProtocolType() != null) {
            jsonWriter.addStringElementToJsonBuffer("protocol", getProtocolType().name(), buffer, false);
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

    @Override
    public String toString() {
        return "Endpoint{" +
                "vertexId='" + getVertexId() + '\'' +
                ", name='" + name + '\'' +
                ", eventId='" + eventId + '\'' +
                '}';
    }
}
