package com.jecstar.etm.processor;

import com.jecstar.etm.core.domain.DbQueryTelemetryEvent;
import com.jecstar.etm.core.domain.DbQueryTelemetryEventBuilder;
import com.jecstar.etm.core.domain.HttpTelemetryEvent;
import com.jecstar.etm.core.domain.HttpTelemetryEventBuilder;
import com.jecstar.etm.core.domain.LogTelemetryEvent;
import com.jecstar.etm.core.domain.LogTelemetryEventBuilder;
import com.jecstar.etm.core.domain.MessagingTelemetryEvent;
import com.jecstar.etm.core.domain.MessagingTelemetryEventBuilder;

public class TelemetryCommand {

	public enum CommandType {
		DB_QUERY_EVENT, 
		HTTP_EVENT, 
		LOG_EVENT, 
		MESSAGING_EVENT
	}

	public CommandType commandType;

	public DbQueryTelemetryEvent dbQueryTelemetryEvent = new DbQueryTelemetryEvent();
	public HttpTelemetryEvent httpTelemetryEvent = new HttpTelemetryEvent();
	public LogTelemetryEvent logTelemetryEvent = new LogTelemetryEvent();
	public MessagingTelemetryEvent messagingTelemetryEvent = new MessagingTelemetryEvent();
	
	public void initialize(DbQueryTelemetryEventBuilder dbQueryTelemetryEventBuilder) {
		this.commandType = CommandType.DB_QUERY_EVENT;
		this.dbQueryTelemetryEvent.initialize(dbQueryTelemetryEventBuilder.build());
		this.httpTelemetryEvent.initialize();
		this.logTelemetryEvent.initialize();
		this.messagingTelemetryEvent.initialize();
	}

	public void initialize(HttpTelemetryEventBuilder httpTelemetryEventBuilder) {
		this.commandType = CommandType.HTTP_EVENT;
		this.dbQueryTelemetryEvent.initialize();
		this.httpTelemetryEvent.initialize(httpTelemetryEventBuilder.build());
		this.logTelemetryEvent.initialize();
		this.messagingTelemetryEvent.initialize();
	}
	
	public void initialize(LogTelemetryEventBuilder logTelemetryEventBuilder) {
		this.commandType = CommandType.LOG_EVENT;
		this.dbQueryTelemetryEvent.initialize();
		this.httpTelemetryEvent.initialize();
		this.logTelemetryEvent.initialize(logTelemetryEventBuilder.build());
		this.messagingTelemetryEvent.initialize();
	}

	public void initialize(MessagingTelemetryEventBuilder messagingTelemetryEventBuilder) {
		this.commandType = CommandType.MESSAGING_EVENT;
		this.dbQueryTelemetryEvent.initialize();
		this.httpTelemetryEvent.initialize();
		this.logTelemetryEvent.initialize();
		this.messagingTelemetryEvent.initialize(messagingTelemetryEventBuilder.build());
	}


}
