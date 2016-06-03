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
	protected final JsonWriter jsonWriter = new JsonWriter();

	@Override
	public String write(Event event) {
		final StringBuilder sb = new StringBuilder();
		boolean added = false;
		sb.append("{");
		added = this.jsonWriter.addStringElementToJsonBuffer(this.tags.getIdTag(), event.id, sb, !added) || added;
		added = this.jsonWriter.addStringElementToJsonBuffer(this.tags.getCorrelationIdTag(), event.correlationId, sb, !added) || added;
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
		if (added) {
			sb.append(", ");
		}
		sb.append("\"" + getTags().getEventHashesTag() + "\": [");
		sb.append(event.getCalculatedHash());
		sb.append("]");
		added = true;
		added = addMapElementToJsonBuffer(this.tags.getExtractedDataTag(), event.extractedData, sb, !added) || added;
		added = this.jsonWriter.addStringElementToJsonBuffer(this.tags.getNameTag(), event.name, sb, !added) || added;
		added = addMapElementToJsonBuffer(this.tags.getMetadataTag(), event.metadata, sb, !added) || added;
		added = this.jsonWriter.addStringElementToJsonBuffer(this.tags.getPayloadTag(), event.payload, sb, !added) || added;
		if (event.payloadFormat != null) {
			added = this.jsonWriter.addStringElementToJsonBuffer(this.tags.getPayloadFormatTag(), event.payloadFormat.name(), sb, !added) || added;
		}
		added = doWrite(event, sb, !added) || added;
		sb.append("}");
		return sb.toString();
	}
	
	abstract boolean doWrite(Event event, StringBuilder buffer, boolean firstElement);
	
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
			buffer.append(this.jsonWriter.escapeObjectToJsonNameValuePair(entry.getKey(), entry.getValue()));
			firstElement = false;
		}
		buffer.append("}");
		return true;
	}
	
	private boolean addEndpointToJsonBuffer(Endpoint endpoint, StringBuilder buffer, boolean firstElement, TelemetryEventTags tags) {
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
			buffer.append("\"" + getTags().getReadingEndpointHandlersTag() + "\": [");
			added = false;
			for (int i = 0; i < endpoint.readingEndpointHandlers.size(); i++) {
				added = addEndpointHandlerToJsonBuffer(endpoint.writingEndpointHandler.handlingTime, endpoint.readingEndpointHandlers.get(i), buffer, i == 0 ? true : !added, getTags()) || added;
			}
			buffer.append("]");
			added = true;
		}
		if (endpoint.writingEndpointHandler.isSet()) {
			if (added) {
				buffer.append(", ");
			}
			buffer.append("\"" + this.tags.getWritingEndpointHandlerTag() + "\": ");
			added = addEndpointHandlerToJsonBuffer(null, endpoint.writingEndpointHandler, buffer, true, this.tags) || added;
		}
		buffer.append("}");
		return added;
	}

	private boolean addEndpointHandlerToJsonBuffer(ZonedDateTime latencyStart, EndpointHandler endpointHandler, StringBuilder buffer, boolean firstElement, TelemetryEventTags tags) {
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
		Application application = endpointHandler.application;
		if (application.isSet()) {
			if (added) {
				buffer.append(", ");
			}
			buffer.append("\"" + tags.getEndpointHandlerApplicationTag() + "\" : {");
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
			buffer.append("\"" + tags.getEndpointHandlerLocationTag() + "\" : {");
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
