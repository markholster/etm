package com.jecstar.etm.core.domain;

import com.jecstar.etm.core.domain.SqlTelemetryEvent.SqlEventType;

public class SqlTelemetryEventBuilder extends TelemetryEventBuilder<SqlTelemetryEvent, SqlTelemetryEventBuilder> {

	public SqlTelemetryEventBuilder() {
		super(new SqlTelemetryEvent());
	}
	
	public SqlTelemetryEventBuilder setDbQueryEventType(SqlEventType sqlEventType) {
		this.event.sqlEventType = sqlEventType;
		return this;
	}
	
}
