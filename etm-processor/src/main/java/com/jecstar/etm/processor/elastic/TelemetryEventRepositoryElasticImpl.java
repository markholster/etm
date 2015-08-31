package com.jecstar.etm.processor.elastic;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Map;
import java.util.stream.Collectors;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.Application;
import com.jecstar.etm.core.domain.EndpointHandler;
import com.jecstar.etm.core.domain.TelemetryEvent;
import com.jecstar.etm.processor.repository.AbstractTelemetryEventRepository;
import com.jecstar.etm.processor.repository.EndpointConfigResult;

public class TelemetryEventRepositoryElasticImpl extends AbstractTelemetryEventRepository {

	private final EtmConfiguration etmConfiguration;
	private final Client elasticClient;
	private final StringBuilder sb = new StringBuilder();
	private final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.YEAR, 4)
			.appendLiteral("-")
			.appendValue(ChronoField.MONTH_OF_YEAR, 2)
			.appendLiteral("-")
			.appendValue(ChronoField.DAY_OF_MONTH, 2).toFormatter();

	
	
	private BulkRequestBuilder bulkRequest;


	public TelemetryEventRepositoryElasticImpl(final EtmConfiguration etmConfiguration, final Client elasticClient) {
		this.etmConfiguration = etmConfiguration;
	    this.elasticClient = elasticClient;
    }
	
	@Override
    public void findEndpointConfig(String endpoint, EndpointConfigResult result, long cacheExpiryTime) {
    }

	@Override
    public void close() throws IOException {
		executeBulk();
    }

	@Override
    protected void startPersist(TelemetryEvent event) {
		if (this.bulkRequest == null) {
			this.bulkRequest = this.elasticClient.prepareBulk();
		}
    }

	@Override
    protected void endPersist() {
		if (this.bulkRequest.numberOfActions() >= this.etmConfiguration.getPersistingBulkCount()) {
			executeBulk();
		}
    }

	@Override
    protected void addTelemetryEvent(TelemetryEvent event) {
		// TODO create upserts for reading and writing applications and to store the response time.
		String index = "etm_" + event.getEventTime().format(this.dateTimeFormatter);
		IndexRequest indexRequest = new IndexRequest(index, event.payloadFormat.name().toLowerCase(), event.id)
				.consistencyLevel(WriteConsistencyLevel.ONE)
		        .source(eventToJson(event));
//		UpdateRequest updateRequest = new UpdateRequest(index, event.payloadFormat.name().toLowerCase(), event.id)
//		        .doc("")
//		        .consistencyLevel(WriteConsistencyLevel.ONE)
//		        .retryOnConflict(5)
//		        .upsert(indexRequest);
		this.bulkRequest.add(indexRequest);
    }
	
	private void executeBulk() {
		if (this.bulkRequest != null && this.bulkRequest.numberOfActions() > 0) {
			BulkResponse bulkResponse = this.bulkRequest.get();
			if (bulkResponse.hasFailures()) {
				throw new RuntimeException(bulkResponse.buildFailureMessage());
			}
		}
		this.bulkRequest = null;
	}
	
	private String eventToJson(TelemetryEvent event) {
		this.sb.setLength(0);
		this.sb.append("{");
		addStringElementToJsonBuffer("id", event.id, this.sb, true);
		addStringElementToJsonBuffer("correlation_id", event.correlationId, this.sb, false);
		addMapElementToJsonBuffer("correlation_data", event.correlationData, this.sb, false);
		addStringElementToJsonBuffer("endpoint", event.endpoint, this.sb, false);
		addMapElementToJsonBuffer("extracted_data", event.extractedData, this.sb, false);
		addStringElementToJsonBuffer("name", event.name, this.sb, false);
		addMapElementToJsonBuffer("metadata", event.metadata, this.sb, false);
		addStringElementToJsonBuffer("packaging", event.packaging, this.sb, false);
		addStringElementToJsonBuffer("payload", event.payload, this.sb, false);
		if (event.payloadFormat != null) {
			addStringElementToJsonBuffer("payload_format", event.payloadFormat.name(), this.sb, false);
		}
		if (event.transport != null) {
			addStringElementToJsonBuffer("transport", event.transport.name(), this.sb, false);
		}
		if (!event.readingEndpointHandlers.isEmpty()) {
			this.sb.append(", \"reading_endpoint_handlers\": [");
			boolean added = false;
			for (int i = 0; i < event.readingEndpointHandlers.size(); i++) {
				added = addEndpointHandlerToJsonBuffer(event.readingEndpointHandlers.get(i), this.sb, i == 0 ? true : added) || added;
			}
			this.sb.append("]");
		}
		if (!event.writingEndpointHandler.isSet()) {
			this.sb.append( ", \"writing_endpoint_handler\": ");
			addEndpointHandlerToJsonBuffer(event.writingEndpointHandler, this.sb, true);
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

	private boolean addEndpointHandlerToJsonBuffer(EndpointHandler endpointHandler, StringBuilder buffer, boolean firstElement) {
		if (endpointHandler.isSet()) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("{");
		boolean added = false;
		if (endpointHandler.handlingTime != null) {
			added = addLongElementToJsonBuffer("handling_time", endpointHandler.handlingTime.toInstant().toEpochMilli(), buffer, true);
		}
		Application application = endpointHandler.application;
		if (!application.isSet()) {
			if (added) {
				buffer.append(", ");
			}
			buffer.append("\"application\" : {");
			added = addStringElementToJsonBuffer("name", application.name, buffer, true);
			added = addStringElementToJsonBuffer("instance", application.instance, buffer, !added) || added;
			added = addStringElementToJsonBuffer("version", application.version, buffer, !added) || added;
			added = addStringElementToJsonBuffer("principal", application.principal, buffer, !added) || added;
			buffer.append("}");
		}
		buffer.append("}");
		return true;
	}
	
	private String escapeToJson(String value) {
		return value.replace("\"", "\\\"");
	}

}
