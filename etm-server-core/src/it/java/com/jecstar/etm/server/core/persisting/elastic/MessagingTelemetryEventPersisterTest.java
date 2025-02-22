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

package com.jecstar.etm.server.core.persisting.elastic;

import com.jecstar.etm.domain.Endpoint;
import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.MessagingTelemetryEvent;
import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.PayloadFormat;
import com.jecstar.etm.domain.builder.ApplicationBuilder;
import com.jecstar.etm.domain.builder.EndpointBuilder;
import com.jecstar.etm.domain.builder.EndpointHandlerBuilder;
import com.jecstar.etm.domain.builder.MessagingTelemetryEventBuilder;
import com.jecstar.etm.server.core.AbstractIntegrationTest;
import com.jecstar.etm.server.core.domain.converter.json.MessagingTelemetryEventConverterJsonImpl;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


public class MessagingTelemetryEventPersisterTest extends AbstractIntegrationTest {

    private final MessagingTelemetryEventConverterJsonImpl messagingEventConverter = new MessagingTelemetryEventConverterJsonImpl();

    protected BulkProcessor.Listener createBulkListener() {
        return new BulkProcessor.Listener() {

            @Override
            public void beforeBulk(long executionId, BulkRequest request) {
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                if (response.hasFailures()) {
                    for (BulkItemResponse bulkResponse : response.getItems()) {
                        if (bulkResponse.isFailed() && bulkResponse.getFailure().getCause() != null) {
                            bulkResponse.getFailure().getCause().printStackTrace();
                        }
                    }
                    fail(response.buildFailureMessage());
                }
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                fail(failure.getMessage());
            }
        };
    }

    /**
     * Test the merging reading event handlers and writing event handlers when the reader is added after the writer.
     */
    @Test
    public void testMergingOfReaderAfterWriter() throws InterruptedException {
        final String eventId = UUID.randomUUID().toString();
        final MessagingTelemetryEventPersister persister = new MessagingTelemetryEventPersister(bulkProcessor, etmConfiguration);

        final EndpointHandler writingEndpointHandler = new EndpointHandlerBuilder()
                .setType(EndpointHandler.EndpointHandlerType.WRITER)
                .setHandlingTime(Instant.now())
                .setApplication(new ApplicationBuilder()
                        .setName("Writing app")
                ).build();

        final EndpointHandler readingEndpointHandler = new EndpointHandlerBuilder()
                .setType(EndpointHandler.EndpointHandlerType.READER)
                .setHandlingTime(Instant.now().plusSeconds(1))
                .setApplication(new ApplicationBuilder()
                        .setName("Reading app")
                ).build();


        MessagingTelemetryEventBuilder builder = new MessagingTelemetryEventBuilder()
                .setId(eventId)
                .setPayload("Test case " + this.getClass().getName())
                .setPayloadFormat(PayloadFormat.TEXT)
                .setMessagingEventType(MessagingEventType.REQUEST)
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("TEST.QUEUE")
                        .addEndpointHandler(writingEndpointHandler)
                );
        persister.persist(builder.build(), this.messagingEventConverter);
        waitFor(persister.getElasticIndexName(), eventId, 1L);

        builder.initialize()
                .setId(eventId)
                .setPayload("Test case " + this.getClass().getName())
                .setPayloadFormat(PayloadFormat.TEXT)
                .setMessagingEventType(MessagingEventType.REQUEST)
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("TEST.QUEUE")
                        .addEndpointHandler(readingEndpointHandler)
                );
        persister.persist(builder.build(), this.messagingEventConverter);
        GetResponse getResponse = waitFor(persister.getElasticIndexName(), eventId, 2L);

        MessagingTelemetryEvent readEvent = this.messagingEventConverter.read(getResponse.getSourceAsMap(), getResponse.getId());
        assertEquals(eventId, readEvent.id, eventId);
        assertEquals(1, readEvent.endpoints.size(), eventId);
        Endpoint endpoint = readEvent.endpoints.get(0);
        assertEquals("TEST.QUEUE", endpoint.name, eventId);
        assertEquals("Writing app", endpoint.getWritingEndpointHandler().application.name, eventId);
        assertEquals(1, endpoint.getReadingEndpointHandlers().size(), eventId);
        assertEquals("Reading app", endpoint.getReadingEndpointHandlers().get(0).application.name, eventId);
    }

    /**
     * Test the merging reading event handlers and writing event handlers when the writer is added after the reader.
     */
    @Test
    public void testMergingOfWriterAfterReader() throws InterruptedException {
        final String eventId = UUID.randomUUID().toString();
        final MessagingTelemetryEventPersister persister = new MessagingTelemetryEventPersister(bulkProcessor, etmConfiguration);

        final EndpointHandler writingEndpointHandler = new EndpointHandlerBuilder()
                .setType(EndpointHandler.EndpointHandlerType.WRITER)
                .setHandlingTime(Instant.now())
                .setApplication(new ApplicationBuilder()
                        .setName("Writing app")
                ).build();

        final EndpointHandler readingEndpointHandler = new EndpointHandlerBuilder()
                .setType(EndpointHandler.EndpointHandlerType.READER)
                .setHandlingTime(Instant.now().plusSeconds(1))
                .setApplication(new ApplicationBuilder()
                        .setName("Reading app")
                ).build();

        MessagingTelemetryEventBuilder builder = new MessagingTelemetryEventBuilder()
                .setId(eventId)
                .setPayload("Test case " + this.getClass().getName())
                .setPayloadFormat(PayloadFormat.TEXT)
                .setMessagingEventType(MessagingEventType.REQUEST)
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("TEST.QUEUE")
                        .addEndpointHandler(readingEndpointHandler)
                );
        persister.persist(builder.build(), this.messagingEventConverter);
        waitFor(persister.getElasticIndexName(), eventId, 1L);

        builder.initialize()
                .setId(eventId)
                .setPayload("Test case " + this.getClass().getName())
                .setPayloadFormat(PayloadFormat.TEXT)
                .setMessagingEventType(MessagingEventType.REQUEST)
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("TEST.QUEUE")
                        .addEndpointHandler(writingEndpointHandler)
                );
        persister.persist(builder.build(), this.messagingEventConverter);
        GetResponse getResponse = waitFor(persister.getElasticIndexName(), eventId, 2L);

        MessagingTelemetryEvent readEvent = this.messagingEventConverter.read(getResponse.getSourceAsMap(), getResponse.getId());
        assertEquals(eventId, readEvent.id, eventId);
        assertEquals(1, readEvent.endpoints.size(), eventId);
        Endpoint endpoint = readEvent.endpoints.get(0);
        assertEquals("TEST.QUEUE", endpoint.name, eventId);
        assertEquals("Writing app", endpoint.getWritingEndpointHandler().application.name, eventId);
        assertEquals(1, endpoint.getReadingEndpointHandlers().size(), eventId);
        assertEquals("Reading app", endpoint.getReadingEndpointHandlers().get(0).application.name, eventId);
    }

    /**
     * Test the merging of endpoints.
     */
    @Test
    public void testMergingOfEndpoints() throws InterruptedException {
        final String eventId = UUID.randomUUID().toString();
        final MessagingTelemetryEventPersister persister = new MessagingTelemetryEventPersister(bulkProcessor, etmConfiguration);

        final EndpointHandler writingEndpointHandler = new EndpointHandlerBuilder()
                .setType(EndpointHandler.EndpointHandlerType.WRITER)
                .setHandlingTime(Instant.now())
                .setApplication(new ApplicationBuilder()
                        .setName("Writing app")
                ).build();

        final EndpointHandler readingEndpointHandler = new EndpointHandlerBuilder()
                .setType(EndpointHandler.EndpointHandlerType.READER)
                .setHandlingTime(Instant.now().plusSeconds(1))
                .setApplication(new ApplicationBuilder()
                        .setName("Reading app")
                ).build();

        MessagingTelemetryEventBuilder builder = new MessagingTelemetryEventBuilder()
                .setId(eventId)
                .setPayload("Test case " + this.getClass().getName())
                .setPayloadFormat(PayloadFormat.TEXT)
                .setMessagingEventType(MessagingEventType.REQUEST)
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("TEST.QUEUE")
                        .addEndpointHandler(writingEndpointHandler)
                        .addEndpointHandler(readingEndpointHandler)
                );
        persister.persist(builder.build(), this.messagingEventConverter);
        waitFor(persister.getElasticIndexName(), eventId, 1L);

        builder.initialize()
                .setId(eventId)
                .setPayload("Test case " + this.getClass().getName())
                .setPayloadFormat(PayloadFormat.TEXT)
                .setMessagingEventType(MessagingEventType.REQUEST)
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("ANOTHER.TEST.QUEUE")
                        .addEndpointHandler(writingEndpointHandler)
                        .addEndpointHandler(readingEndpointHandler)
                );
        persister.persist(builder.build(), this.messagingEventConverter);
        GetResponse getResponse = waitFor(persister.getElasticIndexName(), eventId, 2L);

        MessagingTelemetryEvent readEvent = this.messagingEventConverter.read(getResponse.getSourceAsMap(), getResponse.getId());
        assertEquals(eventId, readEvent.id, eventId);
        assertEquals(2, readEvent.endpoints.size(), eventId);
    }

    /**
     * Test the merging of fields.
     */
    @Test
    public void testMergingOfFields() throws InterruptedException {
        final String eventId = UUID.randomUUID().toString();
        final String eventName = "Test case " + this.getClass().getName() + " - testMergingOfFields()";
        final String payload = "Test case " + this.getClass().getName();
        final MessagingTelemetryEventPersister persister = new MessagingTelemetryEventPersister(bulkProcessor, etmConfiguration);

        final EndpointHandler writingEndpointHandler = new EndpointHandlerBuilder()
                .setType(EndpointHandler.EndpointHandlerType.WRITER)
                .setHandlingTime(Instant.now())
                .setApplication(new ApplicationBuilder()
                        .setName("Writing app")
                ).build();

        final EndpointHandler readingEndpointHandler = new EndpointHandlerBuilder()
                .setType(EndpointHandler.EndpointHandlerType.READER)
                .setHandlingTime(Instant.now().plusSeconds(1))
                .setApplication(new ApplicationBuilder()
                        .setName("Reading app")
                ).build();

        MessagingTelemetryEventBuilder builder = new MessagingTelemetryEventBuilder()
                .setId(eventId)
                .addMetadata("key1", "initial value")
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("TEST.QUEUE")
                        .addEndpointHandler(writingEndpointHandler)
                        .addEndpointHandler(readingEndpointHandler)
                );
        persister.persist(builder.build(), this.messagingEventConverter);
        waitFor(persister.getElasticIndexName(), eventId, 1L);

        builder.initialize()
                .setId(eventId)
                .setPayload(payload)
                .setName(eventName)
                .setPayloadFormat(PayloadFormat.TEXT)
                .addMetadata("key1", "wrong value")
                .addMetadata("key2", "merged value")
                .setMessagingEventType(MessagingEventType.REQUEST)
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("ANOTHER.TEST.QUEUE")
                        .addEndpointHandler(writingEndpointHandler)
                        .addEndpointHandler(readingEndpointHandler)
                );
        persister.persist(builder.build(), this.messagingEventConverter);
        GetResponse getResponse = waitFor(persister.getElasticIndexName(), eventId, 2L);

        MessagingTelemetryEvent readEvent = this.messagingEventConverter.read(getResponse.getSourceAsMap(), getResponse.getId());
        assertEquals(eventId, readEvent.id, eventId);
        assertEquals(eventName, readEvent.name, eventId);
        assertEquals(payload, readEvent.payload, eventId);
        assertEquals(PayloadFormat.TEXT, readEvent.payloadFormat, eventId);
        assertEquals(MessagingEventType.REQUEST, readEvent.messagingEventType, eventId);
        assertEquals("initial value", readEvent.metadata.get("key1"), eventId);
        assertEquals("merged value", readEvent.metadata.get("key2"), eventId);
    }

    /**
     * Test if the response data (correlation id, response time etc) is merged in the request when the response is added after the request.
     */
    @Test
    public void testMergingOfResponseAfterRequest() throws InterruptedException {
        final String requestId = UUID.randomUUID().toString();
        final String responseId = UUID.randomUUID().toString();
        final MessagingTelemetryEventPersister persister = new MessagingTelemetryEventPersister(bulkProcessor, etmConfiguration);

        final Instant timeStamp = Instant.now();
        final EndpointHandlerBuilder requestingEndpointHandler = new EndpointHandlerBuilder()
                .setType(EndpointHandler.EndpointHandlerType.WRITER)
                .setHandlingTime(timeStamp)
                .setApplication(new ApplicationBuilder()
                        .setName("Requesting App")
                );

        final EndpointHandlerBuilder respondingEndpointHandler = new EndpointHandlerBuilder()
                .setType(EndpointHandler.EndpointHandlerType.READER)
                .setHandlingTime(timeStamp.plusSeconds(1))
                .setApplication(new ApplicationBuilder()
                        .setName("Responding App")
                );

        MessagingTelemetryEventBuilder builder = new MessagingTelemetryEventBuilder()
                .setId(requestId)
                .setPayload("Test case " + this.getClass().getName())
                .setPayloadFormat(PayloadFormat.TEXT)
                .setMessagingEventType(MessagingEventType.REQUEST)
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("REQUEST.QUEUE")
                        .addEndpointHandler(requestingEndpointHandler)
                        .addEndpointHandler(respondingEndpointHandler)
                );
        persister.persist(builder.build(), this.messagingEventConverter);
        waitFor(persister.getElasticIndexName(), requestId, 1L);

        builder.initialize()
                .setId(responseId)
                .setPayload("Test case " + this.getClass().getName())
                .setPayloadFormat(PayloadFormat.TEXT)
                .setMessagingEventType(MessagingEventType.RESPONSE)
                .setCorrelationId(requestId)
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("RESPONSE.QUEUE")
                        .addEndpointHandler(respondingEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER).setHandlingTime(timeStamp.plusSeconds(2)))
                        .addEndpointHandler(requestingEndpointHandler.setType(EndpointHandler.EndpointHandlerType.READER).setHandlingTime(timeStamp.plusSeconds(3)))
                );
        persister.persist(builder.build(), this.messagingEventConverter);
        waitFor(persister.getElasticIndexName(), responseId, 1L);
        GetResponse getResponse = waitFor(persister.getElasticIndexName(), requestId, 2L);

        MessagingTelemetryEvent event = this.messagingEventConverter.read(getResponse.getSourceAsMap(), getResponse.getId());
        assertEquals(1, event.correlations.size(), event.id);
        assertEquals(responseId, event.correlations.get(0), event.id);

        assertEquals(1, event.endpoints.size(), event.id);
        Endpoint endpoint = event.endpoints.get(0);
        assertEquals(1000, endpoint.getReadingEndpointHandlers().get(0).responseTime.longValue(), event.id);
        assertEquals(1000, endpoint.getReadingEndpointHandlers().get(0).latency.longValue(), event.id);
        assertEquals(3000, endpoint.getWritingEndpointHandler().responseTime.longValue(), event.id);
    }

    /**
     * Test if the response data (correlation id, response time etc) is merged in the request when the response is added after the request.
     */
    @Test
    public void testMergingOfRequestAfterResponse() throws InterruptedException {
        final String requestId = UUID.randomUUID().toString();
        final String responseId = UUID.randomUUID().toString();
        final MessagingTelemetryEventPersister persister = new MessagingTelemetryEventPersister(bulkProcessor, etmConfiguration);

        final Instant timeStamp = Instant.now();
        final EndpointHandlerBuilder requestingEndpointHandler = new EndpointHandlerBuilder()
                .setType(EndpointHandler.EndpointHandlerType.READER)
                .setHandlingTime(timeStamp)
                .setApplication(new ApplicationBuilder()
                        .setName("Requesting App")
                );

        final EndpointHandlerBuilder respondingEndpointHandler = new EndpointHandlerBuilder()
                .setType(EndpointHandler.EndpointHandlerType.WRITER)
                .setHandlingTime(timeStamp.plusSeconds(1))
                .setApplication(new ApplicationBuilder()
                        .setName("Responding App")
                );

        MessagingTelemetryEventBuilder builder = new MessagingTelemetryEventBuilder()
                .setId(responseId)
                .setPayload("Test case " + this.getClass().getName())
                .setPayloadFormat(PayloadFormat.TEXT)
                .setMessagingEventType(MessagingEventType.RESPONSE)
                .setCorrelationId(requestId)
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("RESPONSE.QUEUE")
                        .addEndpointHandler(respondingEndpointHandler.setHandlingTime(timeStamp.plusSeconds(2)))
                        .addEndpointHandler(requestingEndpointHandler.setHandlingTime(timeStamp.plusSeconds(3)))
                );
        persister.persist(builder.build(), this.messagingEventConverter);
        waitFor(persister.getElasticIndexName(), responseId, 1L);

        builder.initialize()
                .setId(requestId)
                .setPayload("Test case " + this.getClass().getName())
                .setPayloadFormat(PayloadFormat.TEXT)
                .setMessagingEventType(MessagingEventType.REQUEST)
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("REQUEST.QUEUE")
                        .addEndpointHandler(requestingEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER).setHandlingTime(timeStamp))
                        .addEndpointHandler(respondingEndpointHandler.setType(EndpointHandler.EndpointHandlerType.READER).setHandlingTime(timeStamp.plusSeconds(1)))
                );
        persister.persist(builder.build(), this.messagingEventConverter);
        GetResponse getResponse = waitFor(persister.getElasticIndexName(), requestId, 2L);

        MessagingTelemetryEvent event = this.messagingEventConverter.read(getResponse.getSourceAsMap(), getResponse.getId());
        assertEquals(1, event.correlations.size(), event.id);
        assertEquals(responseId, event.correlations.get(0), event.id);

        assertEquals(1, event.endpoints.size(), event.id);
        Endpoint endpoint = event.endpoints.get(0);
        assertEquals(1000, endpoint.getReadingEndpointHandlers().get(0).responseTime.longValue(), event.id);
        assertEquals(1000, endpoint.getReadingEndpointHandlers().get(0).latency.longValue(), event.id);
        assertEquals(3000, endpoint.getWritingEndpointHandler().responseTime.longValue(), event.id);
    }

    /**
     * Test if persisting an event twice doesn't lead to merged endpoints etc. The hash of the events should be equals and hence the merging part in the scripts should be skipped.
     */
    @Test
    public void testPersistMessageTwice() throws InterruptedException {
        final String requestId = UUID.randomUUID().toString();
        final MessagingTelemetryEventPersister persister = new MessagingTelemetryEventPersister(bulkProcessor, etmConfiguration);

        final Instant timeStamp = Instant.now();
        final EndpointHandlerBuilder requestingEndpointHandler = new EndpointHandlerBuilder()
                .setType(EndpointHandler.EndpointHandlerType.WRITER)
                .setHandlingTime(timeStamp)
                .setApplication(new ApplicationBuilder()
                        .setName("Requesting App")
                );

        final EndpointHandlerBuilder respondingEndpointHandler = new EndpointHandlerBuilder()
                .setType(EndpointHandler.EndpointHandlerType.READER)
                .setHandlingTime(timeStamp.plusSeconds(1))
                .setApplication(new ApplicationBuilder()
                        .setName("Responding App")
                );

        MessagingTelemetryEventBuilder builder = new MessagingTelemetryEventBuilder()
                .setId(requestId)
                .setPayload("Test case " + this.getClass().getName())
                .setPayloadFormat(PayloadFormat.TEXT)
                .setMessagingEventType(MessagingEventType.REQUEST)
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("REQUEST.QUEUE")
                        .addEndpointHandler(requestingEndpointHandler)
                        .addEndpointHandler(respondingEndpointHandler)
                );
        persister.persist(builder.build(), this.messagingEventConverter);
        GetResponse getResponse = waitFor(persister.getElasticIndexName(), requestId, 1L);
        MessagingTelemetryEvent event_v1 = this.messagingEventConverter.read(getResponse.getSourceAsMap(), getResponse.getId());

        persister.persist(builder.build(), this.messagingEventConverter);
        getResponse = waitFor(persister.getElasticIndexName(), requestId, 2L);
        MessagingTelemetryEvent event_v2 = this.messagingEventConverter.read(getResponse.getSourceAsMap(), getResponse.getId());
        assertEquals(event_v1.getCalculatedHash(), event_v2.getCalculatedHash(), getResponse.getId());
    }

    /**
     * Normally the response time is calculated based on the applications that read and/or write the requests and responses. If no applications are provided with an endpoint
     * in the request and reply messages the response time should also be set.
     */
    @Test
    public void testSetResponseTimeWithoutApplicationsRequestBeforeResponse() throws InterruptedException {
        final String requestId = UUID.randomUUID().toString();
        final String responseId = UUID.randomUUID().toString();
        final Instant timestamp = Instant.now();
        final EndpointHandlerBuilder endpointHandler = new EndpointHandlerBuilder()
                .setType(EndpointHandler.EndpointHandlerType.WRITER)
                .setHandlingTime(timestamp);
        final MessagingTelemetryEventPersister persister = new MessagingTelemetryEventPersister(bulkProcessor, etmConfiguration);
        MessagingTelemetryEventBuilder builder = new MessagingTelemetryEventBuilder()
                .setId(requestId)
                .setPayload("Test case " + this.getClass().getName())
                .setPayloadFormat(PayloadFormat.TEXT)
                .addOrMergeEndpoint(new EndpointBuilder().addEndpointHandler(endpointHandler))
                .setMessagingEventType(MessagingEventType.REQUEST);
        persister.persist(builder.build(), this.messagingEventConverter);
        waitFor(persister.getElasticIndexName(), requestId, 1L);

        endpointHandler.setHandlingTime(timestamp.plusSeconds(1));
        builder
                .setId(responseId)
                .setCorrelationId(requestId)
                .addOrMergeEndpoint(new EndpointBuilder().addEndpointHandler(endpointHandler))
                .setMessagingEventType(MessagingEventType.RESPONSE);
        persister.persist(builder.build(), this.messagingEventConverter);
        // wait for the request to be updated with the response time
        GetResponse getResponse = waitFor(persister.getElasticIndexName(), requestId, 2L);
        MessagingTelemetryEvent requestEvent = this.messagingEventConverter.read(getResponse.getSourceAsMap(), getResponse.getId());
        assertNotNull(requestEvent.endpoints.get(0).getWritingEndpointHandler().responseTime);
        long responsetTime = requestEvent.endpoints.get(0).getWritingEndpointHandler().responseTime;
        assertEquals(1000L, responsetTime, requestId);
    }

    /**
     * Normally the response time is calculated based on the applications that read and/or write the requests and responses. If no applications are provided with an endpoint
     * in the request and reply messages the response time should also be set.
     */
    @Test
    public void testSetResponseTimeWithoutApplicationsResponseBeforeRequest() throws InterruptedException {
        final String requestId = UUID.randomUUID().toString();
        final String responseId = UUID.randomUUID().toString();
        final Instant timestamp = Instant.now();
        final EndpointHandlerBuilder endpointHandler = new EndpointHandlerBuilder()
                .setType(EndpointHandler.EndpointHandlerType.WRITER)
                .setHandlingTime(timestamp.plusSeconds(1));
        final MessagingTelemetryEventPersister persister = new MessagingTelemetryEventPersister(bulkProcessor, etmConfiguration);
        MessagingTelemetryEventBuilder builder = new MessagingTelemetryEventBuilder()
                .setId(responseId)
                .setCorrelationId(requestId)
                .setPayload("Test case " + this.getClass().getName())
                .setPayloadFormat(PayloadFormat.TEXT)
                .addOrMergeEndpoint(new EndpointBuilder().addEndpointHandler(endpointHandler))
                .setMessagingEventType(MessagingEventType.RESPONSE);
        persister.persist(builder.build(), this.messagingEventConverter);
        waitFor(persister.getElasticIndexName(), requestId, 1L);

        endpointHandler.setHandlingTime(timestamp);
        builder
                .setId(requestId)
                .setCorrelationId(null)
                .addOrMergeEndpoint(new EndpointBuilder().addEndpointHandler(endpointHandler))
                .setMessagingEventType(MessagingEventType.REQUEST);
        persister.persist(builder.build(), this.messagingEventConverter);
        // wait for the request to be updated with the response time
        GetResponse getResponse = waitFor(persister.getElasticIndexName(), requestId, 2L);
        MessagingTelemetryEvent requestEvent = this.messagingEventConverter.read(getResponse.getSourceAsMap(), getResponse.getId());
        assertNotNull(requestEvent.endpoints.get(0).getWritingEndpointHandler().responseTime, requestId);
        long responsetTime = requestEvent.endpoints.get(0).getWritingEndpointHandler().responseTime;
        assertEquals(1000L, responsetTime, requestId);
    }

    /**
     * A fire-forget message that correlates another fire-forget message should
     * be visible on both events. So the correlating event should have the
     * correlation id set and the correlated event should have the id of the
     * correlation event in it's correlations list.
     */
    @Test
    public void testAlwaysCorrelateFirstBeforeSecond() throws InterruptedException {
        final String firstId = UUID.randomUUID().toString();
        final String secondId = UUID.randomUUID().toString();
        final MessagingTelemetryEventPersister persister = new MessagingTelemetryEventPersister(bulkProcessor, etmConfiguration);
        MessagingTelemetryEventBuilder builder = new MessagingTelemetryEventBuilder()
                .setId(firstId)
                .setPayload("Test case " + this.getClass().getName())
                .setPayloadFormat(PayloadFormat.TEXT)
                .setMessagingEventType(MessagingEventType.FIRE_FORGET);
        // Persist a fire-forget message.
        persister.persist(builder.build(), this.messagingEventConverter);
        waitFor(persister.getElasticIndexName(), firstId, 1L);

        // Persist a second fire-forget message correlating the first fire-forget message.
        builder.setId(secondId).setCorrelationId(firstId);
        persister.persist(builder.build(), this.messagingEventConverter);

        GetResponse getResponse = waitFor(persister.getElasticIndexName(), firstId, 2L);
        MessagingTelemetryEvent event = this.messagingEventConverter.read(getResponse.getSourceAsMap(), getResponse.getId());

        // Make sure the second event is referenced in the first event.
        assertTrue(event.correlations.contains(secondId), firstId);
    }

    @Test
    public void testAlwaysCorrelateSecondBeforeFirst() throws InterruptedException {
        final String firstId = UUID.randomUUID().toString();
        final String secondId = UUID.randomUUID().toString();
        final MessagingTelemetryEventPersister persister = new MessagingTelemetryEventPersister(bulkProcessor, etmConfiguration);
        MessagingTelemetryEventBuilder builder = new MessagingTelemetryEventBuilder()
                .setId(secondId)
                .setCorrelationId(firstId)
                .setPayload("Test case " + this.getClass().getName())
                .setPayloadFormat(PayloadFormat.TEXT)
                .setMessagingEventType(MessagingEventType.FIRE_FORGET);
        // Persist a fire-forget message.
        persister.persist(builder.build(), this.messagingEventConverter);
        waitFor(persister.getElasticIndexName(), firstId, 1L);

        // Persist a second fire-forget message correlating the first fire-forget message.
        builder.setId(firstId).setCorrelationId(null);
        persister.persist(builder.build(), this.messagingEventConverter);

        GetResponse getResponse = waitFor(persister.getElasticIndexName(), firstId, 2L);
        MessagingTelemetryEvent event = this.messagingEventConverter.read(getResponse.getSourceAsMap(), getResponse.getId());

        // Make sure the second event is referenced in the first event.
        assertTrue(event.correlations.contains(secondId), firstId);
    }

}
