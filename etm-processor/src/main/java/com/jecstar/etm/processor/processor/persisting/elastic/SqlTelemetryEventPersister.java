package com.jecstar.etm.processor.processor.persisting.elastic;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;

import com.jecstar.etm.domain.SqlTelemetryEvent;
import com.jecstar.etm.domain.writers.json.SqlTelemetryEventWriterJsonImpl;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.processor.persisting.TelemetryEventPersister;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;

public class SqlTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
		implements TelemetryEventPersister<SqlTelemetryEvent, SqlTelemetryEventWriterJsonImpl> {

	public SqlTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
		super(bulkProcessor, etmConfiguration);
	}

	@Override
	public void persist(SqlTelemetryEvent event, SqlTelemetryEventWriterJsonImpl writer) {
		IndexRequest indexRequest = createIndexRequest(event.id).source(writer.write(event));
		// TODO create update event as this should be a request/reply aware persister 
		bulkProcessor.add(indexRequest);
	}

	@Override
	protected String getElasticTypeName() {
		return TelemetryCommand.CommandType.SQL_EVENT.toStringType();
	}

}
