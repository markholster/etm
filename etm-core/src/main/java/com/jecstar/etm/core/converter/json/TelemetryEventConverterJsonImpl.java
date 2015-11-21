package com.jecstar.etm.core.converter.json;

import java.util.Map;
import java.util.stream.Collectors;

import com.jecstar.etm.core.TelemetryEvent;
import com.jecstar.etm.core.TelemetryEventDirection;
import com.jecstar.etm.core.TelemetryEventType;
import com.jecstar.etm.core.converter.TelemetryEventConverter;
import com.jecstar.etm.core.converter.TelemetryEventConverterTags;

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
		added = addStringElementToJsonBuffer(this.tags.getApplicationTag(), event.application, this.sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getContentTag(), event.content, this.sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getCorrelationIdTag(), event.correlationId, this.sb, !added) || added;
		added = addMapElementToJsonBuffer(this.tags.getCorrelationDataTag(), event.correlationData, this.sb, !added) || added;
		if (event.creationTime.getTime() != 0) {
			added = addLongElementToJsonBuffer(this.tags.getCreationTimeTag(), event.creationTime.getTime(), this.sb, !added) || added;
		}
		if (event.direction != null) {
			added = addStringElementToJsonBuffer(this.tags.getDirectionTag(), event.direction.name(), this.sb, !added) || added;
		}
		added = addStringElementToJsonBuffer(this.tags.getEndpointTag(), event.endpoint, this.sb, !added) || added;
		if (event.expiryTime.getTime() != 0) {
			added = addLongElementToJsonBuffer(this.tags.getExpiryTimeTag(), event.expiryTime.getTime(), this.sb, !added) || added;
		}
		added = addMapElementToJsonBuffer(this.tags.getMetadataTag(), event.metadata, this.sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getNameTag(), event.name, this.sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getTransactionIdTag(), event.transactionId, this.sb, !added) || added;
		added = addStringElementToJsonBuffer(this.tags.getTransactionNameTag(), event.transactionName, this.sb, !added) || added;
		if (event.type != null) {
			added = addStringElementToJsonBuffer(this.tags.getTypeTag(), event.type.name(), this.sb, !added) || added;
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
				.map(c -> "{\"" + this.tags.getMapKeyTag() + "\": \"" + escapeToJson(c.getKey()) + "\", \"" + this.tags.getMapValueTag() + "\": \"" + escapeToJson(c.getValue()) + "\"}")
				.sorted()
				.collect(Collectors.joining(", ")));
		buffer.append("]");
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
		telemetryEvent.application = getString(this.tags.getApplicationTag(), valueMap);
		telemetryEvent.content = getString(this.tags.getContentTag(), valueMap);
		telemetryEvent.correlationId = getString(this.tags.getCorrelationIdTag(), valueMap);
		getArray(this.tags.getCorrelationDataTag(), valueMap).forEach(c -> telemetryEvent.correlationData.put(c.get(this.tags.getMapKeyTag()).toString(), c.get(this.tags.getMapValueTag()).toString()));
		Long longValue = getLong(this.tags.getCreationTimeTag(), valueMap);
		if (longValue != null) {
			telemetryEvent.creationTime.setTime(longValue);
		}
		String stringValue = getString(this.tags.getDirectionTag(), valueMap);
		if (stringValue != null) {
			telemetryEvent.direction = TelemetryEventDirection.valueOf(stringValue);
		}
		telemetryEvent.endpoint = getString(this.tags.getEndpointTag(), valueMap);
		longValue = getLong(this.tags.getExpiryTimeTag(), valueMap);
		if (longValue != null) {
			telemetryEvent.expiryTime.setTime(longValue);
		}
		getArray(this.tags.getMetadataTag(), valueMap).forEach(c -> telemetryEvent.metadata.put(c.get(this.tags.getMapKeyTag()).toString(), c.get(this.tags.getMapValueTag()).toString()));
		telemetryEvent.name = getString(this.tags.getNameTag(), valueMap);
		telemetryEvent.transactionId = getString(this.tags.getTransactionIdTag(), valueMap);
		telemetryEvent.transactionName = getString(this.tags.getTransactionNameTag(), valueMap);
		stringValue = getString(this.tags.getTypeTag(), valueMap);
		if (stringValue != null) {
			telemetryEvent.type = TelemetryEventType.valueOf(stringValue);
		}
	}
}
