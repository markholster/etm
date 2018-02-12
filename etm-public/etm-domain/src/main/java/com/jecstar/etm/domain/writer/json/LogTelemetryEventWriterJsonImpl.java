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
    protected boolean doWrite(LogTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
        boolean added = !firstElement;
        added = this.jsonWriter.addStringElementToJsonBuffer(getTags().getLogLevelTag(), event.logLevel, buffer, !added) || added;
        added = this.jsonWriter.addStringElementToJsonBuffer(getTags().getStackTraceTag(), event.stackTrace, buffer, !added) || added;
        return added;
    }

}
