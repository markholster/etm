package com.jecstar.etm.scheduler.retention;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

import javax.annotation.ManagedBean;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.AliasOrIndex;

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
	
	@Schedule(minute="*/15", hour="*", persistent=false)
	public void flushDocuments() {
		if (log.isDebugLevelEnabled()) {
			log.logDebugMessage("Looking for indices to remove.");
		}		
		SortedMap<String, AliasOrIndex> aliases = elasticClient.admin().cluster()
			    .prepareState().execute()
			    .actionGet().getState()
			    .getMetaData().getAliasAndIndexLookup();
		if (aliases.containsKey("etm_event_all")) {
			AliasOrIndex aliasOrIndex = aliases.get("etm_event_all");
			List<String> indices = new ArrayList<String>();
			aliasOrIndex.getIndices().forEach(c -> indices.add(c.getIndex()));
			if (indices.size() > this.etmConfiguration.getMaxIndexCount()) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Found " + (indices.size() - this.etmConfiguration.getMaxIndexCount()) + " indices to remove.");
				}		
				Collections.sort(indices);
				for (int i = 0; i < indices.size() - this.etmConfiguration.getMaxIndexCount(); i++) {
					if (log.isDebugLevelEnabled()) {
						log.logDebugMessage("Removing index '" + indices.get(i) + "'.");
					}		
					this.elasticClient.admin().indices().prepareDelete(indices.get(i)).get();
				}
			}
		}
	}
}
