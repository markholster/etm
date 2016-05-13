package com.jecstar.etm.server.core.enhancers;

import java.time.ZonedDateTime;

import com.jecstar.etm.domain.BusinessTelemetryEvent;

public class DefaultBusinessTelemetryEventEnhancer extends AbstractDefaultTelemetryEventEnhancer implements BusinessTelemetryEventEnhancer {

	@Override
	public String getName() {
		return "Default Business Enhancer";
	}
	
	@Override
	public void enhance(final BusinessTelemetryEvent event, final ZonedDateTime enhanceTime) {
		doEnhance(event, enhanceTime);
	}
	
}
