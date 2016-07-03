package com.jecstar.etm.server.core.enhancers;

import java.time.ZonedDateTime;

import com.jecstar.etm.domain.HttpTelemetryEvent;

public class ConfigurableHttpTelemetryEventEnhancer extends AbstractConfigurableTelemetryEventEnhancer implements HttpTelemetryEventEnhancer {

	@Override
	public String getName() {
		return "Configurable Http Enhancer";
	}
	
	@Override
	public void enhance(final HttpTelemetryEvent event, final ZonedDateTime enhanceTime) {
		doEnhance(event, enhanceTime);
	}
	
}
