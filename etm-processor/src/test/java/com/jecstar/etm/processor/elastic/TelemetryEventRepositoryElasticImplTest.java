package com.jecstar.etm.processor.elastic;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.PayloadFormat;
import com.jecstar.etm.core.domain.TelemetryEvent;
import com.jecstar.etm.core.domain.TelemetryEventBuilder;
import com.jecstar.etm.core.domain.Transport;
import com.jecstar.etm.core.domain.converter.TelemetryEventConverterTags;
import com.jecstar.etm.core.domain.converter.json.TelemetryEventConverterTagsJsonImpl;

/**
 * Test class form the <code>TelemetryEventRepositoryElasticImpl</code> class.
 * 
 * @author mark
 */
public class TelemetryEventRepositoryElasticImplTest {

	private static Node node;
	private Client client;
	private final String nodeName = "etm-test";
	private final TelemetryEventConverterTags tags = new TelemetryEventConverterTagsJsonImpl();

	@BeforeClass
	public static void beforeClass() {
		node = new NodeBuilder().settings(Settings.settingsBuilder()
				.put("cluster.name", "Enterprise Telemetry Monitor - Unit Test")
				.put("node.name", "Unit test " + System.getProperty("user.name"))
				.put("http.enabled", false)
				.put("path.conf", "src/main/resources/config"))
				.local(true)
				.node();
	}
	
	@AfterClass
	public static void afterClass() {
		if (node != null) {
			File dataDirectory = new File(node.settings().get("path.data") == null ? "data" : node.settings().get("path.data"));
			node.close();
			deleteDirectory(dataDirectory);
		}
	}
	
	private static boolean deleteDirectory(File path) {
	    if( path.exists() ) {
	      File[] files = path.listFiles();
	      for(int i=0; i<files.length; i++) {
	         if(files[i].isDirectory()) {
	           deleteDirectory(files[i]);
	         }
	         else {
	           files[i].delete();
	         }
	      }
	    }
	    return( path.delete() );
	  }
	
	@Before
	public void before() {
		this.client = node.client();
	}
	
	@After
	public void after() throws ExecutionException {
		this.client.admin().indices().prepareDelete("_all").get();
	}
	
	/**
	 * Test persistence of a single event with a writing and one reading application.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testPersistOneWriterOneReaderSingleEvent() {
		final String id = "1";
		TelemetryEventBuilder builder = new TelemetryEventBuilder();
		TelemetryEvent event = builder.setId(id)
			.setName("Event 1")
			.setPayloadFormat(PayloadFormat.TEXT)
			.setPayload("Testcase testPersistOneWriterOneReaderSingleEvent.")
			.setWritingEndpointHandler(ZonedDateTime.now(), "TestCase", null, null, null)
			.setTransport(Transport.MQ)
			.setExpiry(ZonedDateTime.now().plus(30, ChronoUnit.SECONDS))
			.addReadingEndpointHandler(ZonedDateTime.now(), "TestCase", null, "Server 1", "sy000012")
			.build();
		// TODO add all possible attributes of an event to the test.
		try (TelemetryEventRepositoryElasticImpl repo = new TelemetryEventRepositoryElasticImpl(createSingleCommitConfiguration(), this.client, new MetricRegistry())) {
			repo.persistTelemetryEvent(event);
			
			// Validate all elements.
			GetResponse getResponse = this.client.prepareGet(repo.getElasticIndexName(event), repo.getElasticType(event), id).get();
			Map<String, Object> source = getResponse.getSourceAsMap();
			assertEquals(event.name, source.get(this.tags.getNameTag()));
			assertEquals(event.payload, source.get(this.tags.getPayloadTag()));
			assertEquals(event.payloadFormat.name(), source.get(this.tags.getPayloadFormatTag()));
			List<Map<String, Object>> readingEndpointHandlers = (List<Map<String, Object>>) source.get(this.tags.getReadingEndpointHandlersTag());
			assertEquals(event.readingEndpointHandlers.get(0).handlingTime.toInstant().toEpochMilli(), readingEndpointHandlers.get(0).get(this.tags.getEndpointHandlerHandlingTimeTag()));
			assertEquals(event.readingEndpointHandlers.get(0).application.name, ((Map<String, Object>)readingEndpointHandlers.get(0).get(this.tags.getEndpointHandlerApplicationTag())).get(this.tags.getApplicationNameTag()));
			Map<String, Object> writingEndpointHandler = (Map<String, Object>) source.get(this.tags.getWritingEndpointHandlerTag());
			assertEquals(event.writingEndpointHandler.handlingTime.toInstant().toEpochMilli(), writingEndpointHandler.get(this.tags.getEndpointHandlerHandlingTimeTag()));
			assertEquals(event.writingEndpointHandler.application.name, ((Map<String, Object>)writingEndpointHandler.get(this.tags.getEndpointHandlerApplicationTag())).get(this.tags.getApplicationNameTag()));
		}
	}

	/**
	 * Test persistence of multiple events with the same id. One event with the writing application and 2 events with a reading application.
	 */

	@SuppressWarnings("unchecked")
	@Test
	public void testPersistOneWriterTwoReadersSeparatedEvents() {
		final String id = "2";
		TelemetryEventBuilder builder = new TelemetryEventBuilder();
		TelemetryEvent event = builder.setId(id)
				.setPayloadFormat(PayloadFormat.TEXT)
				.setPayload("Testcase testPersistOneWriterTwoReadersSeparatedEvents.")
				.setWritingEndpointHandler(ZonedDateTime.now(), "TestCase", null, null, null)
				.build();
		try (TelemetryEventRepositoryElasticImpl repo = new TelemetryEventRepositoryElasticImpl(createSingleCommitConfiguration(), this.client, new MetricRegistry())) {
			// Add the event from the writing application.
			repo.persistTelemetryEvent(event);
			
			// Add the event from the first reading application.
			builder.initialize();
			builder.setId(id)
				.setPayloadFormat(PayloadFormat.TEXT)
				.setPayload("Testcase testPersistOneWriterTwoReadersSeparatedEvents.")
				.addReadingEndpointHandler(ZonedDateTime.now(), "Reading app 1", null, null, null);
			repo.persistTelemetryEvent(builder.build());

			// Add the event from the second reading application.
			builder.initialize();
			builder.setId(id)
				.setPayloadFormat(PayloadFormat.TEXT)
				.setPayload("Testcase testPersistOneWriterTwoReadersSeparatedEvents.")
				.addReadingEndpointHandler(ZonedDateTime.now().plus(10, ChronoUnit.SECONDS), "Reading app 2", null, null, null);
			repo.persistTelemetryEvent(builder.build());
			
			GetResponse getResponse = this.client.prepareGet(repo.getElasticIndexName(event), repo.getElasticType(event), id).get();
			Map<String, Object> source = getResponse.getSourceAsMap();
			List<Map<String, Object>> readingEndpointHandlers = (List<Map<String, Object>>) source.get(this.tags.getReadingEndpointHandlersTag());
			assertEquals(2, readingEndpointHandlers.size());
		}
	}
	
	/**
	 * Test the persistence of a request event that is processed before the response event is processed.
	 */
	@Test
	public void testPersistRequestBeforeResponseEvent() {
		final String id_req = "3";
		final String id_rsp = "4";
		final ZonedDateTime requestTime = ZonedDateTime.now();
		final ZonedDateTime responseReadingTime = requestTime.plus(15, ChronoUnit.SECONDS).plus(12, ChronoUnit.MILLIS);
		TelemetryEventBuilder builder = new TelemetryEventBuilder();
		TelemetryEvent event = builder.setId(id_req)
				.setPayloadFormat(PayloadFormat.TEXT)
				.setPayload("Testcase testPersistRequestBeforeResponseEvent Request.")
				.setTransport(Transport.MQ)
				.setPackaging(TelemetryEvent.PACKAGING_MQ_REQUEST)
				.setWritingEndpointHandler(requestTime, "Request writer", null, null, null)
				.setExpiry(requestTime.plus(30, ChronoUnit.SECONDS))
				.addReadingEndpointHandler(requestTime.plus(10, ChronoUnit.MILLIS), "Request reader", null, null, null)
				.build();
		try (TelemetryEventRepositoryElasticImpl repo = new TelemetryEventRepositoryElasticImpl(createSingleCommitConfiguration(), this.client, new MetricRegistry())) {
			// Add the event from the writing and reading application
			repo.persistTelemetryEvent(event);
			GetResponse getResponse = this.client.prepareGet(repo.getElasticIndexName(event), repo.getElasticType(event), id_req).get();
			// When no response is added, the response time should be the expiry time minus the writing handler time.
			long expectedResponseTime = event.expiry.toInstant().toEpochMilli() - event.writingEndpointHandler.handlingTime.toInstant().toEpochMilli();
			long responseTime = ((Number) getResponse.getSourceAsMap().get(this.tags.getResponseTimeTag())).longValue();
			assertEquals(expectedResponseTime, responseTime);
			
			// Add the response from the reading and writing application
			builder.initialize();
			event = builder.setId(id_rsp)
				.setCorrelationId(id_req)
				.setPayloadFormat(PayloadFormat.TEXT)
				.setPayload("Testcase testPersistRequestBeforeResponseEvent Response.")
				.setTransport(Transport.MQ)
				.setPackaging(TelemetryEvent.PACKAGING_MQ_RESPONSE)
				.setWritingEndpointHandler(responseReadingTime.minus(12, ChronoUnit.MILLIS), "Request reader", null, null, null)
				.addReadingEndpointHandler(responseReadingTime, "Request writer", null, null, null)
				.build();
			repo.persistTelemetryEvent(event);
			getResponse = this.client.prepareGet(repo.getElasticIndexName(event), repo.getElasticType(event), id_req).get();
			expectedResponseTime = responseReadingTime.toInstant().toEpochMilli() - requestTime.toInstant().toEpochMilli();
			long readResponseTime = ((Number) getResponse.getSourceAsMap().get(this.tags.getResponseTimeTag())).longValue();
			assertEquals(expectedResponseTime, readResponseTime);
		}		
	}

	/**
	 * Test the persistence of a request event that is processed before the response event is processed.
	 */
	@Test
	public void testPersistResponseBeforeRequestEvent() {
		final String id_req = "5";
		final String id_rsp = "6";
		final ZonedDateTime requestTime = ZonedDateTime.now();
		final ZonedDateTime responseReadingTime = requestTime.plus(15, ChronoUnit.SECONDS).plus(12, ChronoUnit.MILLIS);
		TelemetryEventBuilder builder = new TelemetryEventBuilder();
		TelemetryEvent event = builder.setId(id_rsp)
				.setCorrelationId(id_req)
				.setPayloadFormat(PayloadFormat.TEXT)
				.setPayload("Testcase testPersistResponseBeforeRequestEvent Response.")
				.setTransport(Transport.MQ)
				.setPackaging(TelemetryEvent.PACKAGING_MQ_RESPONSE)
				.setWritingEndpointHandler(responseReadingTime.minus(12, ChronoUnit.MILLIS), "Request reader", null, null, null)
				.addReadingEndpointHandler(responseReadingTime, "Request writer", null, null, null)
				.build();
		try (TelemetryEventRepositoryElasticImpl repo = new TelemetryEventRepositoryElasticImpl(createSingleCommitConfiguration(), this.client, new MetricRegistry())) {
			// Add the response from the reading application
			repo.persistTelemetryEvent(event);
			GetResponse getResponse = this.client.prepareGet(repo.getElasticIndexName(event), repo.getElasticType(event), id_req).get();
			long responseHandlingTime = ((Number) getResponse.getSourceAsMap().get(this.tags.getResponseHandlingTimeTag())).longValue();
			assertEquals(event.readingEndpointHandlers.get(0).handlingTime.toInstant().toEpochMilli(), responseHandlingTime);
			
			// Add the request from the writing application
			builder.initialize();
			event = builder.setId(id_req)
				.setPayloadFormat(PayloadFormat.TEXT)
				.setPayload("Testcase testPersistResponseBeforeRequestEvent Request.")
				.setTransport(Transport.MQ)
				.setPackaging(TelemetryEvent.PACKAGING_MQ_REQUEST)
				.setWritingEndpointHandler(requestTime, "Request writer", null, null, null)
				.setExpiry(requestTime.plus(30, ChronoUnit.SECONDS))
				.addReadingEndpointHandler(requestTime.plus(10, ChronoUnit.MILLIS), "Request reader", null, null, null)
				.build();
			repo.persistTelemetryEvent(event);
			
			getResponse = this.client.prepareGet(repo.getElasticIndexName(event), repo.getElasticType(event), id_req).get();
			long expectedResponseTime = responseReadingTime.toInstant().toEpochMilli() - requestTime.toInstant().toEpochMilli();
			long readResponseTime = ((Number) getResponse.getSourceAsMap().get(this.tags.getResponseTimeTag())).longValue();
			assertEquals(expectedResponseTime, readResponseTime);
		}		
	}
	
	private EtmConfiguration createSingleCommitConfiguration() {
		EtmConfiguration configuration = new ElasticBackedEtmConfiguration(this.nodeName, this.getClass().getName(), this.client);
		configuration.setPersistingBulkSize(1);
		return configuration;
	}

}
