package com.jecstar.etm.domain.writer.json;

import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.writer.HttpTelemetryEventWriter;
import com.jecstar.etm.domain.writer.TelemetryEventTags;

public class HttpTelemetryEventWriterJsonImpl extends AbstractJsonTelemetryEventWriter<HttpTelemetryEvent> implements HttpTelemetryEventWriter<String> {

    @Override
    String getType() {
        return TelemetryEventTags.EVENT_OBJECT_TYPE_HTTP;
    }

    @Override
    protected boolean doWrite(HttpTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
        boolean added = !firstElement;
        if (event.expiry != null) {
            added = this.jsonWriter.addLongElementToJsonBuffer(getTags().getExpiryTag(), event.expiry.toInstant().toEpochMilli(), buffer, !added) || added;
        }
        if (event.httpEventType != null) {
            added = this.jsonWriter.addStringElementToJsonBuffer(getTags().getHttpEventTypeTag(), event.httpEventType.name(), buffer, !added) || added;
        }
        return added;
    }

}
