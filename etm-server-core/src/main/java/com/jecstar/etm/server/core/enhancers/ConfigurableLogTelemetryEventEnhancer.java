package com.jecstar.etm.server.core.enhancers;

import java.time.ZonedDateTime;

import com.jecstar.etm.domain.LogTelemetryEvent;

public class ConfigurableLogTelemetryEventEnhancer extends AbstractConfigurableTelemetryEventEnhancer implements LogTelemetryEventEnhancer {

	@Override
	public String getName() {
		return "Configurable Log Enhancer";
	}
	
	@Override
	public void enhance(final LogTelemetryEvent event, final ZonedDateTime enhanceTime) {
		doEnhance(event, enhanceTime);
	}
	
}
