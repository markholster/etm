package com.jecstar.etm.server.core.enhancers;

import java.time.ZonedDateTime;

import com.jecstar.etm.domain.MessagingTelemetryEvent;

public class ConfigurableMessagingTelemetryEventEnhancer extends AbstractConfigurableTelemetryEventEnhancer implements MessagingTelemetryEventEnhancer {

	@Override
	public String getName() {
		return "Configurable Messaging Enhancer";
	}
	
	@Override
	public void enhance(final MessagingTelemetryEvent event, final ZonedDateTime enhanceTime) {
		doEnhance(event, enhanceTime);
	}
	
}
