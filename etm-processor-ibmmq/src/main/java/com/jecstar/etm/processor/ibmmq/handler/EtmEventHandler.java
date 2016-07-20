package com.jecstar.etm.processor.ibmmq.handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.LogTelemetryEvent;
import com.jecstar.etm.domain.MessagingTelemetryEvent;
import com.jecstar.etm.domain.SqlTelemetryEvent;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.TelemetryCommand.CommandType;
import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;
import com.jecstar.etm.server.core.domain.converter.json.BusinessTelemetryEventConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.json.HttpTelemetryEventConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.json.LogTelemetryEventConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.json.MessagingTelemetryEventConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.json.SqlTelemetryEventConverterJsonImpl;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

public class EtmEventHandler {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(EtmEventHandler.class);

	private final TelemetryCommandProcessor telemetryCommandProcessor; 	
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final StringBuilder byteArrayBuilder = new StringBuilder();
	
	private final BusinessTelemetryEventConverterJsonImpl businessConverter = new BusinessTelemetryEventConverterJsonImpl();
	private final HttpTelemetryEventConverterJsonImpl httpConverter = new HttpTelemetryEventConverterJsonImpl();
	private final LogTelemetryEventConverterJsonImpl logConverter = new LogTelemetryEventConverterJsonImpl();
	private final MessagingTelemetryEventConverterJsonImpl messagingConverter = new MessagingTelemetryEventConverterJsonImpl();
	private final SqlTelemetryEventConverterJsonImpl sqlConverter = new SqlTelemetryEventConverterJsonImpl();

	private final BusinessTelemetryEvent businessTelemetryEvent = new BusinessTelemetryEvent(); 
	private final HttpTelemetryEvent httpTelemetryEvent = new HttpTelemetryEvent(); 
	private final LogTelemetryEvent logTelemetryEvent = new LogTelemetryEvent(); 
	private final MessagingTelemetryEvent messagingTelemetryEvent = new MessagingTelemetryEvent(); 
	private final SqlTelemetryEvent sqlTelemetryEvent = new SqlTelemetryEvent();


	public EtmEventHandler(TelemetryCommandProcessor telemetryCommandProcessor) {
		this.telemetryCommandProcessor = telemetryCommandProcessor;
	}

	@SuppressWarnings("unchecked")
	public HandlerResult handleMessage(byte[] messageId, byte[] messageContent) {
		try {
			Map<String, Object> event = this.objectMapper.readValue(messageContent, HashMap.class);
			String eventType = (String) event.get("type");
			CommandType commandType = TelemetryCommand.CommandType.valueOfStringType(eventType);
			if (commandType == null) {
				return HandlerResult.FAILED;
			}
			Map<String, Object> eventData = (Map<String, Object>) event.get("data");
			process(commandType, eventData);
		} catch (IOException e) {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Message with id '" + byteArrayToString(messageId) + "' seems to have an invalid format", e);
			}
			return HandlerResult.PARSE_FAILURE;
		}
		return HandlerResult.PROCESSED;
	}
	
	
	private void process(CommandType commandType, Map<String, Object> eventData) {
	    switch (commandType) {
	    // Initializing is done in the converters.
	    case BUSINESS_EVENT:
	    	this.businessConverter.read(eventData, this.businessTelemetryEvent);
	    	this.telemetryCommandProcessor.processTelemetryEvent(this.businessTelemetryEvent);
	    	break;
	    case HTTP_EVENT:
	    	this.httpConverter.read(eventData, this.httpTelemetryEvent);
	    	this.telemetryCommandProcessor.processTelemetryEvent(this.httpTelemetryEvent);
	    	break;
	    case LOG_EVENT:
	    	this.logConverter.read(eventData, this.logTelemetryEvent);
	    	this.telemetryCommandProcessor.processTelemetryEvent(this.logTelemetryEvent);
	    	break;
	    case MESSAGING_EVENT:
	    	this.messagingConverter.read(eventData, this.messagingTelemetryEvent);
	    	this.telemetryCommandProcessor.processTelemetryEvent(this.messagingTelemetryEvent);
	    	break;
	    case SQL_EVENT:
	    	this.sqlConverter.read(eventData, this.sqlTelemetryEvent);
	    	this.telemetryCommandProcessor.processTelemetryEvent(this.sqlTelemetryEvent);
	    	break;
	    case NOOP:
	    	break;
	    }		
	}
	
	private String byteArrayToString(byte[] bytes) {
		this.byteArrayBuilder.setLength(0);
		boolean allZero = true;
		for (int i = 0; i < bytes.length; i++) {
			this.byteArrayBuilder.append(String.format("%02x", bytes[i]));
			if (bytes[i] != 0) {
				allZero = false;
			}
		}
		return allZero ? null : this.byteArrayBuilder.toString();
	}
}
