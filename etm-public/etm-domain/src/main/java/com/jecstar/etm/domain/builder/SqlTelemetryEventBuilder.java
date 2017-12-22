package com.jecstar.etm.domain.builder;

import com.jecstar.etm.domain.SqlTelemetryEvent;
import com.jecstar.etm.domain.SqlTelemetryEvent.SqlEventType;

public class SqlTelemetryEventBuilder extends TelemetryEventBuilder<SqlTelemetryEvent, SqlTelemetryEventBuilder> {

	public SqlTelemetryEventBuilder() {
		super(new SqlTelemetryEvent());
	}
	
	public SqlTelemetryEventBuilder setDbQueryEventType(SqlEventType sqlEventType) {
		this.event.sqlEventType = sqlEventType;
		return this;
	}
	
}
