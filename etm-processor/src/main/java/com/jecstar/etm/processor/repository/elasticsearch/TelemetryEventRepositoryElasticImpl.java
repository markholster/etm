package com.jecstar.etm.processor.repository.elasticsearch;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.SearchHits;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.processor.TelemetryEvent;
import com.jecstar.etm.processor.converter.TelemetryEventConverter;
import com.jecstar.etm.processor.converter.TelemetryEventConverterTags;
import com.jecstar.etm.processor.converter.json.TelemetryEventConverterJsonImpl;
import com.jecstar.etm.processor.processor.IdCorrelationCache;
import com.jecstar.etm.processor.repository.EndpointConfigResult;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;

public class TelemetryEventRepositoryElasticImpl implements TelemetryEventRepository {
	
	private final DateFormat indexDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private final String ETM_EVENT_INDEX_TYPE = "event";
	private final String ETM_SEARCH_INDEX = "etm_all";
	private final TelemetryEventConverter<String> eventConverter = new TelemetryEventConverterJsonImpl();
	private final TelemetryEventConverterTags tags = this.eventConverter.getTags();
	
	
	private final IdCorrelationCache idCorrelations;
	private final Client elasticClient;
	private final EtmConfiguration etmConfiguration;
	
	private BulkRequestBuilder bulkRequest;
	
	public TelemetryEventRepositoryElasticImpl(final IdCorrelationCache idCorrelations, final Client elasticClient, final EtmConfiguration etmConfiguration) {
		this.idCorrelations = idCorrelations;
		this.elasticClient = elasticClient;
		this.indexDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		this.etmConfiguration = etmConfiguration;
	}

	@Override
	public void persistTelemetryEvent(TelemetryEvent event) {
		if (this.bulkRequest == null) {
			this.bulkRequest = this.elasticClient.prepareBulk();
		}
		String index = getElasticIndexName();
		IndexRequest indexRequest = new IndexRequest(index, "event", event.id)
				.consistencyLevel(WriteConsistencyLevel.ONE)
		        .source(this.eventConverter.convert(event));
		this.bulkRequest.add(indexRequest);
		if (this.bulkRequest.numberOfActions() >= this.etmConfiguration.getPersistingBulkSize()) {
			executeBulk();
		}
	}
	
	/**
	 * Gives the name of the elastic index of the given
	 * <code>TelemetryEvent</code>.
	 * 
	 * @return The name of the index.
	 */
	public String getElasticIndexName() {
		return "etm_event_" + this.indexDateFormat.format(new Date());
		
	}


	@Override
	public TelemetryEvent findParent(String sourceId, String application) {
		if (sourceId == null) {
			return null;
		}
		TelemetryEvent parent = null;
		if (application != null) {
			parent = this.idCorrelations.getBySourceIdAndApplication(sourceId, application);
		} else {
			parent = this.idCorrelations.getBySourceId(sourceId);
		}
		if (parent != null) {
			return parent;
		}
		parent = doFindParent(sourceId, application);
		if (parent == null && application != null) {
			return findParent(sourceId, null);
		}
		return parent;
	}


	private TelemetryEvent doFindParent(String id, String application) {
		FilterBuilder filterBuilder = null;
		if (application != null) {
			filterBuilder = FilterBuilders.boolFilter()
					.must(FilterBuilders.termFilter(this.tags.getIdTag(), id))
					.must(FilterBuilders.termFilter(this.tags.getApplicationTag(), application));
		} else {
			filterBuilder = FilterBuilders.termFilter(this.tags.getIdTag(), id);
		}
		SearchHits hits = this.elasticClient.prepareSearch(ETM_SEARCH_INDEX)
			.setTypes(ETM_EVENT_INDEX_TYPE)
			.setPostFilter(filterBuilder)
			.setFrom(0)
			.setSize(1)
			.get()
			.getHits();
		if (hits.totalHits() == 0) {
			return null;
		}
		TelemetryEvent result = new TelemetryEvent();
		this.eventConverter.convert(hits.getAt(0).getSourceAsString(), result);
		return result;
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

	@Override
	public void findEndpointConfig(String endpoint, EndpointConfigResult result) {
		// TODO Auto-generated method stub
		
	}

}
