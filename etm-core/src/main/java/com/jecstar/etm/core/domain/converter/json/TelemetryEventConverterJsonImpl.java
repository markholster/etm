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
public class TelemetryEventConverterJsonImpl extends AbstractJsonConverter implements TelemetryEventConverter<String> {
	
	private StringBuilder sb = new StringBuilder();

	@Override
	public String convert(TelemetryEvent event, TelemetryEventConverterTags tags) {
		this.sb.setLength(0);
		this.sb.append("{");
		boolean added = false;
		added = addStringElementToJsonBuffer(tags.getCorrelationIdTag(), event.correlationId, this.sb, !added) || added;
		added = addMapElementToJsonBuffer(tags.getCorrelationDataTag(), event.correlationData, this.sb, !added) || added;
		added = addStringElementToJsonBuffer(tags.getEndpointTag(), event.endpoint, this.sb, !added) || added;
		if (event.expiry != null) {
			added = addLongElementToJsonBuffer(tags.getExpiryTag(), event.expiry.toInstant().toEpochMilli(), this.sb, !added) || added;
		}
		added = addMapElementToJsonBuffer(tags.getExtractedDataTag(), event.extractedData, this.sb, !added) || added;
		added = addStringElementToJsonBuffer(tags.getNameTag(), event.name, this.sb, !added) || added;
		added = addMapElementToJsonBuffer(tags.getMetadataTag(), event.metadata, this.sb, !added) || added;
		added = addStringElementToJsonBuffer(tags.getPackagingTag(), event.packaging, this.sb, !added) || added;
		added = addStringElementToJsonBuffer(tags.getPayloadTag(), event.payload, this.sb, !added) || added;
		if (event.payloadFormat != null) {
			added = addStringElementToJsonBuffer(tags.getPayloadFormatTag(), event.payloadFormat.name(), this.sb, !added) || added;
		}
		if (event.isRequest() && event.writingEndpointHandler.isSet() && event.expiry != null) {
			// Set the response time to the expiry initially.
			added = addLongElementToJsonBuffer(tags.getResponseTimeTag(), event.expiry.toInstant().toEpochMilli() - event.writingEndpointHandler.handlingTime.toInstant().toEpochMilli(), this.sb, !added) || added;
		}
		added = addStringElementToJsonBuffer(tags.getTransactionIdTag(), event.transactionId, this.sb, !added) || added;
		if (event.transport != null) {
			added = addStringElementToJsonBuffer(tags.getTransportTag(), event.transport.name(), this.sb, !added) || added;
		}
		if (!event.readingEndpointHandlers.isEmpty()) {
			if (added) {
				this.sb.append(", ");
			}
			this.sb.append("\"" + tags.getReadingEndpointHandlersTag() + "\": [");
			added = false;
			for (int i = 0; i < event.readingEndpointHandlers.size(); i++) {
				added = addEndpointHandlerToJsonBuffer(event.readingEndpointHandlers.get(i), this.sb, i == 0 ? true : !added, tags) || added;
			}
			this.sb.append("]");
		}
		if (event.writingEndpointHandler.isSet()) {
			if (added) {
				this.sb.append(", ");
			}
			this.sb.append("\"" + tags.getWritingEndpointHandlerTag() + "\": ");
			addEndpointHandlerToJsonBuffer(event.writingEndpointHandler, this.sb, true, tags);
		}
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
}
