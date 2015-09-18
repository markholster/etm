package com.jecstar.etm.core.domain.converter.json;

import java.util.Map;
import java.util.stream.Collectors;

import com.jecstar.etm.core.domain.Application;
import com.jecstar.etm.core.domain.EndpointHandler;
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
public class TelemetryEventConverterJsonImpl implements TelemetryEventConverter<String> {
	
	private StringBuilder sb = new StringBuilder();

	@Override
	public String convert(TelemetryEvent event, TelemetryEventConverterTags tags) {
		this.sb.setLength(0);
		this.sb.append("{");
		addStringElementToJsonBuffer(tags.getIdTag(), event.id, this.sb, true);
		addStringElementToJsonBuffer(tags.getCorrelationIdTag(), event.correlationId, this.sb, false);
		addMapElementToJsonBuffer(tags.getCorrelationDataTag(), event.correlationData, this.sb, false);
		addStringElementToJsonBuffer(tags.getEndpointTag(), event.endpoint, this.sb, false);
		if (event.expiry != null) {
			addLongElementToJsonBuffer(tags.getExpiryTag(), event.expiry.toInstant().toEpochMilli(), this.sb, false);
		}
		addMapElementToJsonBuffer(tags.getExtractedDataTag(), event.extractedData, this.sb, false);
		addStringElementToJsonBuffer(tags.getNameTag(), event.name, this.sb, false);
		addMapElementToJsonBuffer(tags.getMetadataTag(), event.metadata, this.sb, false);
		addStringElementToJsonBuffer(tags.getPackagingTag(), event.packaging, this.sb, false);
		addStringElementToJsonBuffer(tags.getPayloadTag(), event.payload, this.sb, false);
		if (event.payloadFormat != null) {
			addStringElementToJsonBuffer(tags.getPayloadFormatTag(), event.payloadFormat.name(), this.sb, false);
		}
		if (event.isRequest() && event.writingEndpointHandler.isSet() && event.expiry != null) {
			// Set the response time to the expiry initially.
			addLongElementToJsonBuffer(tags.getResponseTimeTag(), event.expiry.toInstant().toEpochMilli() - event.writingEndpointHandler.handlingTime.toInstant().toEpochMilli(), this.sb, false);
		}
		if (event.transport != null) {
			addStringElementToJsonBuffer(tags.getTransportTag(), event.transport.name(), this.sb, false);
		}
		if (!event.readingEndpointHandlers.isEmpty()) {
			this.sb.append(", \"" + tags.getReadingEndpointHandlersTag() + "\": [");
			boolean added = false;
			for (int i = 0; i < event.readingEndpointHandlers.size(); i++) {
				added = addEndpointHandlerToJsonBuffer(event.readingEndpointHandlers.get(i), this.sb, i == 0 ? true : !added, tags) || added;
			}
			this.sb.append("]");
		}
		if (event.writingEndpointHandler.isSet()) {
			this.sb.append( ", \"" + tags.getWritingEndpointHandlerTag() + "\": ");
			addEndpointHandlerToJsonBuffer(event.writingEndpointHandler, this.sb, true, tags);
		}
		this.sb.append("}");		
		return this.sb.toString();
	}

	
	private boolean addStringElementToJsonBuffer(String elementName, String elementValue, StringBuilder buffer, boolean firstElement) {
		if (elementValue == null) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("\"" + escapeToJson(elementName) + "\": \"" + escapeToJson(elementValue) + "\"");
		return true;
	}

	private boolean addLongElementToJsonBuffer(String elementName, Long elementValue, StringBuilder buffer, boolean firstElement) {
		if (elementValue == null) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("\"" + escapeToJson(elementName) + "\": " + elementValue);
		return true;
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
		Application application = endpointHandler.application;
		if (application.isSet()) {
			if (added) {
				buffer.append(", ");
			}
			buffer.append("\"" + tags.getEndpointHandlerApplicationTag() + "\" : {");
			added = addStringElementToJsonBuffer(tags.getApplicationNameTag(), application.name, buffer, true);
			added = addStringElementToJsonBuffer(tags.getApplicationInstanceTag(), application.instance, buffer, !added) || added;
			added = addStringElementToJsonBuffer(tags.getApplicationVersionTag(), application.version, buffer, !added) || added;
			added = addStringElementToJsonBuffer(tags.getApplicationPrincipalTag(), application.principal, buffer, !added) || added;
			buffer.append("}");
		}
		buffer.append("}");
		return true;
	}
	
	private String escapeToJson(String value) {
		return value.replace("\"", "\\\"");
	}
}
