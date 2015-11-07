package com.jecstar.etm.processor.converter.json;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.stream.Collectors;

import com.jecstar.etm.processor.TelemetryEvent;
import com.jecstar.etm.processor.converter.TelemetryEventConverter;
import com.jecstar.etm.processor.converter.TelemetryEventConverterTags;

/**
 * Converter class that converts a <code>TelemetryEvent</code> to a JSON string.
 * To prevent lots of garbage collections the internal buffer is reused, and
 * hence this class is not thread safe!
 * 
 * @author mark
 */
public class TelemetryEventConverterJsonImpl extends AbstractJsonConverter implements TelemetryEventConverter<String> {
	
	private final StringBuilder sb = new StringBuilder();
	private final TelemetryEventConverterTags tags = new TelemetryEventConverterTagsJsonImpl();

	@Override
	public String convert(TelemetryEvent event) {
		this.sb.setLength(0);
		this.sb.append("{");
		boolean added = false;
//		added = addStringElementToJsonBuffer(this.tags.getCorrelationIdTag(), event.correlationId, this.sb, !added) || added;
//		added = addMapElementToJsonBuffer(this.tags.getCorrelationDataTag(), event.correlationData, this.sb, !added) || added;
//		added = addStringElementToJsonBuffer(this.tags.getEndpointTag(), event.endpoint, this.sb, !added) || added;
//		if (event.expiry != null) {
//			added = addLongElementToJsonBuffer(this.tags.getExpiryTag(), event.expiry.toInstant().toEpochMilli(), this.sb, !added) || added;
//		}
//		added = addMapElementToJsonBuffer(this.tags.getExtractedDataTag(), event.extractedData, this.sb, !added) || added;
//		added = addStringElementToJsonBuffer(this.tags.getNameTag(), event.name, this.sb, !added) || added;
//		added = addMapElementToJsonBuffer(this.tags.getMetadataTag(), event.metadata, this.sb, !added) || added;
//		added = addStringElementToJsonBuffer(this.tags.getPackagingTag(), event.packaging, this.sb, !added) || added;
//		added = addStringElementToJsonBuffer(this.tags.getPayloadTag(), event.payload, this.sb, !added) || added;
//		if (event.payloadFormat != null) {
//			added = addStringElementToJsonBuffer(this.tags.getPayloadFormatTag(), event.payloadFormat.name(), this.sb, !added) || added;
//		}
//		if (event.isRequest() && event.writingEndpointHandler.isSet() && event.expiry != null) {
//			// Set the response time to the expiry initially.
//			added = addLongElementToJsonBuffer(this.tags.getResponseTimeTag(), event.expiry.toInstant().toEpochMilli() - event.writingEndpointHandler.handlingTime.toInstant().toEpochMilli(), this.sb, !added) || added;
//		}
//		added = addStringElementToJsonBuffer(this.tags.getTransactionIdTag(), event.transactionId, this.sb, !added) || added;
//		if (event.transport != null) {
//			added = addStringElementToJsonBuffer(this.tags.getTransportTag(), event.transport.name(), this.sb, !added) || added;
//		}
//		if (!event.readingEndpointHandlers.isEmpty()) {
//			if (added) {
//				this.sb.append(", ");
//			}
//			this.sb.append("\"" + this.tags.getReadingEndpointHandlersTag() + "\": [");
//			added = false;
//			for (int i = 0; i < event.readingEndpointHandlers.size(); i++) {
//				added = addEndpointHandlerToJsonBuffer(event.readingEndpointHandlers.get(i), this.sb, i == 0 ? true : !added, this.tags) || added;
//			}
//			this.sb.append("]");
//		}
//		if (event.writingEndpointHandler.isSet()) {
//			if (added) {
//				this.sb.append(", ");
//			}
//			this.sb.append("\"" + this.tags.getWritingEndpointHandlerTag() + "\": ");
//			addEndpointHandlerToJsonBuffer(event.writingEndpointHandler, this.sb, true, this.tags);
//		}
		this.sb.append("}");
		return this.sb.toString();
	}
	
	private boolean addMapElementToJsonBuffer(String elementName, Map<String, String> elementValues, StringBuilder buffer, boolean firstElement) {
		if (elementValues.size() < 1) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("\"" + elementName + "\": [");
		buffer.append(elementValues.entrySet().stream()
				.map(c -> "{ \"" + escapeToJson(c.getKey()) + "\": \"" + escapeToJson(c.getValue()) + "\" }")
				.sorted()
				.collect(Collectors.joining(", ")));
		buffer.append("]");
		return true;
	}

//	private boolean addEndpointHandlerToJsonBuffer(EndpointHandler endpointHandler, StringBuilder buffer, boolean firstElement, TelemetryEventConverterTags tags) {
//		if (!endpointHandler.isSet()) {
//			return false;
//		}
//		if (!firstElement) {
//			buffer.append(", ");
//		}
//		buffer.append("{");
//		boolean added = false;
//		if (endpointHandler.handlingTime != null) {
//			added = addLongElementToJsonBuffer(tags.getEndpointHandlerHandlingTimeTag(), endpointHandler.handlingTime.toInstant().toEpochMilli(), buffer, true);
//		}
//		Application application = endpointHandler.application;
//		if (application.isSet()) {
//			if (added) {
//				buffer.append(", ");
//			}
//			buffer.append("\"" + tags.getEndpointHandlerApplicationTag() + "\" : {");
//			added = addStringElementToJsonBuffer(tags.getApplicationNameTag(), application.name, buffer, true);
//			added = addInetAddressElementToJsonBuffer(tags.getApplicationHostAddressTag(), application.hostAddress, buffer, !added) || added;
//			added = addStringElementToJsonBuffer(tags.getApplicationInstanceTag(), application.instance, buffer, !added) || added;
//			added = addStringElementToJsonBuffer(tags.getApplicationVersionTag(), application.version, buffer, !added) || added;
//			added = addStringElementToJsonBuffer(tags.getApplicationPrincipalTag(), application.principal, buffer, !added) || added;
//			buffer.append("}");
//		}
//		Location location = endpointHandler.location;
//		if (location.isSet()) {
//			if (added) {
//				buffer.append(", ");
//			}
//			buffer.append("\"" + tags.getEndpointHandlerLocationTag() + "\" : {");
//			added = addDoubleElementToJsonBuffer(tags.getLatitudeTag(), location.latitude, buffer, true);
//			added = addDoubleElementToJsonBuffer(tags.getLongitudeTag(), location.longitude, buffer, !added) || added;
//			buffer.append("}");			
//		}
//		buffer.append("}");
//		return true;
//	}

	@Override
	public TelemetryEventConverterTags getTags() {
		return this.tags;
	}

	@Override
	public void convert(String jsonContent, TelemetryEvent telemetryEvent) {
		Map<String, Object> valueMap = toMap(jsonContent);
		telemetryEvent.initialize();
//		telemetryEvent.id = getString(this.tags.getIdTag(), valueMap);
//		telemetryEvent.correlationId = getString(this.tags.getCorrelationIdTag(), valueMap);
//		telemetryEvent.endpoint = getString(this.tags.getEndpointTag(), valueMap);
//		telemetryEvent.expiry = getZonedDateTime(this.tags.getExpiryTag(), valueMap);
//		telemetryEvent.name = getString(this.tags.getNameTag(), valueMap);
//		getArray(this.tags.getMetadataTag(), valueMap).forEach(c -> c.forEach((k, v) -> telemetryEvent.metadata.put(k, v.toString())));
//		telemetryEvent.packaging = getString(this.tags.getPackagingTag(), valueMap);
//		telemetryEvent.payload = getString(this.tags.getPayloadTag(), valueMap);
//		telemetryEvent.payloadFormat = PayloadFormat.saveValueOf(getString(this.tags.getPayloadFormatTag(), valueMap));
//		getArray(this.tags.getReadingEndpointHandlersTag(), valueMap).forEach(c -> telemetryEvent.readingEndpointHandlers.add(createEndpointFormValueMapHandler(c)));
//		telemetryEvent.transactionId = getString(this.tags.getTransactionIdTag(), valueMap);
//		telemetryEvent.transport = Transport.saveValueOf(getString(this.tags.getTransportTag(), valueMap));
//		telemetryEvent.writingEndpointHandler.initialize(createEndpointFormValueMapHandler(getObject(this.tags.getWritingEndpointHandlerTag(), valueMap)));
	}

//	private EndpointHandler createEndpointFormValueMapHandler(Map<String, Object> valueMap) {
//		if (valueMap.isEmpty()) {
//			return null;
//		}
//		EndpointHandler endpointHandler = new EndpointHandler();
//		Map<String, Object> applicationValueMap = getObject(this.tags.getEndpointHandlerApplicationTag(), valueMap);
//		if (!applicationValueMap.isEmpty()) {
//			String stringHostAddress = getString(this.tags.getApplicationHostAddressTag(), applicationValueMap);
//			if (stringHostAddress != null) {
//				try {
//					endpointHandler.application.hostAddress = InetAddress.getByName(stringHostAddress);
//				} catch (UnknownHostException e) {
//				}
//			}
//			endpointHandler.application.instance = getString(this.tags.getApplicationInstanceTag(), applicationValueMap);
//			endpointHandler.application.name = getString(this.tags.getApplicationNameTag(), applicationValueMap);
//			endpointHandler.application.principal = getString(this.tags.getApplicationPrincipalTag(), applicationValueMap);
//			endpointHandler.application.version = getString(this.tags.getApplicationVersionTag(), applicationValueMap);
//		}
//		endpointHandler.handlingTime = getZonedDateTime(this.tags.getEndpointHandlerHandlingTimeTag(), valueMap);
//		Map<String, Object> locationValueMap = getObject(this.tags.getEndpointHandlerLocationTag(), valueMap);
//		if (locationValueMap != null) {
//			endpointHandler.location.latitude = getDouble(this.tags.getLatitudeTag(), locationValueMap);
//			endpointHandler.location.longitude = getDouble(this.tags.getLongitudeTag(), locationValueMap);
//		}
//		return endpointHandler;
//	}
}
