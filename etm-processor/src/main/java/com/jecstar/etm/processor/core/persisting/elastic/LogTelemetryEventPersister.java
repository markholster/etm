package com.jecstar.etm.processor.core.persisting.elastic;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentType;

import com.jecstar.etm.domain.LogTelemetryEvent;
import com.jecstar.etm.domain.writers.json.LogTelemetryEventWriterJsonImpl;
import com.jecstar.etm.processor.core.persisting.TelemetryEventPersister;
import com.jecstar.etm.server.core.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;

public class LogTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
		implements TelemetryEventPersister<LogTelemetryEvent, LogTelemetryEventWriterJsonImpl> {

	public LogTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
		super(bulkProcessor, etmConfiguration);
	}

	@Override
	public void persist(LogTelemetryEvent event, LogTelemetryEventWriterJsonImpl writer) {
		IndexRequest indexRequest = createIndexRequest(event.id).source(writer.write(event), XContentType.JSON);
		bulkProcessor.add(indexRequest);
		setCorrelationOnParent(event);
	}

	@Override
	protected String getElasticTypeName() {
		return ElasticsearchLayout.ETM_EVENT_INDEX_TYPE_LOG;
	}

}
