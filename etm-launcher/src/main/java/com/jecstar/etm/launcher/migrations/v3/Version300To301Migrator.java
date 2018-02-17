package com.jecstar.etm.launcher.migrations.v3;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import com.jecstar.etm.launcher.ElasticBackedEtmConfiguration;
import com.jecstar.etm.launcher.ElasticsearchIndexTemplateCreator;
import com.jecstar.etm.launcher.migrations.AbstractEtmMigrator;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexAction;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Class that migrates the etm_configuration and etm_state to a new mapping.
 *
 * @since 3.0.1
 */
public class Version300To301Migrator extends AbstractEtmMigrator {

    private final JsonConverter jsonConverter = new JsonConverter();
    private final Client client;
    private final String migrationIndexPrefix = "migetm_";

    public Version300To301Migrator(Client client) {
        this.client = client;
    }

    @Override
    public boolean shouldBeExecuted() {
        IndicesExistsResponse indicesExistsResponse = client.admin().indices().prepareExists(ElasticsearchLayout.STATE_INDEX_NAME,
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME).get();
        if (!indicesExistsResponse.isExists()) {
            return false;
        }
        GetMappingsResponse mappingsResponse = this.client.admin().indices().prepareGetMappings(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .addTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                .get();
        ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = mappingsResponse.getMappings();
        for (ObjectObjectCursor<String, ImmutableOpenMap<String, MappingMetaData>> mappingsCursor : mappings) {
            for (ObjectObjectCursor<String, MappingMetaData> mappingMetadataCursor : mappingsCursor.value) {
                if (!ElasticsearchLayout.ETM_DEFAULT_TYPE.equals(mappingMetadataCursor.key)) {
                    continue;
                }
                MappingMetaData mappingMetaData = mappingMetadataCursor.value;
                Map<String, Object> sourceAsMap = mappingMetaData.getSourceAsMap();
                return findFieldWithTextType(sourceAsMap);
            }
        }
        return false;
    }

    private boolean findFieldWithTextType(Map<String, Object> valueMap) {
        valueMap = this.jsonConverter.getObject("properties", valueMap);
        if (valueMap == null) {
            return false;
        }
        for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
            Map<String, Object> entryValues = (Map<String, Object>) entry.getValue();
            if (entryValues.containsKey("properties")) {
                if (findFieldWithTextType(entryValues)) {
                    return true;
                }
            } else {
                if ("text".equalsIgnoreCase((String) entryValues.get("type"))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void migrate() {
        if (!shouldBeExecuted()) {
            return;
        }

        GetIndexResponse indexResponse = this.client.admin().indices().prepareGetIndex().addIndices(this.migrationIndexPrefix + "*").get();
        if (indexResponse != null && indexResponse.getIndices() != null && indexResponse.getIndices().length > 0) {
            System.out.println("Found migration indices from a previous run. Deleting those indices.");
            for (String index : indexResponse.getIndices()) {
                this.client.admin().indices().prepareDelete(index).get();
                System.out.println("Removed migration index '" + index + "'.");
            }
        }

        FailureDetectingBulkProcessorListener listener = new FailureDetectingBulkProcessorListener();
        boolean succeeded = migrateEtmConfiguration(this.client, listener);
        if (succeeded) {
            succeeded = migrateEtmState(this.client, listener);
        }
        if (!succeeded) {
            System.out.println("Errors detected. Quitting migration. Migrated indices are prefixed with '" + this.migrationIndexPrefix + "' and are still existent in your Elasticsearch cluster!");
            return;
        }
        // Reinitialize template
        ElasticsearchIndexTemplateCreator indexTemplateCreator = new ElasticsearchIndexTemplateCreator(client);
        indexTemplateCreator.addConfigurationChangeNotificationListener(new ElasticBackedEtmConfiguration(null, client, this.migrationIndexPrefix + ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        indexTemplateCreator.reinitialize();

        deleteIndices(this.client, "old index", ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.STATE_INDEX_NAME);
        reindexTemporaryIndicesToNew(this.client, listener, this.migrationIndexPrefix);
        deleteIndices(this.client, "temporary indices", this.migrationIndexPrefix + "*");
    }

    private boolean migrateEtmConfiguration(Client client, FailureDetectingBulkProcessorListener listener) {
        return migrateEntity("etm_configuration",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                null,
                this.client,
                createBulkProcessor(this.client, listener),
                listener,
                new DefaultIndexRequestCreator(client));
    }

    private boolean migrateEtmState(Client client, FailureDetectingBulkProcessorListener listener) {
        return migrateEntity("etm_state",
                ElasticsearchLayout.STATE_INDEX_NAME,
                null,
                this.client,
                createBulkProcessor(this.client, listener),
                listener,
                new DefaultIndexRequestCreator(client));
    }

    private boolean migrateEntity(String entityName, String index, String type, Client client, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener, Function<SearchHit, DocWriteRequest> processor) {
        System.out.println("Start migrating " + entityName + " to temporary index.");
        listener.reset();
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(index)
                .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery()))
                .setTimeout(TimeValue.timeValueSeconds(30))
                .setFetchSource(true);
        if (type != null) {
            searchRequestBuilder.setTypes(type);
        }
        ScrollableSearch searchHits = new ScrollableSearch(client, searchRequestBuilder);
        long total = searchHits.getNumberOfHits();
        long current = 0, lastPrint = 0;
        for (SearchHit searchHit : searchHits) {
            bulkProcessor.add(processor.apply(searchHit));
            lastPrint = printPercentageWhenChanged(lastPrint, ++current, total);
        }
        bulkProcessor.flush();
        try {
            bulkProcessor.awaitClose(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        printPercentageWhenChanged(lastPrint, current, total);
        System.out.println("Done migrating " + entityName + (listener.hasFailures() ? " temporary index with failures." : "."));
        return !listener.hasFailures();
    }

    private class DefaultIndexRequestCreator implements Function<SearchHit, DocWriteRequest> {

        private final Client client;

        private DefaultIndexRequestCreator(Client client) {
            this.client = client;
        }

        @Override
        public IndexRequest apply(SearchHit searchHit) {
            Map<String, Object> sourceMap = searchHit.getSourceAsMap();
            Map<String, Object> valueMap = new HashMap<>();
            valueMap.put(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, sourceMap.remove(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME));
            valueMap.put((String) valueMap.get(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME), sourceMap);
            IndexRequestBuilder builder = new IndexRequestBuilder(this.client, IndexAction.INSTANCE)
                    .setIndex(Version300To301Migrator.this.migrationIndexPrefix + searchHit.getIndex())
                    .setType(searchHit.getType())
                    .setId(searchHit.getId())
                    .setSource(valueMap);
            return builder.request();
        }
    }
}
