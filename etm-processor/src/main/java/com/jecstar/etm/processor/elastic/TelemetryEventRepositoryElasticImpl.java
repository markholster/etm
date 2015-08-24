package com.jecstar.etm.processor.elastic;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.TelemetryEvent;
import com.jecstar.etm.processor.repository.AbstractTelemetryEventRepository;
import com.jecstar.etm.processor.repository.EndpointConfigResult;

public class TelemetryEventRepositoryElasticImpl extends AbstractTelemetryEventRepository {

	private final EtmConfiguration etmConfiguration;
	private final Client elasticClient;
	private final StringBuilder sb = new StringBuilder();
	
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
		if (this.bulkRequest.numberOfActions() > this.etmConfiguration.getPersistingBulkCount()) {
			executeBulk();
		}
    }

	@Override
    protected void addTelemetryEvent(TelemetryEvent event) {
			// TODO serialize to json
			IndexRequest indexRequest = new IndexRequest("etm", event.telemetryEventType.name().toLowerCase(), event.id)
			        .source(eventToJson(event));
//			UpdateRequest updateRequest = new UpdateRequest("etm", event.telemetryEventType.name().toLowerCase(), event.id)
//			        .doc("")
//			        .upsert(indexRequest);              
			this.bulkRequest.add(indexRequest);
    }
	
	private void executeBulk() {
		BulkResponse bulkResponse = this.bulkRequest.execute().actionGet();
		this.bulkRequest = null;
	}
	
	private String eventToJson(TelemetryEvent event) {
		this.sb.setLength(0);
		this.sb.append("{");
		this.sb.append( "\"id\": \"" + event.id + "\"");
		addStringElementToJsonBuffer("correlation_id", event.correlationId, this.sb);
		addMapElementToJsonBuffer("correlation_data", event.correlationData, this.sb);
		addStringElementToJsonBuffer("endpoint", event.endpoint, this.sb);
		addStringElementToJsonBuffer("event_type", event.telemetryEventType.name(), this.sb);
		addStringElementToJsonBuffer("name", event.name, this.sb);
		addMapElementToJsonBuffer("metadata", event.metadata, this.sb);
		addStringElementToJsonBuffer("payload", event.payload, this.sb);
		addStringElementToJsonBuffer("transport_type", event.transportType.name(), this.sb);
		// TODO add reading and writing endpoint handlers.
		this.sb.append("}");
		return this.sb.toString();
	}
	
	private void addStringElementToJsonBuffer(String elementName, String elementValue, StringBuilder buffer) {
		if (elementValue == null) {
			return;
		}
		buffer.append(", \"" + elementName + "\": \"" + elementValue + "\"");
	}
	
	private void addMapElementToJsonBuffer(String elementName, Map<String, String> elementValues, StringBuilder buffer) {
		if (elementValues.size() < 1) {
			return;
		}
		buffer.append(", \"" + elementName + "\": [");
		buffer.append(elementValues.entrySet().stream()
				.map(c -> "{ \"" + c.getKey() + "\": \"" + c.getValue() + "\" }")
				.sorted()
				.collect(Collectors.joining(",")));
		buffer.append("]");
	}

}
