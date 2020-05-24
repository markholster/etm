/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.launcher.background;

import com.jecstar.etm.launcher.http.session.ElasticsearchSessionTags;
import com.jecstar.etm.launcher.http.session.ElasticsearchSessionTagsJsonImpl;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.BulkRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.DeleteRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.persisting.RequestEnhancer;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import org.elasticsearch.index.query.QueryBuilders;

public class HttpSessionCleaner implements Runnable {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(HttpSessionCleaner.class);

    private final EtmConfiguration etmConfiguration;
    private final DataRepository dataRepository;
    private final ElasticsearchSessionTags tags = new ElasticsearchSessionTagsJsonImpl();
    private final RequestEnhancer requestEnhancer;


    public HttpSessionCleaner(final EtmConfiguration etmConfiguration, final DataRepository dataRepository) {
        this.etmConfiguration = etmConfiguration;
        this.dataRepository = dataRepository;
        this.requestEnhancer = new RequestEnhancer(etmConfiguration);
    }

    @Override
    public void run() {
        if (log.isDebugLevelEnabled()) {
            log.logDebugMessage("Looking for expired http sessions to remove.");
        }
        try {
            // Not using a delete by query because it isn't able to delete in a certain type (by the Java api) and cannot delete documents with version equals to zero.
            SearchRequestBuilder searchRequestBuilder = this.requestEnhancer.enhance(
                    new SearchRequestBuilder().setIndices(ElasticsearchLayout.STATE_INDEX_NAME)
            )
                    .setFetchSource(false)
                    .setQuery(
                            QueryBuilders.boolQuery().must(
                                    QueryBuilders.rangeQuery(this.tags.getLastAccessedTag()).lt(System.currentTimeMillis() - this.etmConfiguration.getSessionTimeout()).from(0L)
                            ).filter(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION))
                    );
            ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
            if (!scrollableSearch.hasNext()) {
                return;
            }
            if (log.isInfoLevelEnabled()) {
                log.logInfoMessage("Found " + scrollableSearch.getNumberOfHits() + " expired http sessions marked for deletion.");
            }
            BulkRequestBuilder bulkRequestBuilder = this.requestEnhancer.enhance(new BulkRequestBuilder());
            for (var searchHit : scrollableSearch) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                bulkRequestBuilder.add(
                        this.requestEnhancer.enhance(
                                new DeleteRequestBuilder(searchHit.getIndex(), searchHit.getId())
                        ).build()
                );
                if (bulkRequestBuilder.getNumberOfActions() >= 50) {
                    this.dataRepository.bulk(bulkRequestBuilder);
                    bulkRequestBuilder = this.requestEnhancer.enhance(new BulkRequestBuilder());
                }
            }
            if (bulkRequestBuilder.getNumberOfActions() > 0) {
                this.dataRepository.bulk(bulkRequestBuilder);
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

}
