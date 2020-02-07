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
    protected void doWrite(HttpTelemetryEvent event, JsonBuilder builder) {
        builder.field(getTags().getExpiryTag(), event.expiry);
        if (event.httpEventType != null) {
            builder.field(getTags().getHttpEventTypeTag(), event.httpEventType.name());
        }
        builder.field(getTags().getStatusCodeTag(), event.statusCode);
    }

}
