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

package com.jecstar.etm.launcher.migrations.v4;

import com.jecstar.etm.launcher.migrations.AbstractEtmMigrator;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.*;
import com.jecstar.etm.server.core.persisting.RedactedSearchHit;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Migrator that cleans the index templates that are obsolete after the 4.1 migration.
 */
public class Version42Migrator extends AbstractEtmMigrator {

    private final DataRepository dataRepository;
    private final String migrationIndexPrefix = "migetm_";

    public Version42Migrator(final DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    public boolean shouldBeExecuted() {
        var scriptResponse = this.dataRepository.getStoredScript(new GetStoredScriptRequestBuilder("etm_update-search-template"));
        if (scriptResponse != null) {
            return true;
        }
        scriptResponse = this.dataRepository.getStoredScript(new GetStoredScriptRequestBuilder("etm_remove-search-template"));
        if (scriptResponse != null) {
            return true;
        }
        scriptResponse = this.dataRepository.getStoredScript(new GetStoredScriptRequestBuilder("etm_update-search-history"));
        if (scriptResponse != null) {
            return true;
        }
        boolean indexExist = this.dataRepository.indicesExist(new GetIndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        if (indexExist) {
            var response = this.dataRepository.search(new SearchRequestBuilder()
                    .setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                    .setQuery(new BoolQueryBuilder()
                            .should(new TermQueryBuilder("user.roles", "etm_event_read_without_payload"))
                            .should(new TermQueryBuilder("group.roles", "etm_event_read_without_payload"))
                            .minimumShouldMatch(1)
                    )
                    .setFetchSource(false));
            if (response.getHits().getTotalHits().value > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void migrate(boolean forced) {
        if (!shouldBeExecuted() && !forced) {
            return;
        }
        checkAndCleanupPreviousRun(this.dataRepository, this.migrationIndexPrefix);

        System.out.println("Start removing old script templates.");
        long current = 0, lastPrint = 0, total = 3;
        var scriptResponse = this.dataRepository.getStoredScript(new GetStoredScriptRequestBuilder("etm_update-search-template"));
        if (scriptResponse != null) {
            this.dataRepository.deleteStoredScript(new DeleteStoredScriptRequestBuilder("etm_update-search-template"));
        }
        lastPrint = printPercentageWhenChanged(lastPrint, ++current, total);
        scriptResponse = this.dataRepository.getStoredScript(new GetStoredScriptRequestBuilder("etm_remove-search-template"));
        if (scriptResponse != null) {
            this.dataRepository.deleteStoredScript(new DeleteStoredScriptRequestBuilder("etm_remove-search-template"));
        }
        lastPrint = printPercentageWhenChanged(lastPrint, ++current, total);
        scriptResponse = this.dataRepository.getStoredScript(new GetStoredScriptRequestBuilder("etm_update-search-history"));
        if (scriptResponse != null) {
            this.dataRepository.deleteStoredScript(new DeleteStoredScriptRequestBuilder("etm_update-search-history"));
        }
        printPercentageWhenChanged(lastPrint, ++current, total);
        System.out.println("Done removing old script templates.");

        boolean indexExist = this.dataRepository.indicesExist(new GetIndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        if (indexExist) {
            Function<RedactedSearchHit, DocWriteRequest<?>> processor = searchHit -> {
                IndexRequestBuilder builder = new IndexRequestBuilder(
                        Version42Migrator.this.migrationIndexPrefix + searchHit.getIndex(), searchHit.getId()
                ).setSource(determineSource(searchHit));
                return builder.build();
            };

            var listener = new FailureDetectingBulkProcessorListener();
            var succeeded = migrateEtmConfiguration(this.dataRepository, createBulkProcessor(this.dataRepository, listener), listener, processor);
            if (!succeeded) {
                System.out.println("Errors detected. Quitting migration. Migrated indices are prefixed with '" + this.migrationIndexPrefix + "' and are still existent in your Elasticsearch cluster!");
                return;
            }
            flushIndices(this.dataRepository, this.migrationIndexPrefix + "*");

            deleteIndices(this.dataRepository, "old index", ElasticsearchLayout.CONFIGURATION_INDEX_NAME);
            reindexTemporaryIndicesToNew(this.dataRepository, listener, this.migrationIndexPrefix);
            deleteIndices(this.dataRepository, "temporary indices", this.migrationIndexPrefix + "*");
            deleteTemporaryIndexTemplates(this.dataRepository, this.migrationIndexPrefix);
            checkAndCreateIndexExistence(this.dataRepository, ElasticsearchLayout.CONFIGURATION_INDEX_NAME
            );

        }
    }


    @SuppressWarnings("unchecked")
    private Map<String, Object> determineSource(RedactedSearchHit searchHit) {
        Map<String, Object> sourceAsMap = searchHit.getSourceAsMap();
        if (searchHit.getId().startsWith(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX)) {
            // Migrate users
            var objectMap = (Map<String, Object>) sourceAsMap.get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER);
            migrateReadWithoutPayload(objectMap);
        } else if (searchHit.getId().startsWith(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX)) {
            var objectMap = (Map<String, Object>) sourceAsMap.get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP);
            migrateReadWithoutPayload(objectMap);
        }
        return sourceAsMap;
    }

    @SuppressWarnings("unchecked")
    private void migrateReadWithoutPayload(Map<String, Object> objectMap) {
        if (objectMap.containsKey("roles")) {
            var roles = (List<String>) objectMap.get("roles");
            if (roles != null && roles.remove("etm_event_read_without_payload")) {
                roles.add("etm_event_read");
                var deniedFields = new ArrayList<String>();
                deniedFields.add("payload");
                objectMap.put("event_field_denies", deniedFields);
            }
        }
    }

    private boolean migrateEtmConfiguration(DataRepository dataRepository, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener, Function<RedactedSearchHit, DocWriteRequest<?>> processor) {
        SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setQuery(QueryBuilders.matchAllQuery())
                .setTimeout(TimeValue.timeValueSeconds(30))
                .setFetchSource(true);
        return migrateEntity(searchRequestBuilder, "configuration", dataRepository, bulkProcessor, listener, processor);
    }

}
