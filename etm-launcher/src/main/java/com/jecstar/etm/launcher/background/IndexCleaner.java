package com.jecstar.etm.launcher.background;

import com.jecstar.etm.launcher.migrations.MultiTypeDetector;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.DeleteIndexRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.GetAliasesRequestBuilder;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.persisting.internal.BusinessEventLogger;
import org.elasticsearch.client.GetAliasesResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IndexCleaner implements Runnable {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(IndexCleaner.class);

    private final EtmConfiguration etmConfiguration;
    private final DataRepository dataRepository;

    public IndexCleaner(final EtmConfiguration etmConfiguration, final DataRepository dataRepository) {
        this.etmConfiguration = etmConfiguration;
        this.dataRepository = dataRepository;
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
            new MultiTypeDetector().detect(this.dataRepository);
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

        GetAliasesResponse aliasesResponse = this.dataRepository.indicesGetAliases(new GetAliasesRequestBuilder().setIndices(indexAlias));
        List<String> indices = new ArrayList<>(aliasesResponse.getAliases().keySet());

        if (indices.size() > maxIndices) {
            if (log.isInfoLevelEnabled()) {
                log.logInfoMessage("Found " + (indices.size() - maxIndices) + " indices to remove in alias '" + indexAlias + "'.");
            }
            Collections.sort(indices);
            for (int i = 0; i < indices.size() - maxIndices; i++) {
                if (log.isInfoLevelEnabled()) {
                    log.logInfoMessage("Removing index '" + indices.get(i) + "'.");
                }
                this.dataRepository.indicesDelete(new DeleteIndexRequestBuilder().setIndices(indices.get(i)));
                BusinessEventLogger.logIndexRemoval(indices.get(i));
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
            }
        }

    }

}
