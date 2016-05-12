package com.jecstar.etm.core.enhancers;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.jecstar.etm.domain.Endpoint;
import com.jecstar.etm.domain.PayloadFormat;
import com.jecstar.etm.domain.TelemetryEvent;

public abstract class AbstractDefaultTelemetryEventEnhancer {

	
	protected void doEnhance(final TelemetryEvent<?> event, final ZonedDateTime enhanceTime) {
		setId(event);
		setDetectedPayloadFormat(event);
		setWritingHandlerTimes(event, enhanceTime);
	}
	
	private void setId(final TelemetryEvent<?> event) {
		if (event.id == null) {
			event.id = UUID.randomUUID().toString();
		}
	}
	
	private void setDetectedPayloadFormat(final TelemetryEvent<?> event) {
		if (event.payloadFormat == null) {
			event.payloadFormat = detectPayloadFormat(event.payload);
		}
	}
	
	private void setWritingHandlerTimes(final TelemetryEvent<?> event, final ZonedDateTime enhanceTime) {
		if (event.endpoints.size() == 0) {
			Endpoint endpoint = new Endpoint();
			endpoint.writingEndpointHandler.handlingTime = enhanceTime;
			event.endpoints.add(endpoint);
		} else {
			for (Endpoint endpoint : event.endpoints) {
				if (endpoint.writingEndpointHandler.handlingTime == null) {
					ZonedDateTime earliestReadTime = endpoint.getEarliestReadTime();
					if (earliestReadTime != null && earliestReadTime.isBefore(enhanceTime)) {
						endpoint.writingEndpointHandler.handlingTime = earliestReadTime;
					} else {
						endpoint.writingEndpointHandler.handlingTime = enhanceTime;
					}
				}				
			}
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
			return PayloadFormat.TEXT;
		}
		String trimmed = payload.toLowerCase().trim();
		if (trimmed.indexOf("<soap:envelope ") != -1) {
			return PayloadFormat.SOAP;
		} else if (trimmed.indexOf("<!doctype html") != -1) {
			return PayloadFormat.HTML;
		} else if (trimmed.startsWith("<?xml ")) {
			return PayloadFormat.XML;
		} else if (trimmed.startsWith("{") && payload.endsWith("}")) {
			return PayloadFormat.JSON;
		} else if (trimmed.startsWith("select")
				   || trimmed.startsWith("insert")
				   || trimmed.startsWith("update")
				   || trimmed.startsWith("delete")
				   || trimmed.startsWith("drop")
				   || trimmed.startsWith("create")) {
			return PayloadFormat.SQL;
		}
		return PayloadFormat.TEXT;
	}

}
