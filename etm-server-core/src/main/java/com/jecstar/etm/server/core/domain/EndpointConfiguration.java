package com.jecstar.etm.server.core.domain;

import java.util.ArrayList;
import java.util.List;

import com.jecstar.etm.server.core.enhancers.TelemetryEventEnhancer;

public class EndpointConfiguration {

	public List<TelemetryEventEnhancer> eventEnhancers = new ArrayList<TelemetryEventEnhancer>();
	
	// process state
	public long retrievalTimestamp;

	public EndpointConfiguration initialize() {
		if (this.eventEnhancers != null) {
			this.eventEnhancers.clear();
		}
	    return this;
    }
}
