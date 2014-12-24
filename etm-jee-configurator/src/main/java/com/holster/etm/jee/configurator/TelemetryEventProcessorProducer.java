package com.holster.etm.jee.configurator;

import java.util.Properties;
import java.util.concurrent.Executors;

import javax.annotation.ManagedBean;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrServer;

import com.datastax.driver.core.Session;
import com.holster.etm.processor.processor.TelemetryEventProcessor;

@ManagedBean
public class TelemetryEventProcessorProducer {

	@EtmConfiguration
	@Inject
	private Properties configration;
	
	@Inject
	private Session session;
	
	@Inject
	private SolrServer solrServer;
	
	private TelemetryEventProcessor telemetryEventProcessor;

	@Produces
	public TelemetryEventProcessor getTelemetryEventProcessor() {
		synchronized (this) {
	        if (this.telemetryEventProcessor == null) {
	        	this.telemetryEventProcessor = new TelemetryEventProcessor();
	        	int enhancingHandlerCount = Integer.valueOf(this.configration.getProperty("etm.enhancing_handler_count", "5"));
	        	int indexingHandlerCount = Integer.valueOf(this.configration.getProperty("etm.indexing_handler_count", "5"));
	        	int persistingHandlerCount = Integer.valueOf(this.configration.getProperty("etm.persisting_handler_count", "5"));
	        	int ringbufferSize = Integer.valueOf(this.configration.getProperty("etm.ringbuffer_size", "4096"));
	        	this.telemetryEventProcessor.start(Executors.newCachedThreadPool(), this.session, this.solrServer, ringbufferSize, enhancingHandlerCount, indexingHandlerCount, persistingHandlerCount);
	        }
        }
		return this.telemetryEventProcessor;
	}
}
