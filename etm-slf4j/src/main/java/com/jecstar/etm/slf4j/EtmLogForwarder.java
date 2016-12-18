package com.jecstar.etm.slf4j;

import com.jecstar.etm.domain.LogTelemetryEvent;

public interface EtmLogForwarder {

	void forwardLog(LogTelemetryEvent event);
	
}
