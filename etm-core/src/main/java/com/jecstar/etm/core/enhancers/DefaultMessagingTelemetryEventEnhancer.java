package com.jecstar.etm.core.enhancers;

import java.time.ZonedDateTime;

import com.jecstar.etm.core.domain.MessagingTelemetryEvent;

public class DefaultMessagingTelemetryEventEnhancer extends AbstractDefaultTelemetryEventEnhancer implements MessagingTelemetryEventEnhancer {

	@Override
	public String getName() {
		return "Default Messaging Enhancer";
	}
	
	@Override
	public void enhance(final MessagingTelemetryEvent event, final ZonedDateTime enhanceTime) {
		doEnhance(event, enhanceTime);
	}
}
