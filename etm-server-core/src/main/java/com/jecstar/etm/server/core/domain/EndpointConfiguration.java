package com.jecstar.etm.server.core.domain;

import com.jecstar.etm.server.core.enhancers.TelemetryEventEnhancer;

public class EndpointConfiguration {

    public String name;
    public TelemetryEventEnhancer eventEnhancer;

    public EndpointConfiguration initialize() {
        this.name = null;
        this.eventEnhancer = null;
        return this;
    }
}
