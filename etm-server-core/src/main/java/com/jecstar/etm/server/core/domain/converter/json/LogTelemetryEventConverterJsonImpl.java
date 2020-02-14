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

import com.jecstar.etm.domain.LogTelemetryEvent;
import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.domain.writer.json.LogTelemetryEventWriterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.TelemetryEventConverter;

import java.util.Map;

public class LogTelemetryEventConverterJsonImpl extends LogTelemetryEventWriterJsonImpl implements TelemetryEventConverter<String, LogTelemetryEvent> {

    private final TelemetryEventJsonConverter<LogTelemetryEvent> converter = new TelemetryEventJsonConverter<>();

    @Override
    public String write(LogTelemetryEvent event, boolean includeId, boolean includePayloadEncoding) {
        return super.write(event, includeId, includePayloadEncoding);
    }

    @Override
    protected void doWrite(LogTelemetryEvent event, JsonBuilder builder) {
        this.converter.addDatabaseFields(event, builder);
        super.doWrite(event, builder);
    }

    @Override
    public LogTelemetryEvent read(String content, String id) {
        return read(this.converter.toMap(content), id);
    }

    @Override
    public void read(String content, LogTelemetryEvent event, String id) {
        read(this.converter.toMap(content), event, id);
    }

    @Override
    public LogTelemetryEvent read(Map<String, Object> valueMap, String id) {
        LogTelemetryEvent event = new LogTelemetryEvent();
        read(valueMap, event, id);
        return event;
    }

    @Override
    public void read(Map<String, Object> valueMap, LogTelemetryEvent event, String id) {
        this.converter.convert(valueMap, event, id);
        event.logLevel = this.converter.getString(getTags().getLogLevelTag(), valueMap);
        event.stackTrace = this.converter.getString(getTags().getStackTraceTag(), valueMap);
    }

}
