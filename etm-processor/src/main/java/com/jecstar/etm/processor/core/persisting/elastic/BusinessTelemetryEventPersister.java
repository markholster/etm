package com.jecstar.etm.processor.core.persisting.elastic;

import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.domain.writer.json.BusinessTelemetryEventWriterJsonImpl;
import com.jecstar.etm.processor.core.persisting.TelemetryEventPersister;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentType;

public class BusinessTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
		implements TelemetryEventPersister<BusinessTelemetryEvent, BusinessTelemetryEventWriterJsonImpl> {

	public BusinessTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
		super(bulkProcessor, etmConfiguration);
	}

	@Override
	public void persist(BusinessTelemetryEvent event, BusinessTelemetryEventWriterJsonImpl writer) {
		IndexRequest indexRequest = createIndexRequest(event.id).source(writer.write(event, false), XContentType.JSON);
		bulkProcessor.add(indexRequest);
		setCorrelationOnParent(event);
	}
}
