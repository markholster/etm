package com.jecstar.etm.processor.processor.persisting.elastic;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.converter.json.LogTelemetryEventConverterJsonImpl;
import com.jecstar.etm.domain.LogTelemetryEvent;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.processor.persisting.TelemetryEventPersister;

public class LogTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
		implements TelemetryEventPersister<LogTelemetryEvent, LogTelemetryEventConverterJsonImpl> {

	public LogTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
		super(bulkProcessor, etmConfiguration);
	}

	@Override
	public void persist(LogTelemetryEvent event, LogTelemetryEventConverterJsonImpl converter) {
		IndexRequest indexRequest = createIndexRequest(event.id).source(converter.convert(event));
		bulkProcessor.add(indexRequest);
	}

	@Override
	protected String getElasticTypeName() {
		return TelemetryCommand.CommandType.LOG_EVENT.toStringType();
	}

}
