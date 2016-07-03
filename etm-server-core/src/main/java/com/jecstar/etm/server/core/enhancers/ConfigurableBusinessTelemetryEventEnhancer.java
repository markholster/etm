package com.jecstar.etm.server.core.enhancers;

import java.time.ZonedDateTime;

import com.jecstar.etm.domain.BusinessTelemetryEvent;

public class ConfigurableBusinessTelemetryEventEnhancer extends AbstractConfigurableTelemetryEventEnhancer implements BusinessTelemetryEventEnhancer {

	@Override
	public String getName() {
		return "Configurable Business Enhancer";
	}
	
	@Override
	public void enhance(final BusinessTelemetryEvent event, final ZonedDateTime enhanceTime) {
		doEnhance(event, enhanceTime);
	}
	
}
