package com.jecstar.etm.core.enhancers;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.jecstar.etm.core.domain.HttpTelemetryEvent;
import com.jecstar.etm.core.domain.PayloadFormat;

public class DefaultHttpTelemetryEventEnhancer implements HttpTelemetryEventEnhancer {

	@Override
	public void enhance(final HttpTelemetryEvent event, final ZonedDateTime enhanceTime) {
		if (event.id == null) {
			event.id = UUID.randomUUID().toString();
		}
		if (event.payloadFormat == null) {
			event.payloadFormat = detectPayloadFormat(event.payload);
		}
		if (event.writingEndpointHandler.handlingTime == null) {
			event.writingEndpointHandler.handlingTime = enhanceTime;
		}
	}
	
	/**
	 * Super simple payload format detector. This isn't an enhanced detection
	 * algorithm because we won't be losing to much performance here. The end
	 * user should be able to tell the system which format it is anyway.
	 * 
	 * @param payload The payload.
	 * @return The detected <code>PayloadFormat</code>.
	 */
	private PayloadFormat detectPayloadFormat(String payload) {
		if (payload == null) {
			return PayloadFormat.HTML;
		}
		String trimmed = payload.toLowerCase().trim();
		if (trimmed.indexOf("<soap:envelope ") != -1) {
			return PayloadFormat.SOAP;
		} else if (trimmed.indexOf("<!doctype html") != -1) {
			return PayloadFormat.HTML;
		} else if (trimmed.startsWith("{") && payload.endsWith("}")) {
			return PayloadFormat.JSON;
		} 
		return PayloadFormat.HTML;
	}

}
