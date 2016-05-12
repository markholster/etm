package com.jecstar.etm.processor.processor.persisting.elastic;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.converter.json.SqlTelemetryEventConverterJsonImpl;
import com.jecstar.etm.domain.SqlTelemetryEvent;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.processor.persisting.TelemetryEventPersister;

public class SqlTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
		implements TelemetryEventPersister<SqlTelemetryEvent, SqlTelemetryEventConverterJsonImpl> {

	public SqlTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
		super(bulkProcessor, etmConfiguration);
	}

	@Override
	public void persist(SqlTelemetryEvent event, SqlTelemetryEventConverterJsonImpl converter) {
		IndexRequest indexRequest = createIndexRequest(event.id).source(converter.convert(event));
		// TODO create update event as this should be a request/reply aware persister 
		bulkProcessor.add(indexRequest);
	}

	@Override
	protected String getElasticTypeName() {
		return TelemetryCommand.CommandType.SQL_EVENT.toStringType();
	}

}
