package com.jecstar.etm.processor.rest;

import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.handler.AbstractJsonHandler;
import com.jecstar.etm.processor.handler.HandlerResults;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

@Path("/event")
public class RestTelemetryEventProcessor extends AbstractJsonHandler {

	private static TelemetryCommandProcessor telemetryCommandProcessor;

	public static void setProcessor(TelemetryCommandProcessor processor) {
		RestTelemetryEventProcessor.telemetryCommandProcessor = processor;
	}

	@Override
	protected TelemetryCommandProcessor getProcessor() {
		return RestTelemetryEventProcessor.telemetryCommandProcessor;
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@SuppressWarnings("unchecked")
	public Response addEvent(InputStream data) {
        HandlerResults results = handleSingleEvent(data);
        if (results.hasFailures()) {
            // TODO uitlezen failure en loggen.
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return Response.accepted("{ \"status\": \"acknowledged\" }").build();
	}

	
	@POST
	@Path("/_bulk")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@SuppressWarnings("unchecked")
	public Response addEvents(InputStream data) {
        HandlerResults results = handleBulkEvents(data);
        if (results.hasFailures()) {
            // TODO uitlezen failures en loggen.
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return Response.accepted("{ \"status\": \"acknowledged\" }").build();
	}
}
