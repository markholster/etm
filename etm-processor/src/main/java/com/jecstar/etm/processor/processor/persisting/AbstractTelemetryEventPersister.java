package com.jecstar.etm.processor.processor.persisting;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;

import com.jecstar.etm.core.configuration.EtmConfiguration;

public abstract class AbstractTelemetryEventPersister {
	
	private final EtmConfiguration etmConfiguration;
	protected BulkProcessor bulkProcessor;
	
	private final DateTimeFormatter dateTimeFormatterIndexPerDay = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.YEAR, 4)
			.appendLiteral("-")
			.appendValue(ChronoField.MONTH_OF_YEAR, 2)
			.appendLiteral("-")
			.appendValue(ChronoField.DAY_OF_MONTH, 2).toFormatter().withZone(ZoneId.of("UTC"));

	public AbstractTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
		this.bulkProcessor = bulkProcessor;
		this.etmConfiguration = etmConfiguration;
	}
	
	public void setBulkProcessor(BulkProcessor bulkProcessor) {
		this.bulkProcessor = bulkProcessor;
	}
    
    protected String getElasticIndexName() {
    	return "etm_event_" + this.dateTimeFormatterIndexPerDay.format(ZonedDateTime.now());
    }
    
    protected abstract String getElasticTypeName();
    
    protected IndexRequest createIndexRequest(String id) {
    	return new IndexRequest(getElasticIndexName(), getElasticTypeName(), id)
    			.consistencyLevel(WriteConsistencyLevel.valueOf(this.etmConfiguration.getWriteConsistency().name()));
    }	

}
