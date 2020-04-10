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

import com.google.common.collect.Lists;
import com.jecstar.etm.gui.rest.services.search.query.AdditionalQueryParameter;
import com.jecstar.etm.gui.rest.services.search.query.EtmQuery;
import com.jecstar.etm.gui.rest.services.search.query.Field;
import com.jecstar.etm.gui.rest.services.search.query.ResultLayout;
import com.jecstar.etm.gui.rest.services.search.query.converter.EtmQueryConverter;
import com.jecstar.etm.launcher.ElasticBackedEtmConfiguration;
import com.jecstar.etm.launcher.ElasticsearchIndexTemplateCreator;
import com.jecstar.etm.launcher.migrations.AbstractEtmMigrator;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetIndexRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.GetMappingsRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.IndexRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Migrator that migrate the users search fields to the layout with additional search parameters.
 */
public class Version41Migrator extends AbstractEtmMigrator {

    private final DataRepository dataRepository;
    private final JsonConverter jsonConverter = new JsonConverter();
    private final String migrationIndexPrefix = "migetm_";
    private final EtmQueryConverter queryConverter = new EtmQueryConverter();
    private final EtmPrincipal migrator = new EtmPrincipal("migrator") {
        @Override
        public boolean maySeeEventPayload() {
            return true;
        }
    };

    public Version41Migrator(final DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    public boolean shouldBeExecuted() {
        boolean indexExist = this.dataRepository.indicesExist(new GetIndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        if (!indexExist) {
            return false;
        }
        var response = this.dataRepository.indicesGetMappings(new GetMappingsRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        var sourceAsMap = response.mappings().get(ElasticsearchLayout.CONFIGURATION_INDEX_NAME).getSourceAsMap();
        var userValues = this.jsonConverter.getObject("user", this.jsonConverter.getObject("properties", sourceAsMap));
        var userProperties = this.jsonConverter.getObject("properties", userValues);
        return !userProperties.containsKey("additional_query_parameters");
    }

    @Override
    public void migrate() {
        if (!shouldBeExecuted()) {
            return;
        }

        checkAndCleanupPreviousRun(this.dataRepository, this.migrationIndexPrefix);

        Function<SearchHit, DocWriteRequest<?>> processor = searchHit -> {
            IndexRequestBuilder builder = new IndexRequestBuilder(
                    Version41Migrator.this.migrationIndexPrefix + searchHit.getIndex(), searchHit.getId()
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
        // Reinitialize template
        var indexTemplateCreator = new ElasticsearchIndexTemplateCreator(this.dataRepository);
        indexTemplateCreator.addConfigurationChangeNotificationListener(new ElasticBackedEtmConfiguration(null, this.dataRepository, this.migrationIndexPrefix + ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        indexTemplateCreator.reinitialize();

        deleteIndices(this.dataRepository, "old index", ElasticsearchLayout.CONFIGURATION_INDEX_NAME);

        reindexTemporaryIndicesToNew(this.dataRepository, listener, this.migrationIndexPrefix);
        deleteIndices(this.dataRepository, "temporary indices", this.migrationIndexPrefix + "*");
        deleteTemporaryIndexTemplates(this.dataRepository, this.migrationIndexPrefix);
        checkAndCreateIndexExistence(this.dataRepository,
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME
        );
        //TODO etm_update-search-template stored script should be removed.
        //TODO etm_remove-search-template stored script should be removed.
        //TODO etm_update-search-history stored script should be removed.
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> determineSource(SearchHit searchHit) {
        Map<String, Object> sourceAsMap = searchHit.getSourceAsMap();
        if (searchHit.getId().startsWith(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX)) {
            // Migrate users
            Map<String, Object> objectMap = (Map<String, Object>) sourceAsMap.get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER);
            // Remove default search range.
            objectMap.remove("default_search_range");

            // Add default query params
            var additionalQueryParams = new ArrayList<Map<String, Object>>();
            var fromObject = new HashMap<String, Object>();
            fromObject.put("field", "timestamp");
            fromObject.put("name", "Start date");
            fromObject.put("default_value", "now-1h");
            fromObject.put("type", "RANGE_START");

            var tillObject = new HashMap<String, Object>();
            tillObject.put("field", "timestamp");
            tillObject.put("name", "End date");
            tillObject.put("default_value", "now");
            tillObject.put("type", "RANGE_END");

            additionalQueryParams.add(fromObject);
            additionalQueryParams.add(tillObject);
            objectMap.put("additional_query_parameters", additionalQueryParams);

            // Migrate search history
            List<Map<String, Object>> searchHistoryValues = this.jsonConverter.getArray("search_history", objectMap);
            if (searchHistoryValues != null) {
                List<Map<String, Object>> newSearchHistoryValues = new ArrayList<>();
                for (var searchHistory : searchHistoryValues) {
                    EtmQuery etmQuery = new EtmQuery();
                    mergeMapToEtmQuery(searchHistory, etmQuery);
                    newSearchHistoryValues.add(this.jsonConverter.toMap(this.queryConverter.write(etmQuery)));
                }
                objectMap.put("search_history", Lists.reverse(newSearchHistoryValues));
            }

            // Migrate search templates
            List<Map<String, Object>> searchTemplateValues = this.jsonConverter.getArray("search_templates", objectMap);
            if (searchTemplateValues != null) {
                List<Map<String, Object>> newSearchTemplateValues = new ArrayList<>();
                for (var searchTemplate : searchTemplateValues) {
                    EtmQuery etmQuery = new EtmQuery();
                    etmQuery.setName(this.jsonConverter.getString("name", searchTemplate));
                    mergeMapToEtmQuery(searchTemplate, etmQuery);
                    newSearchTemplateValues.add(this.jsonConverter.toMap(this.queryConverter.write(etmQuery)));
                }
                objectMap.put("search_templates", newSearchTemplateValues);
            }
        }
        return sourceAsMap;
    }

    private void mergeMapToEtmQuery(Map<String, Object> valueMap, EtmQuery etmQuery) {
        etmQuery.getAdditionalQueryParameters().add(new AdditionalQueryParameter()
                .setName("Start date")
                .setField(this.jsonConverter.getString("time_filter_field", valueMap, "timestamp"))
                .setValue(valueMap.get("start_time"))
                .setFieldType(AdditionalQueryParameter.FieldType.RANGE_START)
        );
        etmQuery.getAdditionalQueryParameters().add(new AdditionalQueryParameter()
                .setName("End date")
                .setField(this.jsonConverter.getString("time_filter_field", valueMap, "timestamp"))
                .setValue(valueMap.get("end_time"))
                .setFieldType(AdditionalQueryParameter.FieldType.RANGE_END)
        );
        etmQuery.getResultLayout()
                .setQuery(this.jsonConverter.getString("query", valueMap, "*"))
                .setMaxResults(this.jsonConverter.getInteger("results_per_page", valueMap, 50))
                .setSortField(this.jsonConverter.getString("sort_field", valueMap, "timestamp"))
                .setSortOrder("asc".equals(this.jsonConverter.getString("sort_order", valueMap)) ? ResultLayout.SortOrder.ASC : ResultLayout.SortOrder.DESC);

        List<Map<String, Object>> fieldValuesList = this.jsonConverter.getArray("fields", valueMap, new ArrayList<>());
        for (var fieldValues : fieldValuesList) {
            Field field = new Field();
            field.setName(this.jsonConverter.getString("name", fieldValues));
            field.setField(this.jsonConverter.getString("field", fieldValues));
            field.setLink(this.jsonConverter.getBoolean("link", fieldValues));
            field.setFormat(Field.Format.safeValueOf(this.jsonConverter.getString("format", fieldValues)));
            field.setArraySelector(Field.ArraySelector.safeValueOf(this.jsonConverter.getString("array", fieldValues)));
            etmQuery.getResultLayout().addField(field, this.migrator);
        }
    }

    private boolean migrateEtmConfiguration(DataRepository dataRepository, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener, Function<SearchHit, DocWriteRequest<?>> processor) {
        SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setQuery(QueryBuilders.matchAllQuery())
                .setTimeout(TimeValue.timeValueSeconds(30))
                .setFetchSource(true);
        return migrateEntity(searchRequestBuilder, "configuration", dataRepository, bulkProcessor, listener, processor);
    }
}
