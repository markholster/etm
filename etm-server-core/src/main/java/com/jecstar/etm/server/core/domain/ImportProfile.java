package com.jecstar.etm.server.core.domain;

import com.jecstar.etm.server.core.enhancers.TelemetryEventEnhancer;

public class ImportProfile {

    public String name;
    public TelemetryEventEnhancer eventEnhancer;

    public ImportProfile initialize() {
        this.name = null;
        this.eventEnhancer = null;
        return this;
    }
}
