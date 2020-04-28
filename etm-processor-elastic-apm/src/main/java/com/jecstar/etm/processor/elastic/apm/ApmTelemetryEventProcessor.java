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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jecstar.etm.domain.Application;
import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.SqlTelemetryEvent;
import com.jecstar.etm.domain.builder.*;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.elastic.apm.configuration.ElasticApm;
import com.jecstar.etm.processor.elastic.apm.domain.Metadata;
import com.jecstar.etm.processor.elastic.apm.domain.converter.MetadataConverter;
import com.jecstar.etm.processor.elastic.apm.domain.converter.errors.ErrorConverter;
import com.jecstar.etm.processor.elastic.apm.domain.converter.spans.SpanConverter;
import com.jecstar.etm.processor.elastic.apm.domain.converter.transactions.TransactionConverter;
import com.jecstar.etm.processor.elastic.apm.domain.spans.Span;
import com.jecstar.etm.processor.elastic.apm.domain.transactions.Transaction;
import com.jecstar.etm.server.core.Etm;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.InflaterInputStream;

@Path("/")
public class ApmTelemetryEventProcessor {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(ApmTelemetryEventProcessor.class);
    private static TelemetryCommandProcessor telemetryCommandProcessor;
    private static ElasticApm elasticApm;
    private static final Set<String> allowedHeaders = new HashSet<>();
    private static final String RESPONSE_ID_SUFFIX = "-rsp";
    private static final String RESPONSE_NAME_SUFFIX = " - Response";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MetadataConverter metadataConverter = new MetadataConverter();
    private final TransactionConverter transactionConverter = new TransactionConverter();
    private final ErrorConverter errorConverter = new ErrorConverter();
    private final SpanConverter spanConverter = new SpanConverter();

    static void initialize(TelemetryCommandProcessor processor, ElasticApm elasticApm) {
        ApmTelemetryEventProcessor.telemetryCommandProcessor = processor;
        ApmTelemetryEventProcessor.elasticApm = elasticApm;
        allowedHeaders.add(HttpHeaders.ACCEPT);
        allowedHeaders.add(HttpHeaders.CONTENT_ENCODING);
        allowedHeaders.add(HttpHeaders.CONTENT_TYPE);
        if (elasticApm.allowedHeaders != null) {
            allowedHeaders.addAll(elasticApm.allowedHeaders);
        }
    }

    @POST
    @Path("/assets/v1/sourcemaps")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addApmAssets(InputStream data) {
        return handleEvent(data, false);
    }

    @POST
    @Path("/intake/v2/events")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addApmEvents(InputStream data) {
        return handleEvent(data, true);
    }

    @OPTIONS
    @Path("/intake/v2/rum/events")
    @Produces(MediaType.APPLICATION_JSON)
    public Response optionsApmRumEvents(@Context HttpHeaders headers) {
        List<String> origins = headers.getRequestHeader("Origin");
        var response = Response.ok()
                .header(HttpHeaders.VARY, "Origin")
                .header("Access-Control-Max-Age", "3600")
                .header("Access-Control-Allow-Methods",
                        HttpMethod.POST +
                                ", " + HttpMethod.OPTIONS)
                .header("Access-Control-Allow-Headers",
                        String.join(", ", ApmTelemetryEventProcessor.allowedHeaders))
                .header("Access-Control-Expose-Headers", "Etag");
        if (origins != null && origins.size() > 0 && elasticApm.allowedOrigins != null) {
            var origin = origins.get(0);
            if (elasticApm.allowedOrigins.contains(origin)) {
                response.header("Access-Control-Allow-Origin", origin);
            }
        }
        return response.build();
    }

    @POST
    @Path("/intake/v2/rum/events")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addApmRumEvents(InputStream data) {
        return handleEvent(data, false);
    }


    @GET
    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public Response apmHealthCheck() {
        System.out.println("System registered");
        return Response.ok("{\"name:\":\"Enterprise Telemetry Monitor APM Bridge\",\"version\":\"" + Etm.getVersion() + "\"}").build();
    }

    @SuppressWarnings("unchecked")
    private Response handleEvent(InputStream data, boolean compressedStream) {
        JsonConverter jsonConverter = new JsonConverter();
        try (BufferedReader bufferedReader = new BufferedReader(compressedStream ? new InputStreamReader(new InflaterInputStream(data), StandardCharsets.UTF_8) : new InputStreamReader(data, StandardCharsets.UTF_8))) {
            var lines = bufferedReader
                    .lines()
                    .filter(str -> !str.isEmpty())
                    .collect(Collectors.toList());
            Application application = null;
            if (lines.size() >= 1) {
                var event = this.objectMapper.readValue(lines.get(0), HashMap.class);

                Metadata metadata = this.metadataConverter.read((Map<String, Object>) event.get("metadata"));
                var applicationBuilder = new ApplicationBuilder();
                if (metadata.getService() != null) {
                    var service = metadata.getService();
                    applicationBuilder.setName(service.getName());
                    applicationBuilder.setVersion(service.getVersion());
                    if (service.getNode() != null) {
                        applicationBuilder.setInstance(service.getNode().getConfiguredName());
                    }
                }
                if (metadata.getSystem() != null) {
                    var system = metadata.getSystem();
                    if (system.getHostname() != null) {
                        applicationBuilder.setHostAddress(InetAddress.getByName(system.getHostname()));
                    } else if (system.getConfiguredHostname() != null) {
                        applicationBuilder.setHostAddress(InetAddress.getByName(system.getConfiguredHostname()));
                    } else if (system.getDetectedHostname() != null) {
                        applicationBuilder.setHostAddress(InetAddress.getByName(system.getDetectedHostname()));
                    }
                }
                if (metadata.getUser() != null) {
                    applicationBuilder.setPrincipal(metadata.getUser().getId());
                }
                application = applicationBuilder.build();
            }
            System.out.println("=== " + application.name + " ===");
            for (int i = 1; i < lines.size(); i++) {
                var event = this.objectMapper.readValue(lines.get(i), HashMap.class);
                if (event.containsKey("metricset")) {
                    // Skip metricsets for now..
                    continue;
                } else if (event.containsKey("transaction")) {
                    var transaction = this.transactionConverter.read((Map<String, Object>) event.get("transaction"));
                    handleTransaction(transaction, application);
                } else if (event.containsKey("span")) {
                    var span = this.spanConverter.read((Map<String, Object>) event.get("span"));
                    handleSpan(span, application);
                } else if (event.containsKey("error")) {
                    var error = this.errorConverter.read((Map<String, Object>) event.get("error"));
                }
                System.out.println(jsonConverter.toString(event));
            }
        } catch (IOException e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage(e.getMessage(), e);
            }
        }
        return Response.ok().build();
    }

    private void handleTransaction(Transaction transaction, Application application) {
        if ("request".equals(transaction.getType())) {
            // ALS transaction.type == request && transaction.parent_id != null => Request ontvangen, transaction.parent_id == span.id == requestId in ETM. Endpoints zit in context.request.url.full

            // Incoming request.
            var httpBuilder = new HttpTelemetryEventBuilder();
            httpBuilder.setHttpEventType(HttpTelemetryEvent.HttpEventType.safeValueOf(transaction.getContext().getRequest().getMethod()));
            if (transaction.getParentId() != null) {
                httpBuilder.setId(transaction.getParentId());
            } else {
                httpBuilder.setId(transaction.getId());
            }
            httpBuilder.setName(transaction.getName());
            httpBuilder.setTraceId(transaction.getTraceId());
            httpBuilder.setPayload(transaction.getContext().getRequest().getBody());
            var endpointBuilder = new EndpointBuilder();
            endpointBuilder.setName(transaction.getContext().getRequest().getUrl().getFull());

            var endpointHandlerBuilder = new EndpointHandlerBuilder();
            endpointHandlerBuilder.setType(EndpointHandler.EndpointHandlerType.READER);
            endpointHandlerBuilder.setHandlingTime(Instant.EPOCH.plus(transaction.getTimestamp(), ChronoUnit.MICROS));
            endpointHandlerBuilder.setTransactionId(transaction.getId());
            endpointHandlerBuilder.setApplication(application);
            if (transaction.getContext().getRequest().getHeaders() != null) {
                for (var entry : transaction.getContext().getRequest().getHeaders().entrySet()) {
                    endpointHandlerBuilder.addMetadata("http_" + entry.getKey(), entry.getValue());
                }
            }
            endpointBuilder.addEndpointHandler(endpointHandlerBuilder);
            httpBuilder.addOrMergeEndpoint(endpointBuilder);

            telemetryCommandProcessor.processTelemetryEvent(httpBuilder, null);

            var id = httpBuilder.getId();
            httpBuilder.initialize();
            httpBuilder.setId(id + RESPONSE_ID_SUFFIX);
            httpBuilder.setCorrelationId(id);
            httpBuilder.setHttpEventType(HttpTelemetryEvent.HttpEventType.RESPONSE);
            httpBuilder.setName(transaction.getName() + RESPONSE_NAME_SUFFIX);
            httpBuilder.setTraceId(transaction.getTraceId());
            endpointBuilder = new EndpointBuilder();
            endpointBuilder.setName(transaction.getContext().getRequest().getUrl().getFull());

            endpointHandlerBuilder = new EndpointHandlerBuilder();
            endpointHandlerBuilder.setType(EndpointHandler.EndpointHandlerType.WRITER);
            endpointHandlerBuilder.setHandlingTime(Instant.EPOCH.plus(transaction.getTimestamp(), ChronoUnit.MICROS).plus(transaction.getDuration(), ChronoUnit.MILLIS));
            endpointHandlerBuilder.setTransactionId(transaction.getId());
            endpointHandlerBuilder.setApplication(application);
            if (transaction.getContext().getResponse().getHeaders() != null) {
                for (var entry : transaction.getContext().getResponse().getHeaders().entrySet()) {
                    endpointHandlerBuilder.addMetadata("http_" + entry.getKey(), entry.getValue());
                }
            }
            endpointBuilder.addEndpointHandler(endpointHandlerBuilder);
            httpBuilder.addOrMergeEndpoint(endpointBuilder);

            telemetryCommandProcessor.processTelemetryEvent(httpBuilder, null);
        } else if ("messaging".equals(transaction.getType())) {
            // JMS transaction
        }

    }

    private void handleSpan(Span span, Application application) {
        if (span.getContext() == null) {
            return;
        }
        if (span.getContext().getDb() != null) {
            var db = span.getContext().getDb();
            var sqlBuilder = new SqlTelemetryEventBuilder();
            sqlBuilder.setId(span.getId());
            sqlBuilder.setTraceId(span.getTraceId());
            sqlBuilder.setDbQueryEventType(SqlTelemetryEvent.SqlEventType.safeValueOf(db.getStatement().substring(0, db.getStatement().indexOf(" "))));
            sqlBuilder.setName(span.getName());
            sqlBuilder.setPayload(db.getStatement());

            var endpointBuilder = new EndpointBuilder();
            endpointBuilder.setName(span.getContext().getDestination().getAddress());

            var endpointHandlerBuilder = new EndpointHandlerBuilder();
            endpointHandlerBuilder.setType(EndpointHandler.EndpointHandlerType.WRITER);
            endpointHandlerBuilder.setHandlingTime(Instant.EPOCH.plus(span.getTimestamp(), ChronoUnit.MICROS));
            endpointHandlerBuilder.setTransactionId(span.getTransactionId());
            endpointHandlerBuilder.setApplication(application);

            endpointBuilder.addEndpointHandler(endpointHandlerBuilder);
            sqlBuilder.addOrMergeEndpoint(endpointBuilder);

            telemetryCommandProcessor.processTelemetryEvent(sqlBuilder, null);

            var id = sqlBuilder.getId();
            sqlBuilder.initialize();
            sqlBuilder.setId(id + RESPONSE_ID_SUFFIX);
            sqlBuilder.setCorrelationId(id);
            sqlBuilder.setDbQueryEventType(SqlTelemetryEvent.SqlEventType.RESULTSET);
            sqlBuilder.setName(span.getName() + RESPONSE_NAME_SUFFIX);
            sqlBuilder.setTraceId(span.getTraceId());

            endpointBuilder = new EndpointBuilder();
            endpointBuilder.setName(span.getContext().getDestination().getAddress());

            endpointHandlerBuilder = new EndpointHandlerBuilder();
            endpointHandlerBuilder.setType(EndpointHandler.EndpointHandlerType.READER);
            endpointHandlerBuilder.setHandlingTime(Instant.EPOCH.plus(span.getTimestamp(), ChronoUnit.MICROS).plus(span.getDuration(), ChronoUnit.MILLIS));
            endpointHandlerBuilder.setTransactionId(span.getTransactionId());
            endpointHandlerBuilder.setApplication(application);

            endpointBuilder.addEndpointHandler(endpointHandlerBuilder);
            sqlBuilder.addOrMergeEndpoint(endpointBuilder);

            telemetryCommandProcessor.processTelemetryEvent(sqlBuilder, null);
        }
    }
}
