package com.jecstar.etm.processor.processor.persisting.elastic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.junit.Test;

import com.jecstar.etm.domain.Endpoint;
import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.MessagingTelemetryEvent;
import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.PayloadFormat;
import com.jecstar.etm.domain.builders.ApplicationBuilder;
import com.jecstar.etm.domain.builders.EndpointBuilder;
import com.jecstar.etm.domain.builders.EndpointHandlerBuilder;
import com.jecstar.etm.domain.builders.MessagingTelemetryEventBuilder;
import com.jecstar.etm.processor.AbstractIntegrationTest;
import com.jecstar.etm.server.core.domain.converter.json.MessagingTelemetryEventConverterJsonImpl;

public class MessagingTelemetryEventPersisterTest extends AbstractIntegrationTest {

	private final MessagingTelemetryEventConverterJsonImpl messagingEventConverter = new MessagingTelemetryEventConverterJsonImpl();
	
	protected BulkProcessor.Listener createBuilkListener() {
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
	 * Test the merging reading event handlers and writing event handlers when the writer is added after the reader. 
	 */
	@Test
	public void testMergingOfWriterAfterReader() throws InterruptedException {
		final String eventId = UUID.randomUUID().toString();
		final MessagingTelemetryEventPersister persister = new MessagingTelemetryEventPersister(bulkProcessor, etmConfiguration);
		
		final EndpointHandler writingEndpointHandler = new EndpointHandlerBuilder()
				.setHandlingTime(ZonedDateTime.now())
				.setApplication(new ApplicationBuilder()
						.setName("Writing app")
				).build();
		
		final EndpointHandler readingEndpointHandler = new EndpointHandlerBuilder()
				.setHandlingTime(ZonedDateTime.now().plusSeconds(1))
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
						.setWritingEndpointHandler(writingEndpointHandler)
					);
		persister.persist(builder.build(), this.messagingEventConverter);
		waitFor(persister.getElasticIndexName(), "messaging", eventId, 1L);
		
		builder.initialize()
			.setId(eventId)
			.setPayload("Test case " + this.getClass().getName())
			.setPayloadFormat(PayloadFormat.TEXT)
			.setMessagingEventType(MessagingEventType.REQUEST)
			.addOrMergeEndpoint(new EndpointBuilder()
						.setName("TEST.QUEUE")
						.addReadingEndpointHandler(readingEndpointHandler)
					);
		persister.persist(builder.build(), this.messagingEventConverter);
		GetResponse getResponse = waitFor(persister.getElasticIndexName(), "messaging", eventId, 2L);
		
		MessagingTelemetryEvent readEvent = this.messagingEventConverter.read(getResponse.getSourceAsMap());
		assertEquals(eventId, readEvent.id);
		assertEquals(1, readEvent.endpoints.size());
		Endpoint endpoint = readEvent.endpoints.get(0);
		assertEquals("TEST.QUEUE", endpoint.name);
		assertEquals("Writing app", endpoint.writingEndpointHandler.application.name);
		assertEquals(1, endpoint.readingEndpointHandlers.size());
		assertEquals("Reading app", endpoint.readingEndpointHandlers.get(0).application.name);
	}
	
	/**
	 * Test the merging reading event handlers and writing event handlers when the reader is added after the writer. 
	 */
	@Test
	public void testMergingOfReaderAfterWriter() throws InterruptedException {
		final String eventId = UUID.randomUUID().toString();
		final MessagingTelemetryEventPersister persister = new MessagingTelemetryEventPersister(bulkProcessor, etmConfiguration);
		
		final EndpointHandler writingEndpointHandler = new EndpointHandlerBuilder()
				.setHandlingTime(ZonedDateTime.now())
				.setApplication(new ApplicationBuilder()
						.setName("Writing app")
				).build();
		
		final EndpointHandler readingEndpointHandler = new EndpointHandlerBuilder()
				.setHandlingTime(ZonedDateTime.now().plusSeconds(1))
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
				.addReadingEndpointHandler(readingEndpointHandler)
				);
		persister.persist(builder.build(), this.messagingEventConverter);
		waitFor(persister.getElasticIndexName(), "messaging", eventId, 1L);
		
		builder.initialize()
		.setId(eventId)
		.setPayload("Test case " + this.getClass().getName())
		.setPayloadFormat(PayloadFormat.TEXT)
		.setMessagingEventType(MessagingEventType.REQUEST)
		.addOrMergeEndpoint(new EndpointBuilder()
				.setName("TEST.QUEUE")
				.setWritingEndpointHandler(writingEndpointHandler)
				);
		persister.persist(builder.build(), this.messagingEventConverter);
		GetResponse getResponse = waitFor(persister.getElasticIndexName(), "messaging", eventId, 2L);
		
		MessagingTelemetryEvent readEvent = this.messagingEventConverter.read(getResponse.getSourceAsMap());
		assertEquals(eventId, readEvent.id);
		assertEquals(1, readEvent.endpoints.size());
		Endpoint endpoint = readEvent.endpoints.get(0);
		assertEquals("TEST.QUEUE", endpoint.name);
		assertEquals("Writing app", endpoint.writingEndpointHandler.application.name);
		assertEquals(1, endpoint.readingEndpointHandlers.size());
		assertEquals("Reading app", endpoint.readingEndpointHandlers.get(0).application.name);
	}
	
	/**
	 * Test the merging of endpoints. 
	 */
	@Test
	public void testMergingOfEndpoints() throws InterruptedException {
		final String eventId = UUID.randomUUID().toString();
		final MessagingTelemetryEventPersister persister = new MessagingTelemetryEventPersister(bulkProcessor, etmConfiguration);
		
		final EndpointHandler writingEndpointHandler = new EndpointHandlerBuilder()
				.setHandlingTime(ZonedDateTime.now())
				.setApplication(new ApplicationBuilder()
						.setName("Writing app")
				).build();
		
		final EndpointHandler readingEndpointHandler = new EndpointHandlerBuilder()
				.setHandlingTime(ZonedDateTime.now().plusSeconds(1))
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
				.setWritingEndpointHandler(writingEndpointHandler)
				.addReadingEndpointHandler(readingEndpointHandler)
				);
		persister.persist(builder.build(), this.messagingEventConverter);
		waitFor(persister.getElasticIndexName(), "messaging", eventId, 1L);

		builder.initialize()
				.setId(eventId)
				.setPayload("Test case " + this.getClass().getName())
				.setPayloadFormat(PayloadFormat.TEXT)
				.setMessagingEventType(MessagingEventType.REQUEST)
				.addOrMergeEndpoint(new EndpointBuilder()
						.setName("ANOTHER.TEST.QUEUE")
						.setWritingEndpointHandler(writingEndpointHandler)
						.addReadingEndpointHandler(readingEndpointHandler)
						);
		persister.persist(builder.build(), this.messagingEventConverter);
		GetResponse getResponse = waitFor(persister.getElasticIndexName(), "messaging", eventId, 2L);
		
		MessagingTelemetryEvent readEvent = this.messagingEventConverter.read(getResponse.getSourceAsMap());
		assertEquals(eventId, readEvent.id);
		assertEquals(2, readEvent.endpoints.size());
	}
	
	/**
	 * Test if the response data (correlation id, response time etc) is merged in the request when the response is added after the request.  
	 * 
	 * @throws InterruptedException 
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testMergingOfResponseAfterRequest() throws InterruptedException {
		final String requestId = UUID.randomUUID().toString();
		final String responseId = UUID.randomUUID().toString();
		final MessagingTelemetryEventPersister persister = new MessagingTelemetryEventPersister(bulkProcessor, etmConfiguration);
		
		final ZonedDateTime timeStamp = ZonedDateTime.now();
		final EndpointHandlerBuilder requestingEndpointHandler = new EndpointHandlerBuilder()
				.setHandlingTime(timeStamp)
				.setApplication(new ApplicationBuilder()
						.setName("Requesting App")
				);
		
		final EndpointHandlerBuilder respondingEndpointHandler = new EndpointHandlerBuilder()
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
				.setWritingEndpointHandler(requestingEndpointHandler)
				.addReadingEndpointHandler(respondingEndpointHandler)
				);
		persister.persist(builder.build(), this.messagingEventConverter);
		waitFor(persister.getElasticIndexName(), "messaging", requestId, 1L);
		
		builder.initialize()
		.setId(responseId)
		.setPayload("Test case " + this.getClass().getName())
		.setPayloadFormat(PayloadFormat.TEXT)
		.setMessagingEventType(MessagingEventType.RESPONSE)
		.setCorrelationId(requestId)
		.addOrMergeEndpoint(new EndpointBuilder()
				.setName("RESPONSE.QUEUE")
				.setWritingEndpointHandler(respondingEndpointHandler.setHandlingTime(timeStamp.plusSeconds(2)))
				.addReadingEndpointHandler(requestingEndpointHandler.setHandlingTime(timeStamp.plusSeconds(3)))
				);
		persister.persist(builder.build(), this.messagingEventConverter);
		waitFor(persister.getElasticIndexName(), "messaging", responseId, 1L);
		GetResponse getResponse = waitFor(persister.getElasticIndexName(), "messaging", requestId, 2L);

		Map<String, Object> sourceAsMap = getResponse.getSourceAsMap();
		assertEquals(1, ((List<String>)sourceAsMap.get("correlations")).size());
		assertEquals(responseId, ((List<String>)sourceAsMap.get("correlations")).get(0));
		
		assertEquals(1, ((List<Map<String, Object>>)sourceAsMap.get("endpoints")).size());
		Map<String, Object> endpointMap = ((List<Map<String, Object>>)sourceAsMap.get("endpoints")).get(0);
		Map<String, Object> readingEndpointHandler = ((List<Map<String, Object>>)endpointMap.get("reading_endpoint_handlers")).get(0);
		assertEquals(1000, readingEndpointHandler.get("response_time"));
		Map<String, Object> writingEndpointHandler = (Map<String, Object>) endpointMap.get("writing_endpoint_handler");
		assertEquals(3000, writingEndpointHandler.get("response_time"));
	}
	
}
