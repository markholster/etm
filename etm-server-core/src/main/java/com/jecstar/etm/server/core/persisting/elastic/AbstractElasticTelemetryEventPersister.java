/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.server.core.persisting.elastic;

import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.configuration.LicenseRateLimiter;
import com.jecstar.etm.server.core.elasticsearch.builder.IndexRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.UpdateRequestBuilder;
import com.jecstar.etm.server.core.persisting.RequestEnhancer;
import com.jecstar.etm.server.core.util.DateUtils;
import com.jecstar.etm.server.core.util.IdGenerator;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

/**
 * Base class for <code>TelemetryEvent</code> persisters that store their data in elasticsearch.
 *
 * @author Mark Holster
 */
public abstract class AbstractElasticTelemetryEventPersister {

    /**
     * The <code>DateTimeFormatter</code> instance that is used to distinguish the indices per timestamp.
     */
    private static final DateTimeFormatter dateTimeFormatterIndexPerDay = DateUtils.getIndexPerDayFormatter();

    /**
     * An <code>IdGenerator</code> that will be used to create id's for events that don't have an id supplied.
     */
    protected static final IdGenerator idGenerator = new IdGenerator();

    /**
     * The cost of correlating an event.
     */
    protected static final double correlation_request_units = 0.25;

    /**
     * The <code>BulkProcessor</code> that sends events in bulk to Elasticsearch
     */
    protected BulkProcessor bulkProcessor;

    /**
     * The <code>RequestEnhancer</code> that will initialize the elasticsearch requests.
     */
    private final RequestEnhancer requestEnhancer;

    /**
     * The <code>LicenseThrottler</code> that potentially throttles the throughput of persisting the events.
     */
    final LicenseRateLimiter licenseRateLimiter;

    /**
     * Constructs a new <code>AbstractElasticTelemetryEventPersister</code> instance.
     *
     * @param bulkProcessor    The <code>BulkProcessor</code> that is capable of sending bulk events to elasticsearch.
     * @param etmConfiguration The <code>EtmConfiguration</code> used in the <code>RequestEnhancer</code> and <code>LicenseThrottler</code>.
     */
    AbstractElasticTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
        this.bulkProcessor = bulkProcessor;
        this.requestEnhancer = new RequestEnhancer(etmConfiguration);
        this.licenseRateLimiter = etmConfiguration.getLicenseRateLimiter();
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

    /**
     * Get the Elasticsearch index name for the current time.
     *
     * @return The Elasticsearch index name.
     */
    String getElasticIndexName() {
        return ElasticsearchLayout.EVENT_INDEX_PREFIX + dateTimeFormatterIndexPerDay.format(ZonedDateTime.now());
    }

    protected IndexRequestBuilder createIndexRequest(String id) {
        return this.requestEnhancer.enhance(new IndexRequestBuilder()
                .setIndex(getElasticIndexName())
                .setId(id)
        );
    }

    protected UpdateRequestBuilder createUpdateRequest(String id) {
        return this.requestEnhancer.enhance(new UpdateRequestBuilder()
                .setIndex(getElasticIndexName())
                .setId(id)
        );
    }

    protected void setCorrelationOnParent(TelemetryEvent<?> event) {
        if (event.correlationId == null || event.correlationId.equals(event.id)) {
            return;
        }
        var parameters = new HashMap<String, Object>();
        parameters.put("correlating_id", event.id);
        bulkProcessor.add(createUpdateRequest(event.correlationId)
                .setScript(new Script(ScriptType.STORED, null, "etm_update-event-with-correlation", parameters))
                .setUpsert("{}", XContentType.JSON)
                .setScriptedUpsert(true).build());
        licenseRateLimiter.addRequestUnits(correlation_request_units);
    }

}
