package com.jecstar.etm.processor.elastic;

import java.io.IOException;
import java.io.StringWriter;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.jackson.core.JsonFactory;
import org.elasticsearch.common.jackson.core.JsonGenerator;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.TelemetryEvent;
import com.jecstar.etm.processor.repository.AbstractTelemetryEventRepository;
import com.jecstar.etm.processor.repository.EndpointConfigResult;

public class TelemetryEventRepositoryElasticImpl extends AbstractTelemetryEventRepository {

	private final EtmConfiguration etmConfiguration;
	private final Client elasticClient;
	private final JsonFactory jsonfactory = new JsonFactory();
	
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
		StringWriter writer = new StringWriter();
		JsonGenerator jsonGenerator;
		try {
			jsonGenerator = this.jsonfactory.createGenerator(writer);
			// TODO serialize event to json.
			jsonGenerator.close();
			IndexRequest indexRequest = new IndexRequest("etm", event.telemetryEventType.name().toLowerCase(), event.id)
			        .source(writer.toString());
			UpdateRequest updateRequest = new UpdateRequest("etm", event.telemetryEventType.name().toLowerCase(), event.id)
			        .doc(writer.toString())
			        .upsert(indexRequest);              
			this.bulkRequest.add(updateRequest);
		} catch (IOException e) {
			// TODO handle exception.
			e.printStackTrace();
		}
    }
	
	private void executeBulk() {
		BulkResponse bulkResponse = this.bulkRequest.execute().actionGet();
		this.bulkRequest = null;
	}

}
