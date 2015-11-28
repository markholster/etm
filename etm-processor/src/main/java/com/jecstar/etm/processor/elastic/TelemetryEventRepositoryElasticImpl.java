package com.jecstar.etm.processor.elastic;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.script.ScriptService.ScriptType;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.EndpointConfiguration;
import com.jecstar.etm.core.domain.EndpointHandler;
import com.jecstar.etm.core.domain.TelemetryEvent;
import com.jecstar.etm.core.domain.converter.EndpointConfigurationConverter;
import com.jecstar.etm.core.domain.converter.TelemetryEventConverter;
import com.jecstar.etm.core.domain.converter.TelemetryEventConverterTags;
import com.jecstar.etm.core.domain.converter.json.EndpointConfigurationConverterJsonImpl;
import com.jecstar.etm.core.domain.converter.json.AbstractJsonTelemetryEventConverter;
import com.jecstar.etm.processor.repository.AbstractTelemetryEventRepository;

public class TelemetryEventRepositoryElasticImpl extends AbstractTelemetryEventRepository {

	private final String endpointConfigIndexName = "etm_configuration";
	private final String endpointConfigIndexType = "endpoint";
	private final String endpointConfigDefaultId = "default_configuration";
	private final long endpointConfigExpiryTime = 60000;
	private final EtmConfiguration etmConfiguration;
	private final Client elasticClient;
	private final Timer bulkTimer;
	
	private final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.YEAR, 4)
			.appendLiteral("-")
			.appendValue(ChronoField.MONTH_OF_YEAR, 2)
			.appendLiteral("-")
			.appendValue(ChronoField.DAY_OF_MONTH, 2).toFormatter().withZone(ZoneId.of("UTC"));
//	private final TelemetryEventConverter<String> eventConverter = new AbstractJsonTelemetryEventConverter();
//	private final TelemetryEventConverterTags tags = this.eventConverter.getTags();
	private final EndpointConfigurationConverter<String> endpointConfigurationConverter = new EndpointConfigurationConverterJsonImpl();
	private final UpdateScriptBuilder updateScriptBuilder = new UpdateScriptBuilder();
	private final Map<String, EndpointConfiguration> endpointConfigCache = new HashMap<String, EndpointConfiguration>();
	
	private BulkRequestBuilder bulkRequest;
	

	public TelemetryEventRepositoryElasticImpl(final EtmConfiguration etmConfiguration, final Client elasticClient, final MetricRegistry metricRegistry) {
		this.etmConfiguration = etmConfiguration;
	    this.elasticClient = elasticClient;
	    this.bulkTimer = metricRegistry.timer("event-processor-persisting-repository-bulk-update");
    }
	
	@Override
    public void findEndpointConfig(String endpoint, EndpointConfiguration result) {
		result.initialize();
		if (endpoint == null || endpoint.length() == 0) {
			return;
		}
		EndpointConfiguration dbInstance = null;
		if (this.endpointConfigCache.containsKey(endpoint)) {
			EndpointConfiguration potentialMatch = this.endpointConfigCache.get(endpoint);
			if (System.currentTimeMillis() - potentialMatch.retrievalTimestamp < this.endpointConfigExpiryTime) {
				dbInstance = potentialMatch;
			}
		} 
		if (dbInstance == null) {
			// Checking if the index exist isn't necessary because it's the same
			// index as the EmtConfiguration. All those check are in the
			// ElasticBackedEtmConfiguration which is loaded before this class.
			GetResponse endpointResponse = this.elasticClient.prepareGet(this.endpointConfigIndexName, this.endpointConfigIndexType, endpoint).get();
			if (!endpointResponse.isExists()) {
				endpointResponse = this.elasticClient.prepareGet(this.endpointConfigIndexName, this.endpointConfigIndexType, this.endpointConfigDefaultId).get();
			}
			if (endpointResponse.isExists()) {
				dbInstance = this.endpointConfigurationConverter.convert(endpointResponse.getSourceAsString());
				dbInstance.retrievalTimestamp = System.currentTimeMillis();
				this.endpointConfigCache.put(endpoint, dbInstance);
			}
		}
		if (dbInstance != null) {
			result.correlationDataParsers.putAll(dbInstance.correlationDataParsers);
			result.eventNameParsers.addAll(dbInstance.eventNameParsers);
			result.extractionDataParsers.putAll(dbInstance.extractionDataParsers);
			result.readingApplicationParsers.addAll(dbInstance.readingApplicationParsers);
			result.writingApplicationParsers.addAll(dbInstance.writingApplicationParsers);
		}
    }
	
	@Override
    public void close() {
		executeBulk();
    }

	@Override
    protected void startPersist(TelemetryEvent event) {
		if (this.bulkRequest == null) {
			this.bulkRequest = this.elasticClient.prepareBulk();
		}
    }

	@Override
    protected void endPersist() {
		if (this.bulkRequest.numberOfActions() >= this.etmConfiguration.getPersistingBulkSize()) {
			executeBulk();
		}
    }

	@Override
    protected void addTelemetryEvent(TelemetryEvent event) {
//		String index = getElasticIndexName(event);
//		String type = getElasticType(event);
//		IndexRequest indexRequest = new IndexRequest(index, type, event.id)
//				.consistencyLevel(WriteConsistencyLevel.ONE)
//		        .source(this.eventConverter.convert(event));
//		UpdateRequest updateRequest = new UpdateRequest(index, type, event.id)
//		        .script("etm_update-event", ScriptType.FILE, this.updateScriptBuilder.createUpdateParameterMap(event, this.tags))
//		        .upsert(indexRequest)
//		        .detectNoop(true)
//		        .consistencyLevel(WriteConsistencyLevel.ONE)
//		        .retryOnConflict(5);
//		this.bulkRequest.add(updateRequest);
//		
//		if (event.isResponse() && event.correlationId != null && event.readingEndpointHandlers.size() > 0) {
//			Optional<EndpointHandler> optionalFastestResponse = event.readingEndpointHandlers.stream()
//        	.filter(p -> p.handlingTime != null)
//        	.sorted((e1, e2) -> e1.handlingTime.compareTo(e2.handlingTime))
//        	.findFirst();
//			if (optionalFastestResponse.isPresent()) {
//				EndpointHandler endpointHandler = optionalFastestResponse.get();
//				indexRequest = new IndexRequest(index, type, event.correlationId)
//						.consistencyLevel(WriteConsistencyLevel.ONE)
//				        .source("{ \"" + this.tags.getResponseHandlingTimeTag() + "\": " + endpointHandler.handlingTime.toInstant().toEpochMilli() + " }");
//				updateRequest = new UpdateRequest(index, type, event.correlationId)
//				        .script("etm_update-request-with-responsetime", ScriptType.FILE, this.updateScriptBuilder.createRequestUpdateScriptFromResponse(endpointHandler, this.tags))
//				        .upsert(indexRequest)
//				        .detectNoop(true)
//				        .consistencyLevel(WriteConsistencyLevel.ONE)
//				        .retryOnConflict(5);
//				this.bulkRequest.add(updateRequest);
//			}
//		}
    }
	
	/**
	 * Gives the name of the elastic index of the given
	 * <code>TelemetryEvent</code>.
	 * 
	 * @param event
	 *            The <code>TelemetryEvent</code> to determine the elastic index
	 *            name from.
	 * @return The name of the index.
	 */
	public String getElasticIndexName(TelemetryEvent event) {
		return "etm_event_" + this.dateTimeFormatter.format(ZonedDateTime.now());
		
	}
	
	/**
	 * Gives the type within the elastic index of the given
	 * <code>TelemetryEvent</code>.
	 * 
	 * @param event
	 *            The <code>TelemetryEvent</code> to determine the elastic type
	 *            from.
	 * @return The type within the elastic index.
	 */
	public String getElasticType(TelemetryEvent event) {
		return event.payloadFormat.name().toLowerCase();
	}
	
	private void executeBulk() {
		if (this.bulkRequest != null && this.bulkRequest.numberOfActions() > 0) {
			final Context timerContext = this.bulkTimer.time();
			try {
				BulkResponse bulkResponse = this.bulkRequest.get();
				if (bulkResponse.hasFailures()) {
					throw new RuntimeException(bulkResponse.buildFailureMessage());
				}
			} finally {
				timerContext.stop();
			}
		}
		this.bulkRequest = null;
	}
}
