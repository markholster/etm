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

import com.jecstar.etm.domain.MessagingTelemetryEvent;
import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.MessagingTelemetryEventConverterJsonImpl;
import com.jecstar.etm.server.core.persisting.TelemetryEventPersister;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

import java.util.HashMap;

public class MessagingTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
        implements TelemetryEventPersister<MessagingTelemetryEvent, MessagingTelemetryEventConverterJsonImpl> {

    public MessagingTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
        super(bulkProcessor, etmConfiguration);
    }

    @Override
    public void persist(MessagingTelemetryEvent event, MessagingTelemetryEventConverterJsonImpl converter) {
        if (event.id == null) {
            event.id = idGenerator.createId();
        }
        var indexRequestBuilder = createIndexRequest(event.id).setSource(converter.write(event), XContentType.JSON);
        var parameters = new HashMap<String, Object>();
        parameters.put("source", XContentHelper.convertToMap(new BytesArray(converter.write(event)), false, XContentType.JSON).v2());
        parameters.put("event_id", event.id);
        bulkProcessor.add(createUpdateRequest(event.id)
                .setScript(new Script(ScriptType.STORED, null, "etm_update-event", parameters))
                .setUpsert(indexRequestBuilder)
                .build()
        );
        licenseRateLimiter.addRequestUnits(indexRequestBuilder.calculateIndexRequestUnits()).throttle();

        // Set the correlation on the parent.
        if (MessagingEventType.RESPONSE.equals(event.messagingEventType) && event.correlationId != null) {
            bulkProcessor.add(createUpdateRequest(event.correlationId)
                    .setScript(new Script(ScriptType.STORED, null, "etm_update-request-with-response", parameters))
                    .setUpsert("{}", XContentType.JSON)
                    .setScriptedUpsert(true)
                    .build()
            );
            licenseRateLimiter.addRequestUnits(correlation_request_units);
        } else {
            setCorrelationOnParent(event);
        }

    }
}
