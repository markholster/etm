package com.jecstar.etm.scheduler.retention;

import javax.annotation.ManagedBean;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.jee.configurator.core.SchedulerConfiguration;

@ManagedBean
@Singleton
@Startup
public class RetentionService {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(RetentionService.class);

	@Inject
	@SchedulerConfiguration
	private EtmConfiguration etmConfiguration;

	@Inject
	@SchedulerConfiguration
	private Client elasticClient;
	
	@Schedule(minute="*/5", hour="*", persistent=false)
	public void flushDocuments() {
		if (log.isDebugLevelEnabled()) {
			log.logDebugMessage("Removing events with expired retention.");
		}
		SearchResponse scrollResp = this.elasticClient.prepareSearch("etm_event_all")
		        .setSearchType(SearchType.SCAN)
		        .setScroll(new TimeValue(60000))
		        .setQuery(QueryBuilders.rangeQuery("retention")
		        		.from(0)
		        		.to(System.currentTimeMillis()))
		        .setSize(1000).execute().actionGet();
		
		while (scrollResp.getHits().getHits().length != 0) {
			BulkRequestBuilder bulkDelete = this.elasticClient.prepareBulk();
		    for (SearchHit hit : scrollResp.getHits().getHits()) {
		    	bulkDelete.add(new DeleteRequestBuilder(this.elasticClient)
		    			.setIndex(hit.getIndex())
		    			.setType(hit.getType())
		    			.setId(hit.getId()));
		    }
		    bulkDelete.get();
		    scrollResp = this.elasticClient.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
		}
	}
}
