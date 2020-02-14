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

import com.jecstar.etm.domain.MessagingTelemetryEvent;
import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.domain.writer.json.MessagingTelemetryEventWriterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.TelemetryEventConverter;

import java.util.Map;

public class MessagingTelemetryEventConverterJsonImpl extends MessagingTelemetryEventWriterJsonImpl implements TelemetryEventConverter<String, MessagingTelemetryEvent> {

    private final TelemetryEventJsonConverter<MessagingTelemetryEvent> converter = new TelemetryEventJsonConverter<>();

    @Override
    public String write(MessagingTelemetryEvent event, boolean includeId, boolean includePayloadEncoding) {
        return super.write(event, includeId, includePayloadEncoding);
    }

    @Override
    protected void doWrite(MessagingTelemetryEvent event, JsonBuilder builder) {
        this.converter.addDatabaseFields(event, builder);
        super.doWrite(event, builder);
    }

    @Override
    public MessagingTelemetryEvent read(String content, String id) {
        return read(this.converter.toMap(content), id);
    }

    @Override
    public void read(String content, MessagingTelemetryEvent event, String id) {
        read(this.converter.toMap(content), event, id);
    }

    @Override
    public MessagingTelemetryEvent read(Map<String, Object> valueMap, String id) {
        MessagingTelemetryEvent event = new MessagingTelemetryEvent();
        read(valueMap, event, id);
        return event;
    }

    @Override
    public void read(Map<String, Object> valueMap, MessagingTelemetryEvent event, String id) {
        this.converter.convert(valueMap, event, id);
        event.expiry = this.converter.getInstant(getTags().getExpiryTag(), valueMap);
        event.messagingEventType = MessagingEventType.safeValueOf(this.converter.getString(getTags().getMessagingEventTypeTag(), valueMap));
    }

}
