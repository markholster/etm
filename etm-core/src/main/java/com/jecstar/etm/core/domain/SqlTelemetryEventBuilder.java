package com.jecstar.etm.core.domain;

import java.time.ZonedDateTime;

import com.jecstar.etm.core.domain.SqlTelemetryEvent.SqlEventType;

public class SqlTelemetryEventBuilder extends TelemetryEventBuilder<SqlTelemetryEvent, SqlTelemetryEventBuilder> {

	public SqlTelemetryEventBuilder() {
		super(new SqlTelemetryEvent());
	}
	
	public SqlTelemetryEventBuilder setDbQueryEventType(SqlEventType sqlEventType) {
		this.event.sqlEventType = sqlEventType;
		return this;
	}
	
	public SqlTelemetryEventBuilder setReadingEndpointHandler(EndpointHandler writingEndpointHandler) {
		this.event.readingEndpointHandler = writingEndpointHandler;
		return this;
	}
	
	public SqlTelemetryEventBuilder setReadingEndpointHandler(ZonedDateTime handlingTime, String applicationName, String applicationVersion, String applicationInstance, String principal) {
		this.event.readingEndpointHandler.handlingTime = handlingTime;
		this.event.readingEndpointHandler.application.name = applicationName;
		this.event.readingEndpointHandler.application.version = applicationVersion;
		this.event.readingEndpointHandler.application.instance = applicationInstance;
		this.event.readingEndpointHandler.application.principal = principal;
		return this;
	}
}
