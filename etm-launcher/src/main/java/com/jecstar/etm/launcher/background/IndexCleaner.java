package com.jecstar.etm.launcher.background;

import com.jecstar.etm.launcher.migrations.MultiTypeDetector;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.persisting.internal.BusinessEventLogger;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.AliasOrIndex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

public class IndexCleaner implements Runnable {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(IndexCleaner.class);

    private final EtmConfiguration etmConfiguration;
    private final Client client;

    public IndexCleaner(final EtmConfiguration etmConfiguration, final Client client) {
        this.etmConfiguration = etmConfiguration;
        this.client = client;
    }

    @Override
    public void run() {
        try {
            cleanupIndex(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL, this.etmConfiguration.getMaxEventIndexCount());
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            cleanupIndex(ElasticsearchLayout.METRICS_INDEX_ALIAS_ALL, this.etmConfiguration.getMaxMetricsIndexCount());
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            cleanupIndex(ElasticsearchLayout.AUDIT_LOG_INDEX_ALIAS_ALL, this.etmConfiguration.getMaxAuditLogIndexCount());
            new MultiTypeDetector().detect(this.client);
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
                .prepareState()
                .get()
                .getState()
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
