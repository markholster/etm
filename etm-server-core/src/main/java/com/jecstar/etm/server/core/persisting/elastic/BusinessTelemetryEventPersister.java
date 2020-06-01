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

import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.BusinessTelemetryEventConverterJsonImpl;
import com.jecstar.etm.server.core.persisting.TelemetryEventPersister;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.common.xcontent.XContentType;

public class BusinessTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
        implements TelemetryEventPersister<BusinessTelemetryEvent, BusinessTelemetryEventConverterJsonImpl> {

    public BusinessTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
        super(bulkProcessor, etmConfiguration);
    }

    @Override
    public void persist(BusinessTelemetryEvent event, BusinessTelemetryEventConverterJsonImpl converter) {
        if (event.id == null) {
            event.id = idGenerator.createId();
        }
        var indexRequestBuilder = createIndexRequest(event.id).setSource(converter.write(event), XContentType.JSON);
        bulkProcessor.add(indexRequestBuilder.build());
        setCorrelationOnParent(event);
        licenseRateLimiter.addRequestUnits(indexRequestBuilder.calculateIndexRequestUnits()).throttle();
    }
}
