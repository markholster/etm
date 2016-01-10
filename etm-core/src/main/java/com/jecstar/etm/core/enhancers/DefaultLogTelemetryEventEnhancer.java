package com.jecstar.etm.core.enhancers;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.jecstar.etm.core.domain.LogTelemetryEvent;
import com.jecstar.etm.core.domain.PayloadFormat;

public class DefaultLogTelemetryEventEnhancer implements LogTelemetryEventEnhancer {

	@Override
	public void enhance(final LogTelemetryEvent event, final ZonedDateTime enhanceTime) {
		if (event.id == null) {
			event.id = UUID.randomUUID().toString();
		}
		if (event.payloadFormat == null) {
			event.payloadFormat = PayloadFormat.TEXT;
		}
		if (event.writingEndpointHandler.handlingTime == null) {
			event.writingEndpointHandler.handlingTime = enhanceTime;
		}
	}

}
