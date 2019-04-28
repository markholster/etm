package com.jecstar.etm.launcher.migrations.v4;

import com.jecstar.etm.launcher.ElasticBackedEtmConfiguration;
import com.jecstar.etm.launcher.ElasticsearchIndexTemplateCreator;
import com.jecstar.etm.launcher.migrations.AbstractEtmMigrator;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
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

    public Version4Migrator(final DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @SuppressWarnings("unchecked")
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

        Function<SearchHit, DocWriteRequest> processor = searchHit -> {
            IndexRequestBuilder builder = new IndexRequestBuilder(
                    Version4Migrator.this.migrationIndexPrefix + searchHit.getIndex(),
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
//      Reinitialize template
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
