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

package com.jecstar.etm.server.core.domain.converter.json;

import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.domain.writer.json.BusinessTelemetryEventWriterJsonImpl;
import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.server.core.domain.converter.TelemetryEventConverter;

import java.util.Map;

public class BusinessTelemetryEventConverterJsonImpl extends BusinessTelemetryEventWriterJsonImpl implements TelemetryEventConverter<String, BusinessTelemetryEvent> {

    private final TelemetryEventJsonConverter<BusinessTelemetryEvent> converter = new TelemetryEventJsonConverter<>();

    @Override
    protected void doWrite(BusinessTelemetryEvent event, JsonBuilder builder) {
        this.converter.addDatabaseFields(event, builder);
        super.doWrite(event, builder);
    }

    @Override
    public BusinessTelemetryEvent read(String content, String id) {
        return read(this.converter.toMap(content), id);
    }

    @Override
    public void read(String content, BusinessTelemetryEvent event, String id) {
        read(this.converter.toMap(content), event, id);
    }

    @Override
    public BusinessTelemetryEvent read(Map<String, Object> valueMap, String id) {
        BusinessTelemetryEvent event = new BusinessTelemetryEvent();
        read(valueMap, event, id);
        return event;
    }

    @Override
    public void read(Map<String, Object> valueMap, BusinessTelemetryEvent event, String id) {
        this.converter.convert(valueMap, event, id);
    }

}
