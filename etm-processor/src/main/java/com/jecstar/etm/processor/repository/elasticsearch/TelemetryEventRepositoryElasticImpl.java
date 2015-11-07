package com.jecstar.etm.processor.repository.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.SearchHits;

import com.jecstar.etm.processor.TelemetryEvent;
import com.jecstar.etm.processor.processor.SourceCorrelationCache;
import com.jecstar.etm.processor.repository.EndpointConfigResult;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;

public class TelemetryEventRepositoryElasticImpl implements TelemetryEventRepository {
	
	private final String ETM_SEARCH_INDEX = "etm_all";
	
	private final SourceCorrelationCache sourceCorrelations;
	private final Client elasticClient;
	
	public TelemetryEventRepositoryElasticImpl(final SourceCorrelationCache sourceCorrelations, final Client elasticClient) {
		this.sourceCorrelations = sourceCorrelations;
		this.elasticClient = elasticClient;
	}

	@Override
	public void persistTelemetryEvent(TelemetryEvent telemetryEvent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public TelemetryEvent findParent(String sourceId, String application) {
		if (sourceId == null) {
			return null;
		}
		TelemetryEvent parent = null;
		if (application != null) {
			parent = this.sourceCorrelations.getBySourceIdAndApplication(sourceId, application);
		} else {
			parent = this.sourceCorrelations.getBySourceId(sourceId);
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


	private TelemetryEvent doFindParent(String sourceId, String application) {
		FilterBuilder filterBuilder = null;
		if (application != null) {
			filterBuilder = FilterBuilders.boolFilter().must(FilterBuilders.termFilter("sourceId", sourceId)).must(FilterBuilders.termFilter("application", application));
		} else {
			filterBuilder = FilterBuilders.termFilter("sourceId", sourceId);
		}
		SearchHits hits = this.elasticClient.prepareSearch(ETM_SEARCH_INDEX)
			.setTypes("event")
			.setPostFilter(filterBuilder)
			.setFrom(0)
			.setSize(1)
			.get()
			.getHits();
		if (hits.totalHits() == 0) {
			return null;
		}
		// TODO mapping
		hits.getAt(0).getSource();
		return null;
	}

	@Override
	public void findEndpointConfig(String endpoint, EndpointConfigResult result) {
		// TODO Auto-generated method stub
		
	}

}
