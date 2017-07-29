package com.jecstar.etm.slf4j;

import com.jecstar.etm.domain.LogTelemetryEvent;

interface EtmLogForwarder {

	void forwardLog(LogTelemetryEvent event);
	
}
