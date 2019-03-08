package com.jecstar.etm.processor.rest;

import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.handler.AbstractJsonHandler;
import com.jecstar.etm.processor.handler.HandlerResult;
import com.jecstar.etm.processor.handler.HandlerResults;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.stream.Collectors;

@Path("/event")
public class RestTelemetryEventProcessor extends AbstractJsonHandler {

    private static TelemetryCommandProcessor telemetryCommandProcessor;
//    private static final String APM_PREFIX = "/apm";

    static void setProcessor(TelemetryCommandProcessor processor) {
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
    public Response addEvent(InputStream data) {
        HandlerResults results = handleSingleEvent(data);
        return generateResponse(results);
    }


    @POST
    @Path("/_bulk")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addEvents(InputStream data) {
        HandlerResults results = handleBulkEvents(data);
        return generateResponse(results);
    }

    private Response generateResponse(HandlerResults handlerResults) {
        if (handlerResults.hasFailures()) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Failed to add event: " + handlerResults.getFailures().stream().map(HandlerResult::toString).collect(Collectors.joining(", ")));
            }
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return Response.accepted("{ \"status\": \"acknowledged\" }").build();
    }

//    @POST
//    @Path(APM_PREFIX + "/assets/v1/sourcemaps")
//    @Produces(MediaType.APPLICATION_JSON)
//    @SuppressWarnings("unchecked")
//    public Response addApmAssets(InputStream data) {
//        return printEvent(data);
//    }
//
//    @POST
//    @Path(APM_PREFIX + "/intake/v2/events")
//    @Produces(MediaType.APPLICATION_JSON)
//    @SuppressWarnings("unchecked")
//    public Response addApmEvents(InputStream data) {
//        return printEvent(data);
//    }
//
//    private Response printEvent(InputStream data) {
//        JsonConverter jsonConverter = new JsonConverter();
//        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new InflaterInputStream(data), StandardCharsets.UTF_8))) {
//            bufferedReader
//                    .lines()
//                    .filter(str -> !str.isEmpty())
//                    .forEach(c -> {
//                        try {
//                            Map<String, Object> event = this.objectMapper.readValue(c, HashMap.class);
//                            System.out.println(jsonConverter.toString(event));
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    });
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return Response.ok().build();
//    }
//
//    @GET
//    @Path(APM_PREFIX)
//    @Produces(MediaType.TEXT_PLAIN)
//    @SuppressWarnings("unchecked")
//    public Response apmHealthCheck() {
//        System.out.println("System registered");
//        return Response.ok("Enterprise Telemetry Monitor APM Bridge").build();
//    }

// Zie https://www.elastic.co/guide/en/apm/server/6.5/upgrading-to-65.html intake/v2/rum/events zou ook toegevoegd moeten worden als endpoint
}
