package com.jecstar.etm.processor.processor.persisting.elastic;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;

import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.domain.writers.json.BusinessTelemetryEventWriterJsonImpl;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.processor.persisting.TelemetryEventPersister;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;

public class BusinessTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
		implements TelemetryEventPersister<BusinessTelemetryEvent, BusinessTelemetryEventWriterJsonImpl> {

	public BusinessTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
		super(bulkProcessor, etmConfiguration);
	}

	@Override
	public void persist(BusinessTelemetryEvent event, BusinessTelemetryEventWriterJsonImpl writer) {
		IndexRequest indexRequest = createIndexRequest(event.id).source(writer.write(event));
		bulkProcessor.add(indexRequest);
	}

	@Override
	protected String getElasticTypeName() {
		return TelemetryCommand.CommandType.BUSINESS_EVENT.toStringType();
	}

}
