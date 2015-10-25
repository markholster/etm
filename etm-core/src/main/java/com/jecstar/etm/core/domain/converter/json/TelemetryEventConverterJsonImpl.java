package com.jecstar.etm.core.domain.converter.json;

import java.util.Map;
import java.util.stream.Collectors;

import com.jecstar.etm.core.domain.Application;
import com.jecstar.etm.core.domain.EndpointHandler;
import com.jecstar.etm.core.domain.Location;
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
	
	private final StringBuilder sb = new StringBuilder();
	private final TelemetryEventConverterTags tags = new TelemetryEventConverterTagsJsonImpl();

	@Override
	public String convert(TelemetryEvent event) {
		this.sb.setLength(0);
		this.sb.append("{");
		boolean added = false;
		added = addStringElementToJsonBuffer(this.tags.getIdTag(), event.id, this.sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getCorrelationIdTag(), event.correlationId, this.sb, !added) || added;
		added = addMapElementToJsonBuffer(this.tags.getCorrelationDataTag(), event.correlationData, this.sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getEndpointTag(), event.endpoint, this.sb, !added) || added;
		if (event.expiry != null) {
			added = addLongElementToJsonBuffer(this.tags.getExpiryTag(), event.expiry.toInstant().toEpochMilli(), this.sb, !added) || added;
		}
		added = addMapElementToJsonBuffer(this.tags.getExtractedDataTag(), event.extractedData, this.sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getNameTag(), event.name, this.sb, !added) || added;
		added = addMapElementToJsonBuffer(this.tags.getMetadataTag(), event.metadata, this.sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getPackagingTag(), event.packaging, this.sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getPayloadTag(), event.payload, this.sb, !added) || added;
		if (event.payloadFormat != null) {
			added = addStringElementToJsonBuffer(this.tags.getPayloadFormatTag(), event.payloadFormat.name(), this.sb, !added) || added;
		}
		if (event.isRequest() && event.writingEndpointHandler.isSet() && event.expiry != null) {
			// Set the response time to the expiry initially.
			added = addLongElementToJsonBuffer(this.tags.getResponseTimeTag(), event.expiry.toInstant().toEpochMilli() - event.writingEndpointHandler.handlingTime.toInstant().toEpochMilli(), this.sb, !added) || added;
		}
		added = addStringElementToJsonBuffer(this.tags.getTransactionIdTag(), event.transactionId, this.sb, !added) || added;
		if (event.transport != null) {
			added = addStringElementToJsonBuffer(this.tags.getTransportTag(), event.transport.name(), this.sb, !added) || added;
		}
		if (!event.readingEndpointHandlers.isEmpty()) {
			if (added) {
				this.sb.append(", ");
			}
			this.sb.append("\"" + this.tags.getReadingEndpointHandlersTag() + "\": [");
			added = false;
			for (int i = 0; i < event.readingEndpointHandlers.size(); i++) {
				added = addEndpointHandlerToJsonBuffer(event.readingEndpointHandlers.get(i), this.sb, i == 0 ? true : !added, this.tags) || added;
			}
			this.sb.append("]");
		}
		if (event.writingEndpointHandler.isSet()) {
			if (added) {
				this.sb.append(", ");
			}
			this.sb.append("\"" + this.tags.getWritingEndpointHandlerTag() + "\": ");
			addEndpointHandlerToJsonBuffer(event.writingEndpointHandler, this.sb, true, this.tags);
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
	public void convert(String jsonContent, TelemetryEvent telemetryEvent) {
		Map<String, Object> valueMap = toMap(jsonContent);
		telemetryEvent.initialize();
		telemetryEvent.id = getString(this.tags.getIdTag(), valueMap);
		telemetryEvent.correlationId = getString(this.tags.getCorrelationIdTag(), valueMap);
	}

}
