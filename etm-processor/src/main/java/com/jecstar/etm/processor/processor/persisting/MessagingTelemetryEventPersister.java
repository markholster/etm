package com.jecstar.etm.processor.processor.persisting;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.MessagingTelemetryEvent;
import com.jecstar.etm.core.domain.converter.json.MessagingTelemetryEventConverterJsonImpl;

public class MessagingTelemetryEventPersister extends AbstractTelemetryEventPersister
		implements TelemetryEventPersister<MessagingTelemetryEvent, MessagingTelemetryEventConverterJsonImpl> {

	public MessagingTelemetryEventPersister(final Client elasticClient, final EtmConfiguration etmConfiguration,
			final MetricRegistry metricRegistry) {
		super(elasticClient, etmConfiguration, metricRegistry);
	}

	@Override
	public void persist(MessagingTelemetryEvent event, MessagingTelemetryEventConverterJsonImpl converter) {
		startPersist();
		IndexRequest indexRequest = new IndexRequest(getElasticIndexName(), getElasticTypeName(), event.id)
				.consistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()))
				.source(converter.convert(event));
		// TODO add update script, and the responsetime in case of a response.
		bulkRequest.add(indexRequest);
		endPersist();
	}

	@Override
	protected String getElasticTypeName() {
		return "messaging";
	}
}
