package com.jecstar.etm.launcher.retention;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.AliasOrIndex;

import com.jecstar.etm.processor.internal.persisting.BusinessEventLogger;
import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

public class IndexCleaner implements Runnable {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(IndexCleaner.class);

	private final EtmConfiguration etmConfiguration;
	private final Client client;

	public IndexCleaner(EtmConfiguration etmConfiguration, Client client) {
		this.etmConfiguration = etmConfiguration;
		this.client = client;
	}
	
	@Override
	public void run() {
		try {
			cleanupIndex(ElasticSearchLayout.ETM_EVENT_INDEX_ALIAS_ALL, this.etmConfiguration.getMaxEventIndexCount());
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
			cleanupIndex(ElasticSearchLayout.ETM_METRICS_INDEX_ALIAS_ALL, this.etmConfiguration.getMaxMetricsIndexCount());
		} catch (Exception e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Failed to clean indices", e);
			}
 		}
	}
	
	private void cleanupIndex(String indexAlias, int maxIndices) {
		if (log.isDebugLevelEnabled()) {
			log.logDebugMessage("Looking for indices in alias '" + indexAlias + "' to remove.");
		}		
		SortedMap<String, AliasOrIndex> aliases = this.client.admin().cluster()
			    .prepareState().execute()
			    .actionGet().getState()
			    .getMetaData().getAliasAndIndexLookup();
		if (aliases.containsKey(indexAlias)) {
			AliasOrIndex aliasOrIndex = aliases.get(indexAlias);
			List<String> indices = new ArrayList<>();
			aliasOrIndex.getIndices().forEach(c -> indices.add(c.getIndex().getName()));
			if (indices.size() > maxIndices) {
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Found " + (indices.size() - maxIndices) + " indices to remove in alias '" + indexAlias + "'.");
				}		
				Collections.sort(indices);
				for (int i = 0; i < indices.size() - maxIndices; i++) {
					if (log.isInfoLevelEnabled()) {
						log.logInfoMessage("Removing index '" + indices.get(i) + "'.");
					}		
					this.client.admin().indices().prepareDelete(indices.get(i)).get();
					BusinessEventLogger.logIndexRemoval(indices.get(i));
					if (Thread.currentThread().isInterrupted()) {
						return;
					}
				}
			}
		}		
	}

}
