package com.jecstar.etm.processor.core.persisting.elastic;

import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.util.DateUtils;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for <code>TelemetryEvent</code> persisters that store their data in elasticsearch.
 *
 * @author Mark Holster
 */
public abstract class AbstractElasticTelemetryEventPersister {

    BulkProcessor bulkProcessor;

    private final EtmConfiguration etmConfiguration;
    private static final DateTimeFormatter dateTimeFormatterIndexPerDay = DateUtils.getIndexPerDayFormatter();

    AbstractElasticTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
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

    String getElasticIndexName() {
        return ElasticsearchLayout.EVENT_INDEX_PREFIX + dateTimeFormatterIndexPerDay.format(ZonedDateTime.now());
    }

    IndexRequest createIndexRequest(String id) {
        return new IndexRequest(getElasticIndexName(), ElasticsearchLayout.ETM_DEFAULT_TYPE, id)
                .waitForActiveShards(getActiveShardCount(this.etmConfiguration));
    }

    UpdateRequest createUpdateRequest(String id) {
        return new UpdateRequest(getElasticIndexName(), ElasticsearchLayout.ETM_DEFAULT_TYPE, id)
                .waitForActiveShards(getActiveShardCount(this.etmConfiguration))
                .retryOnConflict(this.etmConfiguration.getRetryOnConflictCount());

    }

    private ActiveShardCount getActiveShardCount(EtmConfiguration etmConfiguration) {
        if (-1 == etmConfiguration.getWaitForActiveShards()) {
            return ActiveShardCount.ALL;
        } else if (0 == etmConfiguration.getWaitForActiveShards()) {
            return ActiveShardCount.NONE;
        }
        return ActiveShardCount.from(etmConfiguration.getWaitForActiveShards());
    }

    void setCorrelationOnParent(TelemetryEvent<?> event) {
        if (event.correlationId == null || event.correlationId.equals(event.id)) {
            return;
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("correlating_id", event.id);
        bulkProcessor.add(createUpdateRequest(event.correlationId)
                .script(new Script(ScriptType.STORED, null, "etm_update-event-with-correlation", parameters))
                .upsert("{}", XContentType.JSON)
                .scriptedUpsert(true));
    }
}
