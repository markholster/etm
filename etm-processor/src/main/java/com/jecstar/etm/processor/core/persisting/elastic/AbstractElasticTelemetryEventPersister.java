package com.jecstar.etm.processor.core.persisting.elastic;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService.ScriptType;

import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;

/**
 * Base class for <code>TelemetryEvent</code> persisters that store their data in elasticsearch.
 * 
 * @author Mark Holster
 */
public abstract class AbstractElasticTelemetryEventPersister {
	
	private final EtmConfiguration etmConfiguration;
	protected BulkProcessor bulkProcessor;
	
	public static final DateTimeFormatter dateTimeFormatterIndexPerDay = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.YEAR, 4)
			.appendLiteral("-")
			.appendValue(ChronoField.MONTH_OF_YEAR, 2)
			.appendLiteral("-")
			.appendValue(ChronoField.DAY_OF_MONTH, 2).toFormatter().withZone(ZoneId.of("UTC"));

	public AbstractElasticTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
		this.bulkProcessor = bulkProcessor;
		this.etmConfiguration = etmConfiguration;
	}
	
	/**
	 * Setter method that is used by the
	 * <code>CommandResourcesElasticImpl</code> class when the configuration of
	 * the <code>BulkProcessor</code> changes. Because the
	 * <code>BulkProcessor</code> is immutable when started a completely new
	 * instance should be "injected" when one of the configuration settings
	 * changes. 
	 * 
	 * @param bulkProcessor The <code>BulkProcess</code> to use for bulk request.
	 */
	public void setBulkProcessor(BulkProcessor bulkProcessor) {
		this.bulkProcessor = bulkProcessor;
	}
    
    protected String getElasticIndexName() {
    	return ElasticSearchLayout.ETM_EVENT_INDEX_PREFIX + dateTimeFormatterIndexPerDay.format(ZonedDateTime.now());
    }
    
    protected abstract String getElasticTypeName();
    
    protected IndexRequest createIndexRequest(String id) {
    	return new IndexRequest(getElasticIndexName(), getElasticTypeName(), id)
    			.waitForActiveShards(getActiveShardCount(this.etmConfiguration));
    }
    
    protected UpdateRequest createUpdateRequest(String id) {
    	return new UpdateRequest(getElasticIndexName(), getElasticTypeName(), id)
    			.waitForActiveShards(getActiveShardCount(this.etmConfiguration))
    			.retryOnConflict(this.etmConfiguration.getRetryOnConflictCount());
    	
    }
    
    protected ActiveShardCount getActiveShardCount(EtmConfiguration etmConfiguration) {
    	if (-1 == etmConfiguration.getWaitForActiveShards()) {
    		return ActiveShardCount.ALL;
    	} else if (0 == etmConfiguration.getWaitForActiveShards()) {
    		return ActiveShardCount.NONE;
    	}
    	return ActiveShardCount.from(etmConfiguration.getWaitForActiveShards());
    }
    
    protected void setCorrelationOnParent(TelemetryEvent<?> event) {
    	if (event.correlationId == null || event.correlationId.equals(event.id)) {
    		return;
    	}
		Map<String, Object> parameters =  new HashMap<>();
		parameters.put("correlating_id", event.id);
		bulkProcessor.add(createUpdateRequest(event.correlationId)
				.script(new Script("etm_update-event-with-correlation", ScriptType.STORED, "painless", parameters))
				.upsert("{}")
				.scriptedUpsert(true));
    }

}
