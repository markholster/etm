package com.jecstar.etm.processor.rest;

import javax.annotation.ManagedBean;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.inject.Inject;

import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.jee.configurator.core.ProcessorConfiguration;
import com.jecstar.etm.processor.processor.TelemetryEventProcessor;

@ManagedBean
@Singleton
public class DocumentFlushRequestor {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(DocumentFlushRequestor.class);

	@Inject
	@ProcessorConfiguration
	private TelemetryEventProcessor telemetryEventProcessor;

	
	@Schedule(minute="*", hour="*", second="0,30")
	public void flushDocuments() {
		if (log.isDebugLevelEnabled()) {
			log.logDebugMessage("Requesting flush of Solr documents");
		}
		if (this.telemetryEventProcessor != null) {
			this.telemetryEventProcessor.requestDocumentsFlush();
		}
	}
}
