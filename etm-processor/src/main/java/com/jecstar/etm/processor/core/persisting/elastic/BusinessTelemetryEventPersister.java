package com.jecstar.etm.processor.core.persisting.elastic;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentType;

import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.domain.writers.json.BusinessTelemetryEventWriterJsonImpl;
import com.jecstar.etm.processor.core.persisting.TelemetryEventPersister;
import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;

public class BusinessTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
		implements TelemetryEventPersister<BusinessTelemetryEvent, BusinessTelemetryEventWriterJsonImpl> {

	public BusinessTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
		super(bulkProcessor, etmConfiguration);
	}

	@Override
	public void persist(BusinessTelemetryEvent event, BusinessTelemetryEventWriterJsonImpl writer) {
		IndexRequest indexRequest = createIndexRequest(event.id).source(writer.write(event), XContentType.JSON);
		bulkProcessor.add(indexRequest);
		setCorrelationOnParent(event);
	}

	@Override
	protected String getElasticTypeName() {
		return ElasticSearchLayout.ETM_EVENT_INDEX_TYPE_BUSINESS;
	}

}
