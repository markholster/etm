package com.jecstar.etm.processor.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

@Path("/event")
public class RestTelemetryEventProcessor {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(RestTelemetryEventProcessor.class);

	private static TelemetryCommandProcessor telemetryCommandProcessor;

	private final ObjectMapper objectMapper = new ObjectMapper();
	
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
	
	public static void setProcessor(TelemetryCommandProcessor processor) {
		RestTelemetryEventProcessor.telemetryCommandProcessor = processor;
	}
	
	@POST
	@Path("/{eventType}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String addSpecificEvent(@PathParam("eventType") String eventType, InputStream data) {
		CommandType commandType = TelemetryCommand.CommandType.valueOfStringType(eventType);
		if (commandType == null) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		try (Reader reader = new InputStreamReader(data)) {
			final char[] buffer = new char[4096];
		    final StringBuilder out = new StringBuilder();
		    int bytesRead = 0;
		    while ((bytesRead = reader.read(buffer, 0, buffer.length)) != -1) {
		    	out.append(buffer, 0, bytesRead);
		    }
		    process(commandType, out);
			return "{ \"status\": \"acknowledged\" }";
		} catch (IOException e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Not processing rest message.", e);
			}
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@SuppressWarnings("unchecked")
	public String addEvent(InputStream data) {
		try {
			Map<String, Object> event = this.objectMapper.readValue(data, HashMap.class);
			String eventType = (String) event.get("type");
			CommandType commandType = TelemetryCommand.CommandType.valueOfStringType(eventType);
			if (commandType == null) {
				throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
			}
			Map<String, Object> eventData = (Map<String, Object>) event.get("data");
			process(commandType, eventData);
			return "{ \"status\": \"acknowledged\" }";
		} catch (IOException e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Not processing rest message.", e);
			}
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	
	@POST
	@Path("/_bulk")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@SuppressWarnings("unchecked")
	public String addEvents(InputStream data) {
		try {
			ArrayList<Map<String, Object>> events = this.objectMapper.readValue(data, ArrayList.class);
			for (Map<String, Object> event : events) {
				String eventType = (String) event.get("type");
				CommandType commandType = TelemetryCommand.CommandType.valueOfStringType(eventType);
				if (commandType == null) {
					//TODO add error in response?
					continue;
				}
				Map<String, Object> eventData = (Map<String, Object>) event.get("data");
				process(commandType, eventData);
			}
			return "{ \"status\": \"acknowledged\" }";
		} catch (IOException e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Not processing rest message.", e);
			}
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
	
	private void process(CommandType commandType, Map<String, Object> eventData) {
	    switch (commandType) {
	    // Initializing is done in the converters.
	    case BUSINESS_EVENT:
	    	this.businessConverter.read(eventData, this.businessTelemetryEvent);
	    	telemetryCommandProcessor.processTelemetryEvent(this.businessTelemetryEvent);
	    	break;
	    case HTTP_EVENT:
	    	this.httpConverter.read(eventData, this.httpTelemetryEvent);
	    	telemetryCommandProcessor.processTelemetryEvent(this.httpTelemetryEvent);
	    	break;
	    case LOG_EVENT:
	    	this.logConverter.read(eventData, this.logTelemetryEvent);
	    	telemetryCommandProcessor.processTelemetryEvent(this.logTelemetryEvent);
	    	break;
	    case MESSAGING_EVENT:
	    	this.messagingConverter.read(eventData, this.messagingTelemetryEvent);
	    	telemetryCommandProcessor.processTelemetryEvent(this.messagingTelemetryEvent);
	    	break;
	    case SQL_EVENT:
	    	this.sqlConverter.read(eventData, this.sqlTelemetryEvent);
	    	telemetryCommandProcessor.processTelemetryEvent(this.sqlTelemetryEvent);
	    	break;
	    }		
	}
	
	private void process(CommandType commandType, StringBuilder json) {
	    switch (commandType) {
	    // Initializing is done in the converters.
	    case BUSINESS_EVENT:
	    	this.businessConverter.read(json.toString(), this.businessTelemetryEvent);
	    	telemetryCommandProcessor.processTelemetryEvent(this.businessTelemetryEvent);
	    	break;
	    case HTTP_EVENT:
	    	this.httpConverter.read(json.toString(), this.httpTelemetryEvent);
	    	telemetryCommandProcessor.processTelemetryEvent(this.httpTelemetryEvent);
	    	break;
	    case LOG_EVENT:
	    	this.logConverter.read(json.toString(), this.logTelemetryEvent);
	    	telemetryCommandProcessor.processTelemetryEvent(this.logTelemetryEvent);
	    	break;
	    case MESSAGING_EVENT:
	    	this.messagingConverter.read(json.toString(), this.messagingTelemetryEvent);
	    	telemetryCommandProcessor.processTelemetryEvent(this.messagingTelemetryEvent);
	    	break;
	    case SQL_EVENT:
	    	this.sqlConverter.read(json.toString(), this.sqlTelemetryEvent);
	    	telemetryCommandProcessor.processTelemetryEvent(this.sqlTelemetryEvent);
	    	break;
	    }		
	}
}
