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

package com.jecstar.etm.domain.writer.json;

import com.jecstar.etm.domain.LogTelemetryEvent;
import com.jecstar.etm.domain.writer.LogTelemetryEventWriter;
import com.jecstar.etm.domain.writer.TelemetryEventTags;

public class LogTelemetryEventWriterJsonImpl extends AbstractJsonTelemetryEventWriter<LogTelemetryEvent> implements LogTelemetryEventWriter<String> {

    @Override
    String getType() {
        return TelemetryEventTags.EVENT_OBJECT_TYPE_LOG;
    }

    @Override
    protected void doWrite(LogTelemetryEvent event, JsonBuilder builder) {
        builder.field(getTags().getLogLevelTag(), event.logLevel);
        builder.field(getTags().getStackTraceTag(), event.stackTrace);
    }

}
