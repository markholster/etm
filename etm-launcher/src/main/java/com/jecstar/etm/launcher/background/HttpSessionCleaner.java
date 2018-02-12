package com.jecstar.etm.launcher.background;

import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import com.jecstar.etm.launcher.http.session.ElasticsearchSessionTags;
import com.jecstar.etm.launcher.http.session.ElasticsearchSessionTagsJsonImpl;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

public class HttpSessionCleaner implements Runnable {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(HttpSessionCleaner.class);

    private final EtmConfiguration etmConfiguration;
    private final Client client;
    private final ElasticsearchSessionTags tags = new ElasticsearchSessionTagsJsonImpl();


    public HttpSessionCleaner(final EtmConfiguration etmConfiguration, final Client client) {
        this.etmConfiguration = etmConfiguration;
        this.client = client;
    }

    @Override
    public void run() {
        if (log.isDebugLevelEnabled()) {
            log.logDebugMessage("Looking for expired http sessions to remove.");
        }
        try {
            // Not using a delete by query because it isn't able to delete in a certain type (by the Java api) and cannot delete documents with version equals to zero.
            SearchRequestBuilder searchRequestBuilder = this.client.prepareSearch(ElasticsearchLayout.STATE_INDEX_NAME).setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setFetchSource(false)
                    .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                    .setQuery(
                            QueryBuilders.boolQuery().must(
                                    QueryBuilders.rangeQuery(this.tags.getLastAccessedTag()).lt(System.currentTimeMillis() - this.etmConfiguration.getSessionTimeout()).from(0L)
                            ).filter(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION))
                    );
            ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
            if (!scrollableSearch.hasNext()) {
                return;
            }
            if (log.isInfoLevelEnabled()) {
                log.logInfoMessage("Found " + scrollableSearch.getNumberOfHits() + " expired http sessions marked for deletion.");
            }
            BulkRequestBuilder bulkRequestBuilder = createBulkRequestBuilder();
            for (SearchHit searchHit : scrollableSearch) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                bulkRequestBuilder.add(
                        this.client.prepareDelete(searchHit.getIndex(), searchHit.getType(), searchHit.getId())
                                .setWaitForActiveShards(getActiveShardCount(this.etmConfiguration))
                                .setTimeout(TimeValue.timeValueMillis(this.etmConfiguration.getQueryTimeout()))
                );
                if (bulkRequestBuilder.numberOfActions() >= 50) {
                    bulkRequestBuilder.get();
                    bulkRequestBuilder = createBulkRequestBuilder();
                }
            }
            if (bulkRequestBuilder.numberOfActions() > 0) {
                bulkRequestBuilder.get();
            }
            if (log.isInfoLevelEnabled()) {
                log.logInfoMessage(scrollableSearch.getNumberOfHits() + " expired http sessions deleted.");
            }
        } catch (Exception e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Failed to clean http sessions", e);
            }
        }
    }

    private BulkRequestBuilder createBulkRequestBuilder() {
        return this.client.prepareBulk()
                .setWaitForActiveShards(getActiveShardCount(this.etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(this.etmConfiguration.getQueryTimeout()));
    }


    private ActiveShardCount getActiveShardCount(EtmConfiguration etmConfiguration) {
        if (-1 == etmConfiguration.getWaitForActiveShards()) {
            return ActiveShardCount.ALL;
        } else if (0 == etmConfiguration.getWaitForActiveShards()) {
            return ActiveShardCount.NONE;
        }
        return ActiveShardCount.from(etmConfiguration.getWaitForActiveShards());
    }


}
