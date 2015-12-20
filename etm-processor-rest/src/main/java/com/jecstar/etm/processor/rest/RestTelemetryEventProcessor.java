package com.jecstar.etm.processor.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.jecstar.etm.core.domain.HttpTelemetryEvent;
import com.jecstar.etm.core.domain.LogTelemetryEvent;
import com.jecstar.etm.core.domain.MessagingTelemetryEvent;
import com.jecstar.etm.core.domain.SqlTelemetryEvent;
import com.jecstar.etm.core.domain.converter.json.HttpTelemetryEventConverterJsonImpl;
import com.jecstar.etm.core.domain.converter.json.LogTelemetryEventConverterJsonImpl;
import com.jecstar.etm.core.domain.converter.json.MessagingTelemetryEventConverterJsonImpl;
import com.jecstar.etm.core.domain.converter.json.SqlTelemetryEventConverterJsonImpl;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.TelemetryCommand.CommandType;
import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;

@Path("/event")
public class RestTelemetryEventProcessor {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(RestTelemetryEventProcessor.class);

	private static TelemetryCommandProcessor telemetryCommandProcessor;

	private final HttpTelemetryEventConverterJsonImpl httpConverter = new HttpTelemetryEventConverterJsonImpl();
	private final LogTelemetryEventConverterJsonImpl logConverter = new LogTelemetryEventConverterJsonImpl();
	private final MessagingTelemetryEventConverterJsonImpl messagingConverter = new MessagingTelemetryEventConverterJsonImpl();
	private final SqlTelemetryEventConverterJsonImpl sqlConverter = new SqlTelemetryEventConverterJsonImpl();

	private final HttpTelemetryEvent httpTelemetryEvent = new HttpTelemetryEvent(); 
	private final LogTelemetryEvent logTelemetryEvent = new LogTelemetryEvent(); 
	private final MessagingTelemetryEvent messagingTelemetryEvent = new MessagingTelemetryEvent(); 
	private final SqlTelemetryEvent sqlTelemetryEvent = new SqlTelemetryEvent(); 
	
	public static void setProcessor(TelemetryCommandProcessor processor) {
		telemetryCommandProcessor = processor;
	}
	
	@POST
	@Path("/${eventType}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String addEvent(@PathParam("eventType") String eventType, InputStream data) {
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
		    switch (commandType) {
		    // Initializing is done in the converters.
		    case HTTP_EVENT:
		    	this.httpConverter.convert(out.toString(), this.httpTelemetryEvent);
		    	telemetryCommandProcessor.processHttpTelemetryEvent(this.httpTelemetryEvent);
		    case LOG_EVENT:
		    	this.logConverter.convert(out.toString(), this.logTelemetryEvent);
		    	telemetryCommandProcessor.processLogTelemetryEvent(this.logTelemetryEvent);
		    case MESSAGING_EVENT:
		    	this.messagingConverter.convert(out.toString(), this.messagingTelemetryEvent);
		    	telemetryCommandProcessor.processMessagingTelemetryEvent(this.messagingTelemetryEvent);
		    case SQL_EVENT:
		    	this.sqlConverter.convert(out.toString(), this.sqlTelemetryEvent);
		    	telemetryCommandProcessor.processSqlTelemetryEvent(this.sqlTelemetryEvent);
		    }
			return "{ \"status\": \"acknowledged\" }";
		} catch (IOException e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Not processing rest message.", e);
			}
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
}
