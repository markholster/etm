package com.jecstar.etm.core.domain;

import java.time.ZonedDateTime;

import com.jecstar.etm.core.domain.DbQueryTelemetryEvent.DbQueryEventType;

public class DbQueryTelemetryEventBuilder extends TelemetryEventBuilder<DbQueryTelemetryEvent, DbQueryTelemetryEventBuilder> {

	public DbQueryTelemetryEventBuilder(DbQueryTelemetryEvent event) {
		super(event);
	}
	
	public DbQueryTelemetryEventBuilder setDbQueryEventType(DbQueryEventType dbQueryEventType) {
		this.event.dbQueryEventType = dbQueryEventType;
		return this;
	}
	
	public DbQueryTelemetryEventBuilder setReadingEndpointHandler(EndpointHandler writingEndpointHandler) {
		this.event.readingEndpointHandler = writingEndpointHandler;
		return this;
	}
	
	public DbQueryTelemetryEventBuilder setReadingEndpointHandler(ZonedDateTime handlingTime, String applicationName, String applicationVersion, String applicationInstance, String principal) {
		this.event.readingEndpointHandler.handlingTime = handlingTime;
		this.event.readingEndpointHandler.application.name = applicationName;
		this.event.readingEndpointHandler.application.version = applicationVersion;
		this.event.readingEndpointHandler.application.instance = applicationInstance;
		this.event.readingEndpointHandler.application.principal = principal;
		return this;
	}

}
