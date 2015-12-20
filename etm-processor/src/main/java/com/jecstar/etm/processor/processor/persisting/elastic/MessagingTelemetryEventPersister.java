package com.jecstar.etm.processor.processor.persisting.elastic;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.MessagingTelemetryEvent;
import com.jecstar.etm.core.domain.converter.json.MessagingTelemetryEventConverterJsonImpl;
import com.jecstar.etm.processor.processor.persisting.TelemetryEventPersister;

public class MessagingTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
		implements TelemetryEventPersister<MessagingTelemetryEvent, MessagingTelemetryEventConverterJsonImpl> {

	public MessagingTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
		super(bulkProcessor, etmConfiguration);
	}

	@Override
	public void persist(MessagingTelemetryEvent event, MessagingTelemetryEventConverterJsonImpl converter) {
		IndexRequest indexRequest = createIndexRequest(event.id)
				.source(converter.convert(event));
		// TODO create update event as this should be a request/reply aware persister 
		bulkProcessor.add(indexRequest);
	}

	@Override
	protected String getElasticTypeName() {
		return "messaging";
	}
}
