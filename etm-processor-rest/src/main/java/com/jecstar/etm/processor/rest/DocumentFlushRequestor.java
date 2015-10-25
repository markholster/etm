package com.jecstar.etm.processor.rest;

import javax.annotation.ManagedBean;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.inject.Inject;

import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;

@ManagedBean
@Singleton
public class DocumentFlushRequestor {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(DocumentFlushRequestor.class);

	@Inject
	private TelemetryCommandProcessor telemetryCommandProcessor;

	
	@Schedule(minute="*", hour="*", second="0,30", persistent=false)
	public void flushDocuments() {
		if (log.isDebugLevelEnabled()) {
			log.logDebugMessage("Requesting flush of documents");
		}
//		if (this.telemetryCommandProcessor != null) {
//			this.telemetryCommandProcessor.requestDocumentsFlush();
//		}
	}
}
