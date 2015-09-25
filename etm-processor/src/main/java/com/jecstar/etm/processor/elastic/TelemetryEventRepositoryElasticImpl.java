package com.jecstar.etm.processor.elastic;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Optional;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.script.ScriptService.ScriptType;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.EndpointHandler;
import com.jecstar.etm.core.domain.TelemetryEvent;
import com.jecstar.etm.core.domain.converter.TelemetryEventConverter;
import com.jecstar.etm.core.domain.converter.TelemetryEventConverterTags;
import com.jecstar.etm.core.domain.converter.json.TelemetryEventConverterJsonImpl;
import com.jecstar.etm.processor.repository.AbstractTelemetryEventRepository;
import com.jecstar.etm.processor.repository.EndpointConfigResult;

public class TelemetryEventRepositoryElasticImpl extends AbstractTelemetryEventRepository {

	private final EtmConfiguration etmConfiguration;
	private final Client elasticClient;
	private final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.YEAR, 4)
			.appendLiteral("-")
			.appendValue(ChronoField.MONTH_OF_YEAR, 2)
			.appendLiteral("-")
			.appendValue(ChronoField.DAY_OF_MONTH, 2).toFormatter().withZone(ZoneId.of("UTC"));
	private final TelemetryEventConverter<String> jsonConverter = new TelemetryEventConverterJsonImpl();
	private final TelemetryEventConverterTags tags = new TelemetryEventConverterTagsElasticImpl();
	private final UpdateScriptBuilder updateScriptBuilder = new UpdateScriptBuilder();

	
	
	private BulkRequestBuilder bulkRequest;


	public TelemetryEventRepositoryElasticImpl(final EtmConfiguration etmConfiguration, final Client elasticClient) {
		this.etmConfiguration = etmConfiguration;
	    this.elasticClient = elasticClient;
    }
	
	@Override
    public void findEndpointConfig(String endpoint, EndpointConfigResult result) {
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
		String index = getElasticIndexName(event);
		String type = getElasticType(event);
		IndexRequest indexRequest = new IndexRequest(index, type, event.id)
				.consistencyLevel(WriteConsistencyLevel.ONE)
		        .source(this.jsonConverter.convert(event, this.tags));
		UpdateRequest updateRequest = new UpdateRequest(index, type, event.id)
		        .script("etm_update-event", ScriptType.FILE, this.updateScriptBuilder.createUpdateParameterMap(event, this.tags))
		        .upsert(indexRequest)
		        .detectNoop(true)
		        .consistencyLevel(WriteConsistencyLevel.ONE)
		        .retryOnConflict(5);
		this.bulkRequest.add(updateRequest);
		
		if (event.isResponse() && event.correlationId != null && event.readingEndpointHandlers.size() > 0) {
			Optional<EndpointHandler> optionalFastestResponse = event.readingEndpointHandlers.stream()
        	.filter(p -> p.handlingTime != null)
        	.sorted((e1, e2) -> e1.handlingTime.compareTo(e2.handlingTime))
        	.findFirst();
			if (optionalFastestResponse.isPresent()) {
				EndpointHandler endpointHandler = optionalFastestResponse.get();
				indexRequest = new IndexRequest(index, type, event.correlationId)
						.consistencyLevel(WriteConsistencyLevel.ONE)
				        .source("{ \"" + this.tags.getResponseHandlingTimeTag() + "\": " + endpointHandler.handlingTime.toInstant().toEpochMilli() + " }");
				updateRequest = new UpdateRequest(index, type, event.correlationId)
				        .script("etm_update-request-with-responsetime", ScriptType.FILE, this.updateScriptBuilder.createRequestUpdateScriptFromResponse(endpointHandler, this.tags))
				        .upsert(indexRequest)
				        .detectNoop(true)
				        .consistencyLevel(WriteConsistencyLevel.ONE)
				        .retryOnConflict(5);
				this.bulkRequest.add(updateRequest);
			}
		}
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
		if (event.isResponse()) {
			// TODO check of dit de juiste index is. De bijbehorende request kan nl in een andere index zitten.
		}
		return "etm_event_" + event.getEventTime().format(this.dateTimeFormatter);
		
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
			BulkResponse bulkResponse = this.bulkRequest.get();
			if (bulkResponse.hasFailures()) {
				throw new RuntimeException(bulkResponse.buildFailureMessage());
			}
		}
		this.bulkRequest = null;
	}
}
