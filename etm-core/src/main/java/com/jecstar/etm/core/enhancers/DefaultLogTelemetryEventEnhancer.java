package com.jecstar.etm.core.enhancers;

import java.time.ZonedDateTime;

import com.jecstar.etm.core.domain.LogTelemetryEvent;

public class DefaultLogTelemetryEventEnhancer extends AbstractDefaultTelemetryEventEnhancer implements LogTelemetryEventEnhancer {

	@Override
	public String getName() {
		return "Default Log Enhancer";
	}
	
	@Override
	public void enhance(final LogTelemetryEvent event, final ZonedDateTime enhanceTime) {
		doEnhance(event, enhanceTime);
	}

}
