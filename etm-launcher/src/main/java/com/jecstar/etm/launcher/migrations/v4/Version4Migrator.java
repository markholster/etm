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

import com.jecstar.etm.launcher.ElasticBackedEtmConfiguration;
import com.jecstar.etm.launcher.ElasticsearchIndexTemplateCreator;
import com.jecstar.etm.launcher.migrations.AbstractEtmMigrator;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.converter.json.EtmConfigurationTagsJsonImpl;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetIndexRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.GetSettingsRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.IndexRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import org.elasticsearch.Version;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Migrator that reindexes the etm_configuration index if the index is created in a v6 elasticsearch index.
 *
 * @since 4.0.0
 */
public class Version4Migrator extends AbstractEtmMigrator {

    // TODO - CorrelatedEventsFieldsConverter is gemigreerd van object array naar string array. Dit moet gemigreerd worden.

    private final DataRepository dataRepository;
    private final String migrationIndexPrefix = "migetm_";
    private final EtmConfigurationTagsJsonImpl configTags = new EtmConfigurationTagsJsonImpl();

    public Version4Migrator(final DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    public boolean shouldBeExecuted() {
        boolean indexExist = this.dataRepository.indicesExist(new GetIndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        if (!indexExist) {
            return false;
        }

        GetSettingsResponse settingsResponse = this.dataRepository.indicesGetSettings(new GetSettingsRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        Version version = Version.indexCreated(settingsResponse.getIndexToSettings().get(ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        return version.before(Version.V_7_0_0);
    }

    @Override
    public void migrate() {
        if (!shouldBeExecuted()) {
            return;
        }

        checkAndCleanupPreviousRun(this.dataRepository, this.migrationIndexPrefix);

        Function<SearchHit, DocWriteRequest<?>> processor = searchHit -> {
            IndexRequestBuilder builder = new IndexRequestBuilder(
                    Version4Migrator.this.migrationIndexPrefix + searchHit.getIndex(), determineId(searchHit.getId())
            ).setSource(determineSource(searchHit));
            return builder.build();
        };

        FailureDetectingBulkProcessorListener listener = new FailureDetectingBulkProcessorListener();
        boolean succeeded = migrateEtmConfiguration(this.dataRepository, createBulkProcessor(this.dataRepository, listener), listener, processor);
        if (!succeeded) {
            System.out.println("Errors detected. Quitting migration. Migrated indices are prefixed with '" + this.migrationIndexPrefix + "' and are still existent in your Elasticsearch cluster!");
            return;
        }
        flushIndices(this.dataRepository, this.migrationIndexPrefix + "*");
//      Reinitialize template
        ElasticsearchIndexTemplateCreator indexTemplateCreator = new ElasticsearchIndexTemplateCreator(this.dataRepository);
        indexTemplateCreator.addConfigurationChangeNotificationListener(new ElasticBackedEtmConfiguration(null, this.dataRepository, this.migrationIndexPrefix + ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        indexTemplateCreator.reinitialize();

        deleteIndices(this.dataRepository, "old index", ElasticsearchLayout.CONFIGURATION_INDEX_NAME);

        reindexTemporaryIndicesToNew(this.dataRepository, listener, this.migrationIndexPrefix);
        deleteIndices(this.dataRepository, "temporary indices", this.migrationIndexPrefix + "*");
        deleteTemporaryIndexTemplates(this.dataRepository, this.migrationIndexPrefix);
        checkAndCreateIndexExistence(this.dataRepository,
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME
        );
    }

    private String determineId(String documentId) {
        if (documentId.startsWith("endpoint_")) {
            return ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE_ID_PREFIX + documentId.substring("endpoint_".length());
        }
        return documentId;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> determineSource(SearchHit searchHit) {
        Map<String, Object> sourceAsMap = searchHit.getSourceAsMap();
        if (searchHit.getId().startsWith(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX) || searchHit.getId().startsWith(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX)) {
            // Migrate users and groups
            Map<String, Object> objectMap;
            if (sourceAsMap.containsKey(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER))
                objectMap = (Map<String, Object>) sourceAsMap.get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER);
            else if (sourceAsMap.containsKey(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP)) {
                objectMap = (Map<String, Object>) sourceAsMap.get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP);
            } else {
                return sourceAsMap;
            }
            if (objectMap.containsKey("roles")) {
                List<String> roles = (List<String>) objectMap.get("roles");
                if (roles.remove("endpoint_settings_read_write")) {
                    roles.add(SecurityRoles.IMPORT_PROFILES_READ_WRITE);
                }
                if (roles.remove("endpoint_settings_read")) {
                    roles.add(SecurityRoles.IMPORT_PROFILES_READ);
                }
            }
        } else if (searchHit.getId().startsWith(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE_ID_PREFIX)) {
            // Migrate nodes
            if (!sourceAsMap.containsKey(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE)) {
                return sourceAsMap;
            }
            Map<String, Object> objectMap = (Map<String, Object>) sourceAsMap.get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE);
            if (objectMap.containsKey("endpoint_configuration_cache_size")) {
                objectMap.put(this.configTags.getImportProfileCacheSizeTag(), objectMap.get("endpoint_configuration_cache_size"));
                objectMap.remove("endpoint_configuration_cache_size");
            }
        }
        return sourceAsMap;
    }

    private boolean migrateEtmConfiguration(DataRepository dataRepository, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener, Function<SearchHit, DocWriteRequest<?>> processor) {
        SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setQuery(QueryBuilders.matchAllQuery())
                .setTimeout(TimeValue.timeValueSeconds(30))
                .setFetchSource(true);
        return migrateEntity(searchRequestBuilder, "configuration", dataRepository, bulkProcessor, listener, processor);
    }

}
