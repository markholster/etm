package com.jecstar.etm.processor;

import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.LogTelemetryEvent;
import com.jecstar.etm.domain.MessagingTelemetryEvent;
import com.jecstar.etm.domain.SqlTelemetryEvent;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;

public class TelemetryCommand {

	public enum CommandType {
		BUSINESS_EVENT(ElasticsearchLayout.ETM_EVENT_INDEX_TYPE_BUSINESS),
		HTTP_EVENT(ElasticsearchLayout.ETM_EVENT_INDEX_TYPE_HTTP), 
		LOG_EVENT(ElasticsearchLayout.ETM_EVENT_INDEX_TYPE_LOG), 
		MESSAGING_EVENT(ElasticsearchLayout.ETM_EVENT_INDEX_TYPE_MESSAGING),
		SQL_EVENT(ElasticsearchLayout.ETM_EVENT_INDEX_TYPE_SQL),
		NOOP(null);
		
		private final String type;

		CommandType(String type) {
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

	public final BusinessTelemetryEvent businessTelemetryEvent = new BusinessTelemetryEvent();
	public final HttpTelemetryEvent httpTelemetryEvent = new HttpTelemetryEvent();
	public final LogTelemetryEvent logTelemetryEvent = new LogTelemetryEvent();
	public final MessagingTelemetryEvent messagingTelemetryEvent = new MessagingTelemetryEvent();
	public final SqlTelemetryEvent sqlTelemetryEvent = new SqlTelemetryEvent();

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
	
	public void initializeToNoop() {
		this.commandType = CommandType.NOOP;
		this.businessTelemetryEvent.initialize();
		this.httpTelemetryEvent.initialize();
		this.logTelemetryEvent.initialize();
		this.messagingTelemetryEvent.initialize();
		this.sqlTelemetryEvent.initialize();		
	}
}
