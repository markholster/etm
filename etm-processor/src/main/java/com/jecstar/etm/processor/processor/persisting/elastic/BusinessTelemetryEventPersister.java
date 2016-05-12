package com.jecstar.etm.processor.processor.persisting.elastic;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.converter.json.BusinessTelemetryEventConverterJsonImpl;
import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.processor.persisting.TelemetryEventPersister;

public class BusinessTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
		implements TelemetryEventPersister<BusinessTelemetryEvent, BusinessTelemetryEventConverterJsonImpl> {

	public BusinessTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
		super(bulkProcessor, etmConfiguration);
	}

	@Override
	public void persist(BusinessTelemetryEvent event, BusinessTelemetryEventConverterJsonImpl converter) {
		IndexRequest indexRequest = createIndexRequest(event.id).source(converter.convert(event));
		bulkProcessor.add(indexRequest);
	}

	@Override
	protected String getElasticTypeName() {
		return TelemetryCommand.CommandType.BUSINESS_EVENT.toStringType();
	}

}
