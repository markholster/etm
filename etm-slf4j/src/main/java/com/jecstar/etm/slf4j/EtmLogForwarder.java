package com.jecstar.etm.slf4j;

import com.jecstar.etm.core.domain.LogTelemetryEvent;

public interface EtmLogForwarder {

	void forwardLog(LogTelemetryEvent event);
	
}
