package com.jecstar.etm.server.core.enhancers;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.jecstar.etm.domain.Endpoint;
import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.PayloadFormat;
import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.server.core.enhancers.DefaultField.WritePolicy;
import com.jecstar.etm.server.core.parsers.ExpressionParser;
import com.jecstar.etm.server.core.parsers.ExpressionParserField;

public class DefaultTelemetryEventEnhancer implements TelemetryEventEnhancer {
	
	private boolean enhancePayloadFormat = true;
	
	private final List<DefaultField> fields = new ArrayList<>();
	
	@Override
	public void enhance(TelemetryEvent<?> event, ZonedDateTime enhanceTime) {
		enchanceId(event);
		enchancePayloadFormat(event);
		enchanceWritingHandlerTimes(event, enhanceTime);
		enhanceFields(event);
	}
	
	public void setEnhancePayloadFormat(boolean enhancePayloadFormat) {
		this.enhancePayloadFormat = enhancePayloadFormat;
	}
	
	public boolean isEnhancePayloadFormat() {
		return this.enhancePayloadFormat;
	}
	
	public List<DefaultField> getFields() {
		return this.fields;
	}
	
	public void addField(DefaultField field) {
		Optional<DefaultField> optional = this.fields.stream().filter(f -> f.getName().equals(field.getName())).findFirst();
		if (optional.isPresent()) {
			DefaultField defaultField = optional.get();
			defaultField.addParsers(field.getParsers());
		} else {
			this.fields.add(field);
		}
	}
	
	/**
	 * Merge the field <code>ExpressionParsers</code> of another <code>DefaultTelemetryEventEnhancer</code> to this one.
	 * 
	 * @param other The other <code>DefaultTelemetryEventEnhancer</code> to merge into this one.
	 */
	public void mergeFieldParsers(DefaultTelemetryEventEnhancer other) {
		if (other.fields.isEmpty()) {
			// Nothing to merge.
			return;
		}
		if (this.fields.isEmpty()) {
			// Current parsers is empty, just overwrite with other.
			this.fields.addAll(other.fields);
			return;
		}
		for (DefaultField field : other.getFields()) {
			Optional<DefaultField> optional = this.fields.stream().filter(p -> p.getName().equals(field.getName())).findFirst();
			if (optional.isPresent()) {
				// Both enhancers contain the same key. Append the "other" parsers to the current ones.
				optional.get().addParsers(field.getParsers());
			} else {
				this.fields.add(field);
			}
		}
	}
	
	private void enchanceId(final TelemetryEvent<?> event) {
		Optional<DefaultField> optional =  this.fields.stream().filter(p -> p.getName().equals(ExpressionParserField.ID.getJsonTag())).findFirst();
		// Check if the id needs to be set by a parsers.
		if (optional.isPresent()) {
			conditionallySetValue(event, optional.get());
		}
		if (event.id != null) {
			return;
		}
		event.id = UUID.randomUUID().toString();
	}
	
	private void enchancePayloadFormat(final TelemetryEvent<?> event) {
		if (event.payloadFormat == null && this.enhancePayloadFormat) {
			event.payloadFormat = detectPayloadFormat(event.payload);
		}
	}
	
	private void enchanceWritingHandlerTimes(final TelemetryEvent<?> event, final ZonedDateTime enhanceTime) {
		if (event.endpoints.size() == 0) {
			Endpoint endpoint = new Endpoint();
			endpoint.writingEndpointHandler.handlingTime = enhanceTime;
			// This writing endpoint handler is forced added because there should always be a writing endpoint handler. 
			endpoint.writingEndpointHandler.forced = true;
			event.endpoints.add(endpoint);
		} else {
			for (Endpoint endpoint : event.endpoints) {
				if (endpoint.writingEndpointHandler.handlingTime == null) {
					ZonedDateTime earliestReadTime = endpoint.getEarliestReadTime();
					if (earliestReadTime != null) {
						endpoint.writingEndpointHandler.handlingTime = earliestReadTime;
					} else {
						endpoint.writingEndpointHandler.handlingTime = enhanceTime;
					}
					endpoint.writingEndpointHandler.forced = true;
				}				
			}
		}
	}
	
	/**
	 * Super simple payload format detector. This isn't an enhanced detection
	 * algorithm because we won't be losing to much performance here. The end
	 * user should be able to tell the system which format it is anyway.
	 * 
	 * @param payload The payload.
	 * @return The detected <code>PayloadFormat</code>.
	 */
	private PayloadFormat detectPayloadFormat(String payload) {
		if (payload == null) {
			return null;
		}
		String trimmed = payload.toLowerCase().trim();
		if (trimmed.contains("http://schemas.xmlsoap.org/soap/envelope/") || trimmed.contains("http://www.w3.org/2003/05/soap-envelope")) {
			return PayloadFormat.SOAP;
		} else if (trimmed.contains("<!doctype html")) {
			return PayloadFormat.HTML;
		} else if (trimmed.startsWith("<?xml ")) {
			return PayloadFormat.XML;
		} else if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
			return PayloadFormat.XML;
		} else if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
			return PayloadFormat.JSON;
		} else if (trimmed.startsWith("select")
				   || trimmed.startsWith("insert")
				   || trimmed.startsWith("update")
				   || trimmed.startsWith("delete")
				   || trimmed.startsWith("drop")
				   || trimmed.startsWith("create")) {
			return PayloadFormat.SQL;
		}
		return null;
	}
	
	
	private void enhanceFields(TelemetryEvent<?> event) {
		if (this.fields.size() == 0) {
			return;
		}
		for (DefaultField field : this.fields) {
			conditionallySetValue(event, field);
		}
			
	}

	private void conditionallySetValue(TelemetryEvent<?> event, DefaultField field) {
		if (field == null) {
			return;
		}
		if (ExpressionParserField.ID.getJsonTag().equals(field.getName())) {
			if (field.getWritePolicy().equals(WritePolicy.ALWAYS_OVERWRITE) 
				|| (field.getWritePolicy().equals(WritePolicy.WHEN_EMPTY) && event.id == null)) {
				event.id = parseValue(field.getParsers(), event.payload);
			} else if (field.getWritePolicy().equals(WritePolicy.OVERWRITE_WHEN_FOUND)) {
				String value = parseValue(field.getParsers(), event.payload);
				if (value != null) {
					event.id = value;
				}
			}
		} else if (ExpressionParserField.CORRELATION_ID.getJsonTag().equals(field.getName())) {
			if (field.getWritePolicy().equals(WritePolicy.ALWAYS_OVERWRITE) 
					|| (field.getWritePolicy().equals(WritePolicy.WHEN_EMPTY) && event.correlationId == null)) {
				event.correlationId = parseValue(field.getParsers(), event.payload);
			} else if (field.getWritePolicy().equals(WritePolicy.OVERWRITE_WHEN_FOUND)) {
				String value = parseValue(field.getParsers(), event.payload);
				if (value != null) {
					event.correlationId = value;
				}
			}
		} else if (ExpressionParserField.NAME.getJsonTag().equals(field.getName())) {
			if (field.getWritePolicy().equals(WritePolicy.ALWAYS_OVERWRITE) 
					|| (field.getWritePolicy().equals(WritePolicy.WHEN_EMPTY) && event.name == null)) {
				event.name = parseValue(field.getParsers(), event.payload);
			} else if (field.getWritePolicy().equals(WritePolicy.OVERWRITE_WHEN_FOUND)) {
				String value = parseValue(field.getParsers(), event.payload);
				if (value != null) {
					event.name = value;
				}
			}
		} else if (field.getName().startsWith(ExpressionParserField.CORRELATION_DATA.getJsonTag())) {
			conditionallyPutInMap(event, field, ExpressionParserField.CORRELATION_DATA, event.correlationData);
		} else if (field.getName().startsWith(ExpressionParserField.EXTRACTED_DATA.getJsonTag())) {
			conditionallyPutInMap(event, field, ExpressionParserField.EXTRACTED_DATA, event.extractedData);
		} else if (field.getName().startsWith(ExpressionParserField.METADATA.getJsonTag())) {
			conditionallyPutInMap(event, field, ExpressionParserField.METADATA, event.metadata);
		} else if (ExpressionParserField.WRITER_TRANSACTION_ID.getJsonTag().equals(field.getName())) {
			// transaction id can be on several levels. Add them to every position when the current value is not set.
			setWritingTransactionIdOnEndpoints(field, event);
		} else if (ExpressionParserField.READER_TRANSACTION_ID.getJsonTag().equals(field.getName())) {
			// transaction id can be on several levels. Add them to every position when the current value is not set.
			setReadingTransactionIdOnEndpoints(field, event);
		}
	}
	
	private void setWritingTransactionIdOnEndpoints(DefaultField field, TelemetryEvent<?> event) {
		if (event.endpoints == null || event.endpoints.size() < 1) {
			return;
		}
		// Do everything to prevent the slow extraction of the field value from the data. Speed is everything...
		if (field.getWritePolicy().equals(WritePolicy.ALWAYS_OVERWRITE)) {
			String transactionId = parseValue(field.getParsers(), event.payload);
			if (transactionId != null) {
				for (Endpoint endpoint : event.endpoints) {
					endpoint.writingEndpointHandler.transactionId = transactionId;
				}
			}
		} else {
			List<EndpointHandler> emptyEndpointHanlders = event.endpoints.stream().map(f -> f.writingEndpointHandler).filter(p -> p.transactionId == null).collect(Collectors.toList());
			if (emptyEndpointHanlders != null && emptyEndpointHanlders.size() > 0) {
				String transactionId = parseValue(field.getParsers(), event.payload);
				for (EndpointHandler endpointHandler : emptyEndpointHanlders) {
					if ((field.getWritePolicy().equals(WritePolicy.OVERWRITE_WHEN_FOUND) && transactionId != null) || endpointHandler.transactionId == null) {
						endpointHandler.transactionId = transactionId;
					}
				}
			}
		}
	}
	
	private void setReadingTransactionIdOnEndpoints(DefaultField field, TelemetryEvent<?> event) {
		if (event.endpoints == null || event.endpoints.size() < 1) {
			return;
		}
		// Do everything to prevent the slow extraction of the field value from the data. Speed is everything...
		if (field.getWritePolicy().equals(WritePolicy.ALWAYS_OVERWRITE)) {
			String transactionId = parseValue(field.getParsers(), event.payload);
			if (transactionId != null) {
				for (Endpoint endpoint : event.endpoints) {
					for (EndpointHandler handler : endpoint.readingEndpointHandlers) {
						handler.transactionId = transactionId;
					}
				}
			}
		} else {
			List<EndpointHandler> emptyEndpointHanlders = event.endpoints.stream().flatMap(f -> f.readingEndpointHandlers.stream()).filter(p -> p.transactionId == null).collect(Collectors.toList());
			if (emptyEndpointHanlders != null && emptyEndpointHanlders.size() > 0) {
				String transactionId = parseValue(field.getParsers(), event.payload);
				for (EndpointHandler endpointHandler : emptyEndpointHanlders) {
					if ((field.getWritePolicy().equals(WritePolicy.OVERWRITE_WHEN_FOUND) && transactionId != null) || endpointHandler.transactionId == null) {
						endpointHandler.transactionId = transactionId;
					}
				}
			}
		}
	}

	private void conditionallyPutInMap(TelemetryEvent<?> event, DefaultField field, ExpressionParserField parserField, Map<String, Object> container) {
		String dataKey = parserField.getCollectionKeyName(field.getName());
		if (field.getWritePolicy().equals(WritePolicy.ALWAYS_OVERWRITE) 
				|| (field.getWritePolicy().equals(WritePolicy.WHEN_EMPTY) && !container.containsKey(dataKey))) {
			String value = parseValue(field.getParsers(), event.payload);
			if (value == null) {
				container.remove(dataKey);
			} else {
				container.put(dataKey, parseValue(field.getParsers(), event.payload));
			}
		} else if (field.getWritePolicy().equals(WritePolicy.OVERWRITE_WHEN_FOUND)) {
			String value = parseValue(field.getParsers(), event.payload);
			if (value != null) {
				container.put(dataKey, value);
			}
		}
	}
	
	private String parseValue(List<ExpressionParser> expressionParsers, String payload) {
		if (payload == null || expressionParsers == null) {
			return null;
		}
		for (ExpressionParser expressionParser : expressionParsers) {
			String value = parseValue(expressionParser, payload);
			if (value != null) {
				return value;
			}
		}
		return null;
    }
	
	private String parseValue(ExpressionParser expressionParser, String payload) {
		if (expressionParser == null || payload == null) {
			return null;
		}
		String value = expressionParser.evaluate(payload);
		if (value != null && value.trim().length() > 0) {
			return value;
		}
		return null;
	}

}
