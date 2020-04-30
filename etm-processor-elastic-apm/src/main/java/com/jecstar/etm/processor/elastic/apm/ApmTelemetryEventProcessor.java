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
import com.jecstar.etm.domain.*;
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
        return handleEvent(data, false).build();
    }

    @POST
    @Path("/intake/v2/events")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addApmEvents(InputStream data) {
        return handleEvent(data, true).build();
    }

    @OPTIONS
    @Path("/intake/v2/rum/events")
    @Produces(MediaType.APPLICATION_JSON)
    public Response optionsApmRumEvents(@Context HttpHeaders headers) {
        var response = Response.ok()
                .header(HttpHeaders.VARY, "Origin")
                .header("Access-Control-Max-Age", "3600")
                .header("Access-Control-Allow-Methods",
                        HttpMethod.POST +
                                ", " + HttpMethod.OPTIONS)
                .header("Access-Control-Allow-Headers",
                        String.join(", ", ApmTelemetryEventProcessor.allowedHeaders))
                .header("Access-Control-Expose-Headers", "Etag");
        addOriginHeader(headers.getRequestHeader("Origin"), response);
        return response.build();
    }

    @POST
    @Path("/intake/v2/rum/events")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addApmRumEvents(@Context HttpHeaders headers, InputStream data) {
        var response = handleEvent(data, false);
        addOriginHeader(headers.getRequestHeader("Origin"), response);
        return response.build();
    }

    private void addOriginHeader(List<String> origins, Response.ResponseBuilder response) {
        if (origins != null && origins.size() > 0 && elasticApm.allowedOrigins != null) {
            var origin = origins.get(0);
            if (elasticApm.allowedOrigins.contains(origin)) {
                response.header("Access-Control-Allow-Origin", origin);
            }
        }
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public Response apmHealthCheck() {
        return Response.ok("{\"name:\":\"Enterprise Telemetry Monitor APM Bridge\",\"version\":\"" + Etm.getVersion() + "\"}").build();
    }

    @SuppressWarnings("unchecked")
    private Response.ResponseBuilder handleEvent(InputStream data, boolean compressedStream) {
        var now = Instant.now();
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
                System.out.println("=== " + application.name + " ===");
            }
            for (int i = 1; i < lines.size(); i++) {
                var line = lines.get(i);
                if (line.startsWith("{\"span\":")) {
                    // work around for https://github.com/elastic/apm-agent-rum-js/pull/753
                    line = line.replace("\"subType\":", "\"subtype\":");
                }
                var event = this.objectMapper.readValue(line, HashMap.class);
                if (event.containsKey("metricset")) {
                    // Skip metricsets for now..
                    continue;
                } else if (event.containsKey("transaction")) {
                    var transaction = this.transactionConverter.read((Map<String, Object>) event.get("transaction"));
                    handleTransaction(transaction, application, now);
                } else if (event.containsKey("span")) {
                    var span = this.spanConverter.read((Map<String, Object>) event.get("span"));
                    handleSpan(span, application, now);
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
        return Response.ok();
    }

    private void handleTransaction(Transaction transaction, Application application, Instant handlingTime) {
        if ("request".equals(transaction.getType())
                || "page-load".equals(transaction.getType())
        ) {
            // Incoming http request.
            var httpBuilder = new HttpTelemetryEventBuilder();
            createRequestFromTransaction(httpBuilder, transaction, application, handlingTime);
            telemetryCommandProcessor.processTelemetryEvent(httpBuilder, null);

            var id = httpBuilder.getId();
            createResponseFromTransaction(httpBuilder, transaction, application, handlingTime);
            httpBuilder.setId(id + RESPONSE_ID_SUFFIX);
            httpBuilder.setCorrelationId(id);
            telemetryCommandProcessor.processTelemetryEvent(httpBuilder, null);
        } else if ("messaging".equals(transaction.getType())) {
            // Incoming JMS request
            var messagingBuilder = new MessagingTelemetryEventBuilder();
            createRequestFromTransaction(messagingBuilder, transaction, application, handlingTime);
            telemetryCommandProcessor.processTelemetryEvent(messagingBuilder, null);

//            var id = messagingBuilder.getId();
            // TODO, might be a fire-forget message.
//            createResponseFromTransaction(messagingBuilder, transaction, application, handlingTime);
//            messagingBuilder.setId(id + RESPONSE_ID_SUFFIX);
//            messagingBuilder.setCorrelationId(id);
//            telemetryCommandProcessor.processTelemetryEvent(messagingBuilder, null);
        }

    }

    private void handleSpan(Span span, Application application, Instant handlingTime) {
        if (span.getContext() == null) {
            return;
        }
        var context = span.getContext();
        if (context.getHttp() != null && "http".equals(span.getSubtype())) {
            var httpBuilder = new HttpTelemetryEventBuilder();
            createRequestFromSpan(httpBuilder, span, application, handlingTime);
            telemetryCommandProcessor.processTelemetryEvent(httpBuilder, null);

            var id = httpBuilder.getId();
            createResponseFromSpan(httpBuilder, span, application, handlingTime);
            httpBuilder.setId(id + RESPONSE_ID_SUFFIX);
            httpBuilder.setCorrelationId(id);
            telemetryCommandProcessor.processTelemetryEvent(httpBuilder, null);
        } else if (span.getContext().getDb() != null) {
            var sqlBuilder = new SqlTelemetryEventBuilder();
            createRequestFromSpan(sqlBuilder, span, application, handlingTime);
            telemetryCommandProcessor.processTelemetryEvent(sqlBuilder, null);

            var id = sqlBuilder.getId();
            createResponseFromSpan(sqlBuilder, span, application, handlingTime);
            sqlBuilder.setId(id + RESPONSE_ID_SUFFIX);
            sqlBuilder.setCorrelationId(id);
            telemetryCommandProcessor.processTelemetryEvent(sqlBuilder, null);
        }
    }

    private <T extends TelemetryEvent<T>, Q extends TelemetryEventBuilder<T, Q>> void createRequestFromTransaction(TelemetryEventBuilder<T, Q> builder, Transaction transaction, Application application, Instant handlingTime) {
        builder.initialize();
        var context = transaction.getContext();
        var endpointBuilder = new EndpointBuilder();
        var endpointHandlerBuilder = new EndpointHandlerBuilder();

        if (transaction.getParentId() != null) {
            builder.setId(transaction.getParentId());
        } else {
            builder.setId(transaction.getId());
        }
        builder.setName(transaction.getName());
        builder.setTraceId(transaction.getTraceId());


        if (context.getRequest() != null) {
            var request = context.getRequest();
            endpointBuilder.setName(request.getUrl().getFull());
            builder.setPayload(request.getBody());
            if (request.getHeaders() != null) {
                String metadataPrefix = "";
                if (builder instanceof HttpTelemetryEventBuilder) {
                    metadataPrefix = "http_";
                }
                for (var entry : request.getHeaders().entrySet()) {
                    endpointHandlerBuilder.addMetadata(metadataPrefix + entry.getKey().toLowerCase(), entry.getValue());
                }
            }
        }
        if (context.getPage() != null) {
            var page = context.getPage();
            endpointBuilder.setName(page.getUrl());
        }
        endpointHandlerBuilder.setType(EndpointHandler.EndpointHandlerType.READER);
        if (transaction.getTimestamp() != null) {
            endpointHandlerBuilder.setHandlingTime(Instant.EPOCH.plus(transaction.getTimestamp(), ChronoUnit.MICROS));
        } else {
            endpointHandlerBuilder.setHandlingTime(handlingTime);
        }
        endpointHandlerBuilder.setTransactionId(transaction.getId());
        endpointHandlerBuilder.setApplication(application);
        endpointBuilder.addEndpointHandler(endpointHandlerBuilder);
        builder.addOrMergeEndpoint(endpointBuilder);

        if (builder instanceof HttpTelemetryEventBuilder) {
            var httpBuilder = ((HttpTelemetryEventBuilder) builder);
            if (context.getRequest() != null) {
                httpBuilder.setHttpEventType(HttpTelemetryEvent.HttpEventType.safeValueOf(context.getRequest().getMethod()));
            } else if (context.getPage() != null) {
                httpBuilder.setHttpEventType(HttpTelemetryEvent.HttpEventType.GET);
            }
        }
    }

    private <T extends TelemetryEvent<T>, Q extends TelemetryEventBuilder<T, Q>> void createResponseFromTransaction(TelemetryEventBuilder<T, Q> builder, Transaction transaction, Application application, Instant handlingTime) {
        builder.initialize();
        var context = transaction.getContext();
        var endpointBuilder = new EndpointBuilder();
        var endpointHandlerBuilder = new EndpointHandlerBuilder();

        builder.setName(transaction.getName() + RESPONSE_NAME_SUFFIX);
        builder.setTraceId(transaction.getTraceId());
        if (context.getRequest() != null) {
            var request = context.getRequest();
            endpointBuilder.setName(request.getUrl().getFull());
        }
        if (context.getPage() != null) {
            var page = context.getPage();
            endpointBuilder.setName(page.getUrl());
        }
        if (context.getResponse() != null) {
            var response = context.getResponse();
            if (response.getHeaders() != null) {
                String metadataPrefix = "";
                if (builder instanceof HttpTelemetryEventBuilder) {
                    metadataPrefix = "http_";
                }
                for (var entry : response.getHeaders().entrySet()) {
                    endpointHandlerBuilder.addMetadata(metadataPrefix + entry.getKey().toLowerCase(), entry.getValue());
                }
            }
            if (builder instanceof HttpTelemetryEventBuilder) {
                if (response.getStatusCode() != null && response.getStatusCode() != 0) {
                    ((HttpTelemetryEventBuilder) builder).setStatusCode(response.getStatusCode().intValue());
                }
            }
        }

        endpointHandlerBuilder.setType(EndpointHandler.EndpointHandlerType.WRITER);
        if (transaction.getTimestamp() != null) {
            endpointHandlerBuilder.setHandlingTime(Instant.EPOCH.plus(transaction.getTimestamp(), ChronoUnit.MICROS).plus(transaction.getDuration(), ChronoUnit.MILLIS));
        } else {
            endpointHandlerBuilder.setHandlingTime(handlingTime.plus(transaction.getDuration(), ChronoUnit.MILLIS));
        }
        endpointHandlerBuilder.setTransactionId(transaction.getId());
        endpointHandlerBuilder.setApplication(application);
        endpointBuilder.addEndpointHandler(endpointHandlerBuilder);
        builder.addOrMergeEndpoint(endpointBuilder);

        if (builder instanceof HttpTelemetryEventBuilder) {
            ((HttpTelemetryEventBuilder) builder).setHttpEventType(HttpTelemetryEvent.HttpEventType.RESPONSE);
        }
    }

    private <T extends TelemetryEvent<T>, Q extends TelemetryEventBuilder<T, Q>> void createRequestFromSpan(TelemetryEventBuilder<T, Q> builder, Span span, Application application, Instant handlingTime) {
        builder.initialize();
        var context = span.getContext();
        var endpointBuilder = new EndpointBuilder();
        var endpointHandlerBuilder = new EndpointHandlerBuilder();

        if (context.getHttp() != null) {
            var http = context.getHttp();
            if (builder instanceof HttpTelemetryEventBuilder) {
                var httpBuilder = (HttpTelemetryEventBuilder) builder;
                httpBuilder.setHttpEventType(HttpTelemetryEvent.HttpEventType.safeValueOf(http.getMethod()));
            }
            endpointBuilder.setName(http.getUrl());
        }
        if (context.getDb() != null) {
            var db = context.getDb();
            if (builder instanceof SqlTelemetryEventBuilder) {
                var sqlBuilder = (SqlTelemetryEventBuilder) builder;
                sqlBuilder.setDbQueryEventType(SqlTelemetryEvent.SqlEventType.safeValueOf(db.getStatement().substring(0, db.getStatement().indexOf(" "))));
            }
            builder.setPayload(db.getStatement());
            endpointBuilder.setName(span.getContext().getDestination().getAddress());
        }

        builder.setId(span.getId());
        builder.setName(span.getName());
        builder.setTraceId(span.getTraceId());

        endpointHandlerBuilder.setType(EndpointHandler.EndpointHandlerType.WRITER);
        if (span.getTimestamp() != null) {
            endpointHandlerBuilder.setHandlingTime(Instant.EPOCH.plus(span.getTimestamp(), ChronoUnit.MICROS));
        } else if (span.getStart() != null) {
            endpointHandlerBuilder.setHandlingTime(handlingTime.plus(span.getStart(), ChronoUnit.MILLIS));
        }
        endpointHandlerBuilder.setTransactionId(span.getTransactionId());
        endpointHandlerBuilder.setApplication(application);
        endpointBuilder.addEndpointHandler(endpointHandlerBuilder);
        builder.addOrMergeEndpoint(endpointBuilder);
    }

    private <T extends TelemetryEvent<T>, Q extends TelemetryEventBuilder<T, Q>> void createResponseFromSpan(TelemetryEventBuilder<T, Q> builder, Span span, Application application, Instant handlingTime) {
        builder.initialize();
        var context = span.getContext();
        var endpointBuilder = new EndpointBuilder();
        var endpointHandlerBuilder = new EndpointHandlerBuilder();

        if (context.getHttp() != null) {
            var http = context.getHttp();
            if (builder instanceof HttpTelemetryEventBuilder) {
                var httpBuilder = (HttpTelemetryEventBuilder) builder;
                httpBuilder.setHttpEventType(HttpTelemetryEvent.HttpEventType.RESPONSE);
            }
            endpointBuilder.setName(http.getUrl());
        }
        if (context.getDb() != null) {
            if (builder instanceof SqlTelemetryEventBuilder) {
                var sqlBuilder = (SqlTelemetryEventBuilder) builder;
                sqlBuilder.setDbQueryEventType(SqlTelemetryEvent.SqlEventType.RESULTSET);
            }
            endpointBuilder.setName(span.getContext().getDestination().getAddress());
        }
        builder.setName(span.getName() + RESPONSE_NAME_SUFFIX);
        builder.setTraceId(span.getTraceId());

        endpointHandlerBuilder.setType(EndpointHandler.EndpointHandlerType.READER);
        if (span.getTimestamp() != null) {
            endpointHandlerBuilder.setHandlingTime(Instant.EPOCH.plus(span.getTimestamp(), ChronoUnit.MICROS).plus(span.getDuration(), ChronoUnit.MILLIS));
        } else if (span.getStart() != null) {
            endpointHandlerBuilder.setHandlingTime(handlingTime.plus(span.getStart(), ChronoUnit.MILLIS).plus(span.getDuration(), ChronoUnit.MILLIS));
        }
        endpointHandlerBuilder.setTransactionId(span.getTransactionId());
        endpointHandlerBuilder.setApplication(application);
        endpointBuilder.addEndpointHandler(endpointHandlerBuilder);
        builder.addOrMergeEndpoint(endpointBuilder);
    }
}
