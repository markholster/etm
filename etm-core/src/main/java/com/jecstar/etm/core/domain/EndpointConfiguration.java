package com.jecstar.etm.core.domain;

import java.util.ArrayList;
import java.util.List;

import com.jecstar.etm.core.enhancers.HttpTelemetryEventEnhancer;
import com.jecstar.etm.core.enhancers.LogTelemetryEventEnhancer;
import com.jecstar.etm.core.enhancers.MessagingTelemetryEventEnhancer;
import com.jecstar.etm.core.enhancers.SqlTelemetryEventEnhancer;

public class EndpointConfiguration {

	public List<HttpTelemetryEventEnhancer> httpEventEnhancers = new ArrayList<HttpTelemetryEventEnhancer>();
	public List<LogTelemetryEventEnhancer> logEventEnhancers = new ArrayList<LogTelemetryEventEnhancer>();
	public List<MessagingTelemetryEventEnhancer> messagingEventEnhancers = new ArrayList<MessagingTelemetryEventEnhancer>();
	public List<SqlTelemetryEventEnhancer> sqlEventEnhancers = new ArrayList<SqlTelemetryEventEnhancer>();
	
	// process state
	public long retrievalTimestamp;

	public EndpointConfiguration initialize() {
	    if (this.httpEventEnhancers != null) {
	    	this.httpEventEnhancers.clear();
	    }
	    if (this.logEventEnhancers != null) {
	    	this.logEventEnhancers.clear();
	    }
	    if (this.messagingEventEnhancers != null) {
	    	this.messagingEventEnhancers.clear();
	    }
	    if (this.sqlEventEnhancers != null) {
	    	this.sqlEventEnhancers.clear();
	    }
	    return this;
    }
}
