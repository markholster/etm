package com.jecstar.etm.processor.processor.persisting;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.SqlTelemetryEvent;
import com.jecstar.etm.core.domain.converter.json.SqlTelemetryEventConverterJsonImpl;

public class SqlTelemetryEventPersister extends AbstractTelemetryEventPersister
		implements TelemetryEventPersister<SqlTelemetryEvent, SqlTelemetryEventConverterJsonImpl> {

	public SqlTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
		super(bulkProcessor, etmConfiguration);
	}

	@Override
	public void persist(SqlTelemetryEvent event, SqlTelemetryEventConverterJsonImpl converter) {
		IndexRequest indexRequest = createIndexRequest(event.id).source(converter.convert(event));
		bulkProcessor.add(indexRequest);
	}

	@Override
	protected String getElasticTypeName() {
		return "dbquery";
	}

}
