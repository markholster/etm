package com.jecstar.etm.domain.writers.json;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Map.Entry;

import com.jecstar.etm.domain.Application;
import com.jecstar.etm.domain.Endpoint;
import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.Location;
import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.domain.writers.TelemetryEventWriter;
import com.jecstar.etm.domain.writers.TelemetryEventTags;

/**
 * Converter class that converts a <code>TelemetryEvent</code> to a JSON string.
 * To prevent lots of garbage collections the internal buffer is reused, and
 * hence this class is not thread safe!
 * 
 * @author mark
 */
public abstract class AbstractJsonTelemetryEventWriter<Event extends TelemetryEvent<Event>> implements TelemetryEventWriter<String, Event> {
	
	private final TelemetryEventTags tags = new TelemetryEventTagsJsonImpl();
	final JsonWriter jsonWriter = new JsonWriter();

	@Override
	public String write(Event event) {
		return write(event, false);
	}	
	
	@Override
	public String write(Event event, boolean forUpdate) {
		final StringBuilder sb = new StringBuilder();
		sb.append("{");
		boolean added = this.jsonWriter.addLongElementToJsonBuffer(this.tags.getTimestampTag(), System.currentTimeMillis(), sb, true);
		added = this.jsonWriter.addStringElementToJsonBuffer(this.tags.getIdTag(), event.id, sb, !added) || added;
		added = this.jsonWriter.addStringElementToJsonBuffer(this.tags.getCorrelationIdTag(), event.correlationId, sb, !added) || added;
		added = addMapElementToJsonBuffer(this.tags.getCorrelationDataTag(), event.correlationData, sb, !added) || added;
		if (event.endpoints.size() != 0) {
			if (added) {
				sb.append(", ");
			}
			sb.append(this.jsonWriter.escapeToJson(getTags().getEndpointsTag(), true)).append(": [");
			boolean endpointAdded = false;
			for (int i = 0; i < event.endpoints.size(); i++) {
				endpointAdded = addEndpointToJsonBuffer(event.endpoints.get(i), sb, i == 0 || !endpointAdded, getTags(), forUpdate) || endpointAdded;
			}
			sb.append("]");			
			added = endpointAdded || added;
		}
		if (event.id != null) {
			if (added) {
				sb.append(", ");
			}
			sb.append(this.jsonWriter.escapeToJson(getTags().getEventHashesTag(), true)).append(": [");
			sb.append(event.getCalculatedHash());
			sb.append("]");
			added = true;
		}
		added = addMapElementToJsonBuffer(this.tags.getExtractedDataTag(), event.extractedData, sb, !added) || added;
		added = this.jsonWriter.addStringElementToJsonBuffer(this.tags.getNameTag(), event.name, sb, !added) || added;
		added = addMapElementToJsonBuffer(this.tags.getMetadataTag(), event.metadata, sb, !added) || added;
		added = this.jsonWriter.addStringElementToJsonBuffer(this.tags.getPayloadTag(), event.payload, sb, !added) || added;
		if (event.payloadFormat != null) {
			added = this.jsonWriter.addStringElementToJsonBuffer(this.tags.getPayloadFormatTag(), event.payloadFormat.name(), sb, !added) || added;
		}
		if (event.payload != null) {
			added = this.jsonWriter.addIntegerElementToJsonBuffer(this.tags.getPayloadLengthTag(), event.payload.length(), sb, !added) || added;
		}
		added = doWrite(event, sb, !added) || added;
		sb.append("}");
		return sb.toString();
	}
	
	abstract boolean doWrite(Event event, StringBuilder buffer, boolean firstElement);
	
	private boolean addMapElementToJsonBuffer(String elementName, Map<String, Object> elementValues, StringBuilder buffer, boolean firstElement) {
		if (elementValues.size() < 1) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append(this.jsonWriter.escapeToJson(elementName, true)).append(": {");
		firstElement = true;
		for (Entry<String, Object> entry : elementValues.entrySet()) {
			if (!firstElement) {
				buffer.append(", ");
			}
			buffer.append(this.jsonWriter.escapeObjectToJsonNameValuePair(entry.getKey(), entry.getValue()));
			firstElement = false;
		}
		buffer.append("}");
		return true;
	}
	
	private boolean addEndpointToJsonBuffer(Endpoint endpoint, StringBuilder buffer, boolean firstElement, TelemetryEventTags tags, boolean forUpdate) {
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
		added = this.jsonWriter.addStringElementToJsonBuffer(tags.getEndpointNameTag(), endpoint.name, buffer, true);
		if (readingEndpointHandlerSet) {
			if (added) {
				buffer.append(", ");
			}
			buffer.append(this.jsonWriter.escapeToJson(getTags().getReadingEndpointHandlersTag(), true)).append(": [");
			added = false;
			for (int i = 0; i < endpoint.readingEndpointHandlers.size(); i++) {
				added = addEndpointHandlerToJsonBuffer(endpoint.writingEndpointHandler.handlingTime, endpoint.readingEndpointHandlers.get(i), buffer, i == 0 || !added, getTags(), forUpdate) || added;
			}
			buffer.append("]");
			added = true;
		}
		if (endpoint.writingEndpointHandler.isSet()) {
			if (added) {
				buffer.append(", ");
			}
			buffer.append(this.jsonWriter.escapeToJson(this.tags.getWritingEndpointHandlerTag(), true)).append(": ");
			added = addEndpointHandlerToJsonBuffer(null, endpoint.writingEndpointHandler, buffer, true, this.tags, forUpdate) || added;
		}
		buffer.append("}");
		return added;
	}

	private boolean addEndpointHandlerToJsonBuffer(ZonedDateTime latencyStart, EndpointHandler endpointHandler, StringBuilder buffer, boolean firstElement, TelemetryEventTags tags, boolean forUpdate) {
		if (!endpointHandler.isSet()) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("{");
		boolean added = false;
		if (endpointHandler.handlingTime != null) {
			added = this.jsonWriter.addLongElementToJsonBuffer(tags.getEndpointHandlerHandlingTimeTag(), endpointHandler.handlingTime.toInstant().toEpochMilli(), buffer, true);
			if (latencyStart != null) {
				added = this.jsonWriter.addLongElementToJsonBuffer(tags.getEndpointHandlerLatencyTag(), endpointHandler.handlingTime.toInstant().toEpochMilli() - latencyStart.toInstant().toEpochMilli(), buffer, !added) || added;
			}
		}
		added = this.jsonWriter.addStringElementToJsonBuffer(this.tags.getEndpointHandlerTransactionIdTag(), endpointHandler.transactionId, buffer, !added) || added;
		if (forUpdate) {
			added = this.jsonWriter.addBooleanElementToJsonBuffer(this.tags.getEndpointHandlerForcedTag(), endpointHandler.forced, buffer, !added) || added;
		}
		Application application = endpointHandler.application;
		if (application.isSet()) {
			if (added) {
				buffer.append(", ");
			}
			buffer.append(this.jsonWriter.escapeToJson(tags.getEndpointHandlerApplicationTag(), true)).append(": {");
			added = this.jsonWriter.addStringElementToJsonBuffer(tags.getApplicationNameTag(), application.name, buffer, true);
			added = this.jsonWriter.addInetAddressElementToJsonBuffer(tags.getApplicationHostAddressTag(), tags.getApplicationHostNameTag(), application.hostAddress, buffer, !added) || added;
			added = this.jsonWriter.addStringElementToJsonBuffer(tags.getApplicationInstanceTag(), application.instance, buffer, !added) || added;
			added = this.jsonWriter.addStringElementToJsonBuffer(tags.getApplicationVersionTag(), application.version, buffer, !added) || added;
			added = this.jsonWriter.addStringElementToJsonBuffer(tags.getApplicationPrincipalTag(), application.principal, buffer, !added) || added;
			buffer.append("}");
		}
		Location location = endpointHandler.location;
		if (location.isSet()) {
			if (added) {
				buffer.append(", ");
			}
			buffer.append(this.jsonWriter.escapeToJson(tags.getEndpointHandlerLocationTag(), true)).append(": {");
			added = this.jsonWriter.addDoubleElementToJsonBuffer(tags.getLatitudeTag(), location.latitude, buffer, true);
			added = this.jsonWriter.addDoubleElementToJsonBuffer(tags.getLongitudeTag(), location.longitude, buffer, !added) || added;
			buffer.append("}");			
		}
		buffer.append("}");
		return true;
	}

	@Override
	public TelemetryEventTags getTags() {
		return this.tags;
	}
}
