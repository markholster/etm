package com.jecstar.etm.server.core.domain;

import java.util.ArrayList;
import java.util.List;

import com.jecstar.etm.server.core.enhancers.BusinessTelemetryEventEnhancer;
import com.jecstar.etm.server.core.enhancers.HttpTelemetryEventEnhancer;
import com.jecstar.etm.server.core.enhancers.LogTelemetryEventEnhancer;
import com.jecstar.etm.server.core.enhancers.MessagingTelemetryEventEnhancer;
import com.jecstar.etm.server.core.enhancers.SqlTelemetryEventEnhancer;

public class EndpointConfiguration {

	public List<BusinessTelemetryEventEnhancer> businessEventEnhancers = new ArrayList<BusinessTelemetryEventEnhancer>();
	public List<HttpTelemetryEventEnhancer> httpEventEnhancers = new ArrayList<HttpTelemetryEventEnhancer>();
	public List<LogTelemetryEventEnhancer> logEventEnhancers = new ArrayList<LogTelemetryEventEnhancer>();
	public List<MessagingTelemetryEventEnhancer> messagingEventEnhancers = new ArrayList<MessagingTelemetryEventEnhancer>();
	public List<SqlTelemetryEventEnhancer> sqlEventEnhancers = new ArrayList<SqlTelemetryEventEnhancer>();
	
	// process state
	public long retrievalTimestamp;

	public EndpointConfiguration initialize() {
		if (this.businessEventEnhancers != null) {
			this.businessEventEnhancers.clear();
		}
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
