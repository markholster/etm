package com.jecstar.etm.core.domain.converter.json;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.jecstar.etm.core.domain.Application;
import com.jecstar.etm.core.domain.EndpointHandler;
import com.jecstar.etm.core.domain.Location;
import com.jecstar.etm.core.domain.PayloadFormat;
import com.jecstar.etm.core.domain.TelemetryEvent;
import com.jecstar.etm.core.domain.converter.TelemetryEventConverter;
import com.jecstar.etm.core.domain.converter.TelemetryEventConverterTags;

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
		added = addStringElementToJsonBuffer(this.tags.getEndpointTag(), event.endpoint, sb, !added) || added;
		added = addMapElementToJsonBuffer(this.tags.getExtractedDataTag(), event.extractedData, sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getNameTag(), event.name, sb, !added) || added;
		added = addMapElementToJsonBuffer(this.tags.getMetadataTag(), event.metadata, sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getPayloadTag(), event.payload, sb, !added) || added;
		if (event.payloadFormat != null) {
			added = addStringElementToJsonBuffer(this.tags.getPayloadFormatTag(), event.payloadFormat.name(), sb, !added) || added;
		}
		added = addStringElementToJsonBuffer(this.tags.getTransactionIdTag(), event.transactionId, sb, !added) || added;
		if (event.writingEndpointHandler.isSet()) {
			if (added) {
				sb.append(", ");
			}
			sb.append("\"" + this.tags.getWritingEndpointHandlerTag() + "\": ");
			added = addEndpointHandlerToJsonBuffer(event.writingEndpointHandler, sb, true, this.tags) || added;
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
		buffer.append("\"" + elementName + "\": [");
		buffer.append(elementValues.entrySet().stream()
				.map(c -> "{\"" + this.tags.getMapKeyTag() + "\": \"" + escapeToJson(c.getKey()) + "\", " +  escapeObjectToJsonNameValuePair(this.tags.getMapValueTag(), c.getValue()) + "}")
				.sorted()
				.collect(Collectors.joining(", ")));
		buffer.append("]");
		return true;
	}

	protected boolean addEndpointHandlerToJsonBuffer(EndpointHandler endpointHandler, StringBuilder buffer, boolean firstElement, TelemetryEventConverterTags tags) {
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
		Application application = endpointHandler.application;
		if (application.isSet()) {
			if (added) {
				buffer.append(", ");
			}
			buffer.append("\"" + tags.getEndpointHandlerApplicationTag() + "\" : {");
			added = addStringElementToJsonBuffer(tags.getApplicationNameTag(), application.name, buffer, true);
			added = addInetAddressElementToJsonBuffer(tags.getApplicationHostAddressTag(), application.hostAddress, buffer, !added) || added;
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
		List<Map<String, Object>> eventMap = getArray(this.tags.getCorrelationDataTag(), valueMap);
		if (eventMap != null) {
			eventMap.forEach(c -> telemetryEvent.correlationData.put(c.get(this.tags.getMapKeyTag()).toString(), unescapeObjectFromJsonNameValuePair(this.tags.getMapValueTag(), c)));
		}
		telemetryEvent.endpoint = getString(this.tags.getEndpointTag(), valueMap);
		eventMap = getArray(this.tags.getExtractedDataTag(), valueMap);
		if (eventMap != null) {
			eventMap.forEach(c -> telemetryEvent.extractedData.put(c.get(this.tags.getMapKeyTag()).toString(), unescapeObjectFromJsonNameValuePair(this.tags.getMapValueTag(), c)));
		}
		telemetryEvent.name = getString(this.tags.getNameTag(), valueMap);
		eventMap = getArray(this.tags.getMetadataTag(), valueMap);
		if (eventMap != null) {
			eventMap.forEach(c -> telemetryEvent.metadata.put(c.get(this.tags.getMapKeyTag()).toString(), unescapeObjectFromJsonNameValuePair(this.tags.getMapValueTag(), c)));
		}
		telemetryEvent.payload = getString(this.tags.getPayloadTag(), valueMap);
		telemetryEvent.payloadFormat = PayloadFormat.safeValueOf(getString(this.tags.getPayloadFormatTag(), valueMap));
		telemetryEvent.transactionId = getString(this.tags.getTransactionIdTag(), valueMap);
		telemetryEvent.writingEndpointHandler.initialize(createEndpointFormValueMapHandler(getObject(this.tags.getWritingEndpointHandlerTag(), valueMap)));
		doConvert(telemetryEvent, valueMap);		
	}

	abstract void doConvert(Event telemetryEvent, Map<String, Object> valueMap);

	protected EndpointHandler createEndpointFormValueMapHandler(Map<String, Object> valueMap) {
		if (valueMap.isEmpty()) {
			return null;
		}
		EndpointHandler endpointHandler = new EndpointHandler();
		Map<String, Object> applicationValueMap = getObject(this.tags.getEndpointHandlerApplicationTag(), valueMap);
		if (!applicationValueMap.isEmpty()) {
			String stringHostAddress = getString(this.tags.getApplicationHostAddressTag(), applicationValueMap);
			if (stringHostAddress != null) {
				try {
					endpointHandler.application.hostAddress = InetAddress.getByName(stringHostAddress);
				} catch (UnknownHostException e) {
				}
			}
			endpointHandler.application.instance = getString(this.tags.getApplicationInstanceTag(), applicationValueMap);
			endpointHandler.application.name = getString(this.tags.getApplicationNameTag(), applicationValueMap);
			endpointHandler.application.principal = getString(this.tags.getApplicationPrincipalTag(), applicationValueMap);
			endpointHandler.application.version = getString(this.tags.getApplicationVersionTag(), applicationValueMap);
		}
		endpointHandler.handlingTime = getZonedDateTime(this.tags.getEndpointHandlerHandlingTimeTag(), valueMap);
		Map<String, Object> locationValueMap = getObject(this.tags.getEndpointHandlerLocationTag(), valueMap);
		if (locationValueMap != null) {
			endpointHandler.location.latitude = getDouble(this.tags.getLatitudeTag(), locationValueMap);
			endpointHandler.location.longitude = getDouble(this.tags.getLongitudeTag(), locationValueMap);
		}
		return endpointHandler;
	}
}
