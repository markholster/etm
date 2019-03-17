package com.jecstar.etm.launcher.migrations.v3;

import com.jecstar.etm.launcher.ElasticBackedEtmConfiguration;
import com.jecstar.etm.launcher.ElasticsearchIndexTemplateCreator;
import com.jecstar.etm.launcher.migrations.AbstractEtmMigrator;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetIndexRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.GetMappingsRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.IndexRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Migrator that migrates the from and till fields in the search history from dates to string.
 *
 * @since 3.5.0
 */
public class SearchHistoryTimestampMigrator extends AbstractEtmMigrator {

    private final DataRepository dataRepository;
    private final String migrationIndexPrefix = "migetm_";

    public SearchHistoryTimestampMigrator(final DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean shouldBeExecuted() {
        boolean indexExist = this.dataRepository.indicesExist(new GetIndexRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        if (!indexExist) {
            return false;
        }
        GetMappingsResponse mappingsResponse = this.dataRepository.indicesGetMappings(new GetMappingsRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = mappingsResponse.getMappings();
        if (!mappings.containsKey(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)) {
            return false;
        }
        ImmutableOpenMap<String, MappingMetaData> docTypeMappings = mappings.get(ElasticsearchLayout.CONFIGURATION_INDEX_NAME);
        if (!docTypeMappings.containsKey(ElasticsearchLayout.ETM_DEFAULT_TYPE)) {
            return false;
        }
        MappingMetaData mappingMetaData = docTypeMappings.get(ElasticsearchLayout.ETM_DEFAULT_TYPE);
        Map<String, Object> mappingMap = mappingMetaData.getSourceAsMap();
        if (!mappingMap.containsKey("dynamic_templates")) {
            return false;
        }
        List<Map<String, Object>> templatesList = (List<Map<String, Object>>) mappingMap.get("dynamic_templates");
        for (Map<String, Object> template : templatesList) {
            if (template.containsKey("start_time") || template.containsKey("end_time")) {
                // Just checking if this
                return true;
            }
        }
        return false;
    }

    @Override
    public void migrate() {
        if (!shouldBeExecuted()) {
            return;
        }

        checkAndCleanupPreviousRun(this.dataRepository, this.migrationIndexPrefix);

        Function<SearchHit, DocWriteRequest> processor = searchHit -> {
            IndexRequestBuilder builder = new IndexRequestBuilder(
                    SearchHistoryTimestampMigrator.this.migrationIndexPrefix + searchHit.getIndex(),
                    searchHit.getType(),
                    searchHit.getId()
            )
                    .setSource(searchHit.getSourceAsMap());
            return builder.build();
        };


        FailureDetectingBulkProcessorListener listener = new FailureDetectingBulkProcessorListener();
        boolean succeeded = migrateEtmConfiguration(this.dataRepository, createBulkProcessor(this.dataRepository, listener), listener, processor);
        if (!succeeded) {
            System.out.println("Errors detected. Quitting migration. Migrated indices are prefixed with '" + this.migrationIndexPrefix + "' and are still existent in your Elasticsearch cluster!");
            return;
        }
        flushIndices(this.dataRepository, this.migrationIndexPrefix + "*");
        // Reinitialize template
        ElasticsearchIndexTemplateCreator indexTemplateCreator = new ElasticsearchIndexTemplateCreator(this.dataRepository);
        indexTemplateCreator.addConfigurationChangeNotificationListener(new ElasticBackedEtmConfiguration(null, this.dataRepository, this.migrationIndexPrefix + ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        indexTemplateCreator.reinitialize();

        deleteIndices(this.dataRepository, "old index", ElasticsearchLayout.CONFIGURATION_INDEX_NAME);
        reindexTemporaryIndicesToNew(this.dataRepository, listener, this.migrationIndexPrefix);
        deleteIndices(this.dataRepository, "temporary indices", this.migrationIndexPrefix + "*");
        checkAndCreateIndexExistence(this.dataRepository,
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME
        );
    }

    private boolean migrateEtmConfiguration(DataRepository dataRepository, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener, Function<SearchHit, DocWriteRequest> processor) {
        SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setQuery(QueryBuilders.matchAllQuery())
                .setTimeout(TimeValue.timeValueSeconds(30))
                .setFetchSource(true);
        return migrateEntity(searchRequestBuilder, "configuration", dataRepository, bulkProcessor, listener, processor);

    }

}
