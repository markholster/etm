package com.jecstar.etm.processor.processor.persisting.elastic;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;

import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.writers.json.HttpTelemetryEventWriterJsonImpl;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.processor.persisting.TelemetryEventPersister;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;

public class HttpTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
		implements TelemetryEventPersister<HttpTelemetryEvent, HttpTelemetryEventWriterJsonImpl> {

	public HttpTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
		super(bulkProcessor, etmConfiguration);
	}

	@Override
	public void persist(HttpTelemetryEvent event, HttpTelemetryEventWriterJsonImpl writer) {
		IndexRequest indexRequest = createIndexRequest(event.id).source(writer.write(event));
		// TODO create update event as this should be a request/reply aware persister 
		bulkProcessor.add(indexRequest);
	}

	@Override
	protected String getElasticTypeName() {
		return TelemetryCommand.CommandType.HTTP_EVENT.toStringType();
	}

}
