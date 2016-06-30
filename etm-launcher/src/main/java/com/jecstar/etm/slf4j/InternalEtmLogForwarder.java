package com.jecstar.etm.slf4j;

import com.jecstar.etm.domain.LogTelemetryEvent;
import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;

public class InternalEtmLogForwarder implements EtmLogForwarder {

	public static TelemetryCommandProcessor processor;

	@Override
	public void forwardLog(LogTelemetryEvent event) {
		if (processor != null && processor.isReadyForProcessing()) {
			processor.processTelemetryEvent(event);
		} else {
			// TODO log op console dumpen als processor leeg is?
//			System.out.println(event.payload);
		}
		
	}

}
