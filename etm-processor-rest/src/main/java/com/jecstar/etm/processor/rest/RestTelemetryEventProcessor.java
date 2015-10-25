package com.jecstar.etm.processor.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.jecstar.etm.core.domain.converter.TelemetryEventConverter;
import com.jecstar.etm.core.domain.converter.json.TelemetryEventConverterJsonImpl;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.TelemetryCommand.CommandType;
import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;

@Path("/rest/event")
public class RestTelemetryEventProcessor {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(RestTelemetryEventProcessor.class);

	private static TelemetryCommandProcessor telemetryCommandProcessor;

	private final TelemetryCommand command = new TelemetryCommand();
	
	private final TelemetryEventConverter<String> telemetryEventConverter = new TelemetryEventConverterJsonImpl();

	public static void setProcessor(TelemetryCommandProcessor processor) {
		telemetryCommandProcessor = processor;
		
	}

	
	@POST
	@Path("/add")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String addEvent(InputStream data) {
		try (Reader reader = new InputStreamReader(data)) {
			final char[] buffer = new char[4096];
		    final StringBuilder out = new StringBuilder();
		    int bytesRead = 0;
		    while ((bytesRead = reader.read(buffer, 0, buffer.length)) != -1) {
		    	out.append(buffer, 0, bytesRead);
		    }
		    this.command.initialize();
		    this.command.commandType = CommandType.EVENT;
		    this.telemetryEventConverter.convert(out.toString(), command.event);
		    telemetryCommandProcessor.processTelemetryCommand(command);
			return "{ \"status\": \"success\" }";
		} catch (IOException e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Not processing rest message.", e);
			}
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
}
