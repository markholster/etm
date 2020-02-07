package com.jecstar.etm.domain.writer.json;

import com.jecstar.etm.domain.SqlTelemetryEvent;
import com.jecstar.etm.domain.writer.SqlTelemetryEventWriter;
import com.jecstar.etm.domain.writer.TelemetryEventTags;

public class SqlTelemetryEventWriterJsonImpl extends AbstractJsonTelemetryEventWriter<SqlTelemetryEvent> implements SqlTelemetryEventWriter<String> {

    @Override
    String getType() {
        return TelemetryEventTags.EVENT_OBJECT_TYPE_SQL;
    }

    @Override
    protected void doWrite(SqlTelemetryEvent event, JsonBuilder builder) {
        if (event.sqlEventType != null) {
            builder.field(getTags().getSqlEventTypeTag(), event.sqlEventType.name());
        }
    }

}
