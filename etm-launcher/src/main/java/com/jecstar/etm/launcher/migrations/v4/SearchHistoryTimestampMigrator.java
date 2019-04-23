package com.jecstar.etm.launcher.migrations.v4;

import com.jecstar.etm.launcher.migrations.AbstractEtmMigrator;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetIndexRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.GetMappingsRequestBuilder;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;

import java.util.Map;

/**
 * Migrator that migrates the from and till fields in the search history from dates to string.
 *
 * @since 4.0.0
 */
public class SearchHistoryTimestampMigrator extends AbstractEtmMigrator {

    // TODO - CorrelatedEventsFieldsConverter is gemigreerd van object array naar string array. Dit moet gemigreerd worden.
    // -  Van timestamps in graphs + dashboards moet hard naar String gezet zodat een eerste grafiek met een epoch timestamp er geen number van maakt.
    // - etm_configuratie index migreren naar v7 dus herindexeren.

    private final DataRepository dataRepository;
    private final String migrationIndexPrefix = "migetm_";

    public SearchHistoryTimestampMigrator(final DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean shouldBeExecuted() {
        boolean indexExist = this.dataRepository.indicesExist(new GetIndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        if (!indexExist) {
            return false;
        }
        GetMappingsResponse mappingsResponse = this.dataRepository.indicesGetMappings(new GetMappingsRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        Map<String, MappingMetaData> mappings = mappingsResponse.mappings();
        if (!mappings.containsKey(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)) {
            return false;
        }
//        MappingMetaData mappingMetaData = docTypeMappings.get(ElasticsearchLayout.CONFIGURATION_INDEX_NAME);
//        Map<String, Object> mappingMap = mappingMetaData.getSourceAsMap();
//        if (!mappingMap.containsKey("dynamic_templates")) {
//            return false;
//        }
//        List<Map<String, Object>> templatesList = (List<Map<String, Object>>) mappingMap.get("dynamic_templates");
//        for (Map<String, Object> template : templatesList) {
//            if (template.containsKey("start_time") || template.containsKey("end_time")) {
                // Just checking if this
//                return true;
//            }
//        }
        return false;
    }

    @Override
    public void migrate() {
        if (!shouldBeExecuted()) {
            return;
        }

        checkAndCleanupPreviousRun(this.dataRepository, this.migrationIndexPrefix);

//        Function<SearchHit, DocWriteRequest> processor = searchHit -> {
//            IndexRequestBuilder builder = new IndexRequestBuilder(
//                    SearchHistoryTimestampMigrator.this.migrationIndexPrefix + searchHit.getIndex(),
//                    searchHit.getType(),
//                    searchHit.getId()
//            )
//                    .setSource(searchHit.getSourceAsMap());
//            return builder.build();
//        };


//        FailureDetectingBulkProcessorListener listener = new FailureDetectingBulkProcessorListener();
//        boolean succeeded = migrateEtmConfiguration(this.dataRepository, createBulkProcessor(this.dataRepository, listener), listener, processor);
//        if (!succeeded) {
//            System.out.println("Errors detected. Quitting migration. Migrated indices are prefixed with '" + this.migrationIndexPrefix + "' and are still existent in your Elasticsearch cluster!");
//            return;
//        }
//        flushIndices(this.dataRepository, this.migrationIndexPrefix + "*");
//         Reinitialize template
//        ElasticsearchIndexTemplateCreator indexTemplateCreator = new ElasticsearchIndexTemplateCreator(this.dataRepository);
//        indexTemplateCreator.addConfigurationChangeNotificationListener(new ElasticBackedEtmConfiguration(null, this.dataRepository, this.migrationIndexPrefix + ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
//        indexTemplateCreator.reinitialize();
//
//        deleteIndices(this.dataRepository, "old index", ElasticsearchLayout.CONFIGURATION_INDEX_NAME);
//        reindexTemporaryIndicesToNew(this.dataRepository, listener, this.migrationIndexPrefix);
//        deleteIndices(this.dataRepository, "temporary indices", this.migrationIndexPrefix + "*");
//        checkAndCreateIndexExistence(this.dataRepository,
//                ElasticsearchLayout.CONFIGURATION_INDEX_NAME
//        );
    }

//    private boolean migrateEtmConfiguration(DataRepository dataRepository, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener, Function<SearchHit, DocWriteRequest> processor) {
//        SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
//                .setQuery(QueryBuilders.matchAllQuery())
//                .setTimeout(TimeValue.timeValueSeconds(30))
//                .setFetchSource(true);
//        return migrateEntity(searchRequestBuilder, "configuration", dataRepository, bulkProcessor, listener, processor);
//
//    }

}
