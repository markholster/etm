/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.processor.elastic.apm;

import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.handler.AbstractJsonHandler;
import com.jecstar.etm.server.core.Etm;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.zip.InflaterInputStream;

@Path("/")
public class ApmTelemetryEventProcessor extends AbstractJsonHandler {

    public ApmTelemetryEventProcessor() {
        super(null);
    }

    private static TelemetryCommandProcessor telemetryCommandProcessor;

    static void setProcessor(TelemetryCommandProcessor processor) {
        ApmTelemetryEventProcessor.telemetryCommandProcessor = processor;
    }

    @Override
    protected TelemetryCommandProcessor getProcessor() {
        return ApmTelemetryEventProcessor.telemetryCommandProcessor;
    }


    @POST
    @Path("/assets/v1/sourcemaps")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addApmAssets(InputStream data) {
        return printEvent(data, false);
    }

    @POST
    @Path("/intake/v2/events")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addApmEvents(InputStream data) {
        return printEvent(data, true);
    }

    @OPTIONS
    @Path("/intake/v2/rum/events")
    @Produces(MediaType.APPLICATION_JSON)
    public Response optionsApmRumEvents() {
        return Response.ok()
                .header(HttpHeaders.VARY, "Origin")
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Max-Age", "3600")
                .header("Access-Control-Allow-Methods",
                        HttpMethod.POST +
                                ", " + HttpMethod.OPTIONS)
                .header("Access-Control-Allow-Headers",
                        HttpHeaders.CONTENT_TYPE +
                                ", " + HttpHeaders.CONTENT_ENCODING +
                                ", " + HttpHeaders.ACCEPT)
                .header("Access-Control-Expose-Headers", "Etag")
                .build();
    }

    @POST
    @Path("/intake/v2/rum/events")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addApmRumEvents(InputStream data) {
        return printEvent(data, false);
    }

    private Response printEvent(InputStream data, boolean compressedStream) {
        JsonConverter jsonConverter = new JsonConverter();
        try (BufferedReader bufferedReader = new BufferedReader(compressedStream ? new InputStreamReader(new InflaterInputStream(data), StandardCharsets.UTF_8) : new InputStreamReader(data, StandardCharsets.UTF_8))) {
            bufferedReader
                    .lines()
                    .filter(str -> !str.isEmpty())
                    .forEach(c -> {
                        try {
                            var event = this.objectMapper.readValue(c, HashMap.class);
                            System.out.println(jsonConverter.toString(event));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
        return Response.ok().build();
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public Response apmHealthCheck() {
        System.out.println("System registered");
        return Response.ok("{\"name:\":\"Enterprise Telemetry Monitor APM Bridge\",\"version\":\"" + Etm.getVersion() + "\"}").build();
    }
}
