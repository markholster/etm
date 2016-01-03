package com.jecstar.etm.processor.ibmmq.startup;

import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;

public class FlushRequestor implements Runnable {

	private static final LogWrapper log = LogFactory.getLogger(FlushRequestor.class);
	
	private AutoManagedTelemetryEventProcessor processor;

	public FlushRequestor(AutoManagedTelemetryEventProcessor processor) {
		this.processor = processor;
	}
	
	@Override
	public void run() {
		if (log.isDebugLevelEnabled()) {
			log.logDebugMessage("Requesting document flush.");
		}
		try {
			this.processor.requestDocumentsFlush();
		} catch (Throwable t) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Request for document flush failed.", t);
			}
		}
	}

}
