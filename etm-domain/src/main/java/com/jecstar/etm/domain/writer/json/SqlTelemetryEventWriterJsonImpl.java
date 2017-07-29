package com.jecstar.etm.domain.writer.json;

import com.jecstar.etm.domain.SqlTelemetryEvent;

public class SqlTelemetryEventWriterJsonImpl extends AbstractJsonTelemetryEventWriter<SqlTelemetryEvent>{

	@Override
	boolean doWrite(SqlTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
		boolean added = !firstElement;
		if (event.sqlEventType != null) {
			added = this.jsonWriter.addStringElementToJsonBuffer(getTags().getSqlEventTypeTag(), event.sqlEventType.name(), buffer, !added) || added;
		}
		return added;
	}

}
