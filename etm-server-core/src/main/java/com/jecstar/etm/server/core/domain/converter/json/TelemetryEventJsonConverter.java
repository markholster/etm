package com.jecstar.etm.server.core.domain.converter.json;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import com.jecstar.etm.domain.Endpoint;
import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.PayloadFormat;
import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.domain.writers.TelemetryEventTags;
import com.jecstar.etm.domain.writers.json.TelemetryEventTagsJsonImpl;

public class TelemetryEventJsonConverter<Event extends TelemetryEvent<Event>> extends JsonConverter {

	private final TelemetryEventTags tags = new TelemetryEventTagsJsonImpl();
	
	public void convert(Map<String, Object> valueMap, Event telemetryEvent) {
		telemetryEvent.initialize();
		telemetryEvent.id = getString(this.tags.getIdTag(), valueMap);
		telemetryEvent.correlationId = getString(this.tags.getCorrelationIdTag(), valueMap);
		Map<String, Object> eventMap = getObject(this.tags.getCorrelationDataTag(), valueMap);
		if (eventMap != null && !eventMap.isEmpty()) {
			telemetryEvent.correlationData.putAll(eventMap);
		}
		List<Map<String, Object>> endpoints = getArray(this.tags.getEndpointsTag(), valueMap);
		if (endpoints != null) {
			for (Map<String, Object> endpointMap : endpoints) {
				Endpoint endpoint = new Endpoint();
				endpoint.name = getString(this.tags.getEndpointNameTag(), endpointMap);
				List<Map<String, Object>> endpointHandlers = getArray(this.tags.getReadingEndpointHandlersTag(), endpointMap);
				if (endpointHandlers != null) {
					endpointHandlers.forEach(c -> endpoint.readingEndpointHandlers.add(createEndpointFormValueMapHandler(c)));
				}
				endpoint.writingEndpointHandler.initialize(createEndpointFormValueMapHandler(getObject(this.tags.getWritingEndpointHandlerTag(), endpointMap)));
				telemetryEvent.endpoints.add(endpoint);
			}
		}
		eventMap = getObject(this.tags.getExtractedDataTag(), valueMap);
		if (eventMap != null && !eventMap.isEmpty()) {
			telemetryEvent.extractedData.putAll(eventMap);
		}
		telemetryEvent.name = getString(this.tags.getNameTag(), valueMap);
		eventMap = getObject(this.tags.getMetadataTag(), valueMap);
		if (eventMap != null && !eventMap.isEmpty()) {
			telemetryEvent.metadata.putAll(eventMap);
		}
		telemetryEvent.payload = getString(this.tags.getPayloadTag(), valueMap);
		telemetryEvent.payloadFormat = PayloadFormat.safeValueOf(getString(this.tags.getPayloadFormatTag(), valueMap));
	}
	
	private EndpointHandler createEndpointFormValueMapHandler(Map<String, Object> valueMap) {
		if (valueMap.isEmpty()) {
			return null;
		}
		EndpointHandler endpointHandler = new EndpointHandler();
		Map<String, Object> applicationValueMap = getObject(this.tags.getEndpointHandlerApplicationTag(), valueMap);
		if (!applicationValueMap.isEmpty()) {
			String stringHostAddress = getString(this.tags.getApplicationHostAddressTag(), applicationValueMap);
			String hostName = getString(this.tags.getApplicationHostNameTag(), applicationValueMap);
			if (stringHostAddress != null) {
				byte[] address = null;
				try {
					address = InetAddress.getByName(stringHostAddress).getAddress();
				} catch (UnknownHostException e) {
				}
				if (address != null) {
					if (hostName != null) {
						try {
							endpointHandler.application.hostAddress = InetAddress.getByAddress(hostName, address);
						} catch (UnknownHostException e) {
						}
					} else {
						try {
							endpointHandler.application.hostAddress = InetAddress.getByAddress(address);
						} catch (UnknownHostException e) {
						}
					}
				}
			} else if (hostName != null) {
				try {
					endpointHandler.application.hostAddress = InetAddress.getByName(hostName);
				} catch (UnknownHostException e) {
				}
			}
			endpointHandler.application.instance = getString(this.tags.getApplicationInstanceTag(), applicationValueMap);
			endpointHandler.application.name = getString(this.tags.getApplicationNameTag(), applicationValueMap);
			endpointHandler.application.principal = getString(this.tags.getApplicationPrincipalTag(), applicationValueMap);
			endpointHandler.application.version = getString(this.tags.getApplicationVersionTag(), applicationValueMap);
		}
		endpointHandler.handlingTime = getZonedDateTime(this.tags.getEndpointHandlerHandlingTimeTag(), valueMap);
		endpointHandler.transactionId = getString(this.tags.getEndpointHandlerTransactionIdTag(), valueMap);
		Map<String, Object> locationValueMap = getObject(this.tags.getEndpointHandlerLocationTag(), valueMap);
		if (locationValueMap != null) {
			endpointHandler.location.latitude = getDouble(this.tags.getLatitudeTag(), locationValueMap);
			endpointHandler.location.longitude = getDouble(this.tags.getLongitudeTag(), locationValueMap);
		}
		return endpointHandler;
	}

}
