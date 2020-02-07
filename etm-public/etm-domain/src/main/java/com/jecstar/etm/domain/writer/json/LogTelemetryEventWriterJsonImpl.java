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
