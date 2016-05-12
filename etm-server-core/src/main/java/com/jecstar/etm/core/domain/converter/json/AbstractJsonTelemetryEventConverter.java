package com.jecstar.etm.core.domain.converter.json;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jecstar.etm.core.domain.converter.TelemetryEventConverter;
import com.jecstar.etm.core.domain.converter.TelemetryEventConverterTags;
import com.jecstar.etm.domain.Application;
import com.jecstar.etm.domain.Endpoint;
import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.Location;
import com.jecstar.etm.domain.PayloadFormat;
import com.jecstar.etm.domain.TelemetryEvent;

/**
 * Converter class that converts a <code>TelemetryEvent</code> to a JSON string.
 * To prevent lots of garbage collections the internal buffer is reused, and
 * hence this class is not thread safe!
 * 
 * @author mark
 */
public abstract class AbstractJsonTelemetryEventConverter<Event extends TelemetryEvent<Event>> extends AbstractJsonConverter implements TelemetryEventConverter<String, Event> {
	
	private final TelemetryEventConverterTags tags = new TelemetryEventConverterTagsJsonImpl();

	@Override
	public String convert(Event event) {
		final StringBuilder sb = new StringBuilder();
		boolean added = false;
		sb.append("{");
		added = addStringElementToJsonBuffer(this.tags.getIdTag(), event.id, sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getCorrelationIdTag(), event.correlationId, sb, !added) || added;
		added = addMapElementToJsonBuffer(this.tags.getCorrelationDataTag(), event.correlationData, sb, !added) || added;
		if (event.endpoints.size() != 0) {
			if (added) {
				sb.append(", ");
			}
			sb.append("\"" + getTags().getEndpointsTag() + "\": [");
			boolean endpointAdded = false;
			for (int i = 0; i < event.endpoints.size(); i++) {
				endpointAdded = addEndpointToJsonBuffer(event.endpoints.get(i), sb, i == 0 ? true : !endpointAdded, getTags()) || endpointAdded;
			}
			sb.append("]");			
			added = endpointAdded || added;
		}
		added = addMapElementToJsonBuffer(this.tags.getExtractedDataTag(), event.extractedData, sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getNameTag(), event.name, sb, !added) || added;
		added = addMapElementToJsonBuffer(this.tags.getMetadataTag(), event.metadata, sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getPayloadTag(), event.payload, sb, !added) || added;
		if (event.payloadFormat != null) {
			added = addStringElementToJsonBuffer(this.tags.getPayloadFormatTag(), event.payloadFormat.name(), sb, !added) || added;
		}
		added = doConvert(event, sb, !added) || added;
		sb.append("}");
		return sb.toString();
	}
	
	abstract boolean doConvert(Event event, StringBuilder buffer, boolean firstElement);
	
	protected boolean addMapElementToJsonBuffer(String elementName, Map<String, Object> elementValues, StringBuilder buffer, boolean firstElement) {
		if (elementValues.size() < 1) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("\"" + elementName + "\": {");
		firstElement = true;
		for (Entry<String, Object> entry : elementValues.entrySet()) {
			if (!firstElement) {
				buffer.append(", ");
			}
			buffer.append(escapeObjectToJsonNameValuePair(entry.getKey(), entry.getValue()));
			firstElement = false;
		}
		buffer.append("}");
		return true;
	}
	
	private boolean addEndpointToJsonBuffer(Endpoint endpoint, StringBuilder buffer, boolean firstElement, TelemetryEventConverterTags tags) {
		boolean readingEndpointHandlerSet = false;
		for (EndpointHandler handler : endpoint.readingEndpointHandlers) {
			if (handler.isSet()) {
				readingEndpointHandlerSet = true;
				break;
			}
		}
		if (endpoint.name == null && !endpoint.writingEndpointHandler.isSet() && !readingEndpointHandlerSet) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("{");
		boolean added = false;
		added = addStringElementToJsonBuffer(tags.getEndpointNameTag(), endpoint.name, buffer, true);
		if (readingEndpointHandlerSet) {
			if (added) {
				buffer.append(", ");
			}
			buffer.append("\"" + getTags().getReadingEndpointHandlersTag() + "\": [");
			added = false;
			for (int i = 0; i < endpoint.readingEndpointHandlers.size(); i++) {
				added = addEndpointHandlerToJsonBuffer(endpoint.readingEndpointHandlers.get(i), buffer, i == 0 ? true : !added, getTags()) || added;
			}
			buffer.append("]");
			added = true;
		}
		if (endpoint.writingEndpointHandler.isSet()) {
			if (added) {
				buffer.append(", ");
			}
			buffer.append("\"" + this.tags.getWritingEndpointHandlerTag() + "\": ");
			added = addEndpointHandlerToJsonBuffer(endpoint.writingEndpointHandler, buffer, true, this.tags) || added;
		}
		buffer.append("}");
		return added;
	}

	private boolean addEndpointHandlerToJsonBuffer(EndpointHandler endpointHandler, StringBuilder buffer, boolean firstElement, TelemetryEventConverterTags tags) {
		if (!endpointHandler.isSet()) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("{");
		boolean added = false;
		if (endpointHandler.handlingTime != null) {
			added = addLongElementToJsonBuffer(tags.getEndpointHandlerHandlingTimeTag(), endpointHandler.handlingTime.toInstant().toEpochMilli(), buffer, true);
		}
		added = addStringElementToJsonBuffer(this.tags.getEndpointHandlerTransactionIdTag(), endpointHandler.transactionId, buffer, !added) || added;
		Application application = endpointHandler.application;
		if (application.isSet()) {
			if (added) {
				buffer.append(", ");
			}
			buffer.append("\"" + tags.getEndpointHandlerApplicationTag() + "\" : {");
			added = addStringElementToJsonBuffer(tags.getApplicationNameTag(), application.name, buffer, true);
			added = addInetAddressElementToJsonBuffer(tags.getApplicationHostAddressTag(), tags.getApplicationHostNameTag(), application.hostAddress, buffer, !added) || added;
			added = addStringElementToJsonBuffer(tags.getApplicationInstanceTag(), application.instance, buffer, !added) || added;
			added = addStringElementToJsonBuffer(tags.getApplicationVersionTag(), application.version, buffer, !added) || added;
			added = addStringElementToJsonBuffer(tags.getApplicationPrincipalTag(), application.principal, buffer, !added) || added;
			buffer.append("}");
		}
		Location location = endpointHandler.location;
		if (location.isSet()) {
			if (added) {
				buffer.append(", ");
			}
			buffer.append("\"" + tags.getEndpointHandlerLocationTag() + "\" : {");
			added = addDoubleElementToJsonBuffer(tags.getLatitudeTag(), location.latitude, buffer, true);
			added = addDoubleElementToJsonBuffer(tags.getLongitudeTag(), location.longitude, buffer, !added) || added;
			buffer.append("}");			
		}
		buffer.append("}");
		return true;
	}

	@Override
	public TelemetryEventConverterTags getTags() {
		return this.tags;
	}

	@Override
	public void convert(String jsonContent, Event telemetryEvent) {
		Map<String, Object> valueMap = toMap(jsonContent);
		convert(valueMap, telemetryEvent);
	}
	
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
				List<Map<String, Object>> endpointHandlers = getArray(getTags().getReadingEndpointHandlersTag(), endpointMap);
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
		doConvert(telemetryEvent, valueMap);		
	}

	abstract void doConvert(Event telemetryEvent, Map<String, Object> valueMap);

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
