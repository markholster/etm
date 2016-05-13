package com.jecstar.etm.server.core.enhancers;

import java.time.ZonedDateTime;

import com.jecstar.etm.domain.HttpTelemetryEvent;

public class DefaultHttpTelemetryEventEnhancer extends AbstractDefaultTelemetryEventEnhancer implements HttpTelemetryEventEnhancer {
	
	@Override
	public String getName() {
		return "Default HTTP Enhancer";
	}

	@Override
	public void enhance(final HttpTelemetryEvent event, final ZonedDateTime enhanceTime) {
		doEnhance(event, enhanceTime);
	}

}
