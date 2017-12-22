package com.jecstar.etm.processor.core.persisting.elastic;

import com.jecstar.etm.domain.LogTelemetryEvent;
import com.jecstar.etm.processor.core.persisting.TelemetryEventPersister;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.LogTelemetryEventConverterJsonImpl;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentType;

public class LogTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
		implements TelemetryEventPersister<LogTelemetryEvent, LogTelemetryEventConverterJsonImpl> {

	public LogTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
		super(bulkProcessor, etmConfiguration);
	}

	@Override
	public void persist(LogTelemetryEvent event, LogTelemetryEventConverterJsonImpl converter) {
		IndexRequest indexRequest = createIndexRequest(event.id).source(converter.write(event, false), XContentType.JSON);
		bulkProcessor.add(indexRequest);
		setCorrelationOnParent(event);
	}
}
