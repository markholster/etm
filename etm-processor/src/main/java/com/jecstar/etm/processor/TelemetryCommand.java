package com.jecstar.etm.processor;

import com.jecstar.etm.core.domain.BusinessTelemetryEvent;
import com.jecstar.etm.core.domain.HttpTelemetryEvent;
import com.jecstar.etm.core.domain.LogTelemetryEvent;
import com.jecstar.etm.core.domain.MessagingTelemetryEvent;
import com.jecstar.etm.core.domain.SqlTelemetryEvent;

public class TelemetryCommand {

	public enum CommandType {
		BUSINESS_EVENT("business"),
		HTTP_EVENT("http"), 
		LOG_EVENT("log"), 
		MESSAGING_EVENT("messaging"),
		SQL_EVENT("sql");
		
		private final String type;

		private CommandType(String type) {
			this.type = type;
		}
		
		public String toStringType() {
			return this.type;
		}
		
		public static CommandType valueOfStringType(String type) {
			if (BUSINESS_EVENT.toStringType().equalsIgnoreCase(type)) {
				return BUSINESS_EVENT;  
			} else if (HTTP_EVENT.toStringType().equalsIgnoreCase(type)) {
				return HTTP_EVENT;  
			} else if (LOG_EVENT.toStringType().equalsIgnoreCase(type)) {
				return LOG_EVENT;
			} else if (MESSAGING_EVENT.toStringType().equalsIgnoreCase(type)) {
				return MESSAGING_EVENT;
			} else if (SQL_EVENT.toStringType().equalsIgnoreCase(type)) {
				return SQL_EVENT;
			}
			throw new IllegalArgumentException("'" + type + "' is an invalid CommandType");
		}
	}

	public CommandType commandType;

	public BusinessTelemetryEvent businessTelemetryEvent = new BusinessTelemetryEvent();
	public HttpTelemetryEvent httpTelemetryEvent = new HttpTelemetryEvent();
	public LogTelemetryEvent logTelemetryEvent = new LogTelemetryEvent();
	public MessagingTelemetryEvent messagingTelemetryEvent = new MessagingTelemetryEvent();
	public SqlTelemetryEvent sqlTelemetryEvent = new SqlTelemetryEvent();

	public void initialize(BusinessTelemetryEvent businessTelemetryEvent) {
		this.commandType = CommandType.BUSINESS_EVENT;
		this.businessTelemetryEvent.initialize(businessTelemetryEvent);
		this.httpTelemetryEvent.initialize();
		this.logTelemetryEvent.initialize();
		this.messagingTelemetryEvent.initialize();
		this.sqlTelemetryEvent.initialize();
	}
	
	public void initialize(HttpTelemetryEvent httpTelemetryEvent) {
		this.commandType = CommandType.HTTP_EVENT;
		this.businessTelemetryEvent.initialize();
		this.httpTelemetryEvent.initialize(httpTelemetryEvent);
		this.logTelemetryEvent.initialize();
		this.messagingTelemetryEvent.initialize();
		this.sqlTelemetryEvent.initialize();
	}
	
	public void initialize(LogTelemetryEvent logTelemetryEvent) {
		this.commandType = CommandType.LOG_EVENT;
		this.businessTelemetryEvent.initialize();
		this.httpTelemetryEvent.initialize();
		this.logTelemetryEvent.initialize(logTelemetryEvent);
		this.messagingTelemetryEvent.initialize();
		this.sqlTelemetryEvent.initialize();
	}

	public void initialize(MessagingTelemetryEvent messagingTelemetryEvent) {
		this.commandType = CommandType.MESSAGING_EVENT;
		this.businessTelemetryEvent.initialize();
		this.httpTelemetryEvent.initialize();
		this.logTelemetryEvent.initialize();
		this.messagingTelemetryEvent.initialize(messagingTelemetryEvent);
		this.sqlTelemetryEvent.initialize();
	}
	
	public void initialize(SqlTelemetryEvent sqlTelemetryEvent) {
		this.commandType = CommandType.SQL_EVENT;
		this.businessTelemetryEvent.initialize();
		this.httpTelemetryEvent.initialize();
		this.logTelemetryEvent.initialize();
		this.messagingTelemetryEvent.initialize();
		this.sqlTelemetryEvent.initialize(sqlTelemetryEvent);
	}
}
