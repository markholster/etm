package com.jecstar.etm.processor.processor.persisting;

import java.io.Closeable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.configuration.EtmConfiguration;

public abstract class AbstractTelemetryEventPersister implements Closeable {
	
	protected final EtmConfiguration etmConfiguration;
	private final Client elasticClient;
	private final Timer bulkTimer;
	
	protected BulkRequestBuilder bulkRequest;
	
	private final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.YEAR, 4)
			.appendLiteral("-")
			.appendValue(ChronoField.MONTH_OF_YEAR, 2)
			.appendLiteral("-")
			.appendValue(ChronoField.DAY_OF_MONTH, 2).toFormatter().withZone(ZoneId.of("UTC"));


	public AbstractTelemetryEventPersister(final Client elasticClient, final EtmConfiguration etmConfiguration, final MetricRegistry metricRegistry) {
		this.elasticClient = elasticClient;
		this.etmConfiguration = etmConfiguration;
		this.bulkTimer = metricRegistry.timer("event-processor-persisting-repository-bulk-update");
	}

    protected void startPersist() {
		if (this.bulkRequest == null) {
			this.bulkRequest = this.elasticClient.prepareBulk();
		}
    }

    protected void endPersist() {
		if (this.bulkRequest.numberOfActions() >= this.etmConfiguration.getPersistingBulkSize()) {
			executeBulk();
		}
    }
    
    protected String getElasticIndexName() {
    	return "etm_event_" + this.dateTimeFormatter.format(ZonedDateTime.now());
    }
    
    protected abstract String getElasticTypeName();
	
	private void executeBulk() {
		if (this.bulkRequest != null && this.bulkRequest.numberOfActions() > 0) {
			final Context timerContext = this.bulkTimer.time();
			try {
				BulkResponse bulkResponse = this.bulkRequest.get();
				if (bulkResponse.hasFailures()) {
					throw new RuntimeException(bulkResponse.buildFailureMessage());
				}
			} finally {
				timerContext.stop();
			}
		}
		this.bulkRequest = null;
	}
	
	@Override
	public void close() {
		executeBulk();
	}
}
