package com.jecstar.etm.launcher.migrations;

import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.*;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Abstract superclass for all <code>EtmMigrator</code> instances.
 */
public abstract class AbstractEtmMigrator implements EtmMigrator {

    /**
     * Print a progress bar on the console.
     *
     * @param lastPrint The last print percentage.
     * @param current   The current number of tasks executed.
     * @param total     The total number of tasks to be executed.
     * @return The last print percentage.
     */
    protected long printPercentageWhenChanged(long lastPrint, long current, long total) {
        long percentage = Math.round(current * 100.0 / total);
        if (percentage > lastPrint) {
            if (percentage == 100) {
                System.out.println("[##########] 100%");
            } else {
                System.out.print("[");
                for (int i = 0; i < 10; i++) {
                    if (percentage / 10 > i) {
                        System.out.print("#");
                    } else {
                        System.out.print(" ");
                    }
                }
                System.out.print("] " + percentage + "%\r");
                System.out.flush();
            }
        }
        return percentage;
    }

    protected BulkProcessor createBulkProcessor(DataRepository dataRepository, BulkProcessor.Listener listener) {
        BiConsumer<BulkRequest, ActionListener<BulkResponse>> bulkConsumer =
                (request, bulkListener) ->
                        dataRepository.getClient().bulkAsync(request, RequestOptions.DEFAULT, bulkListener);
        return BulkProcessor.builder(bulkConsumer, listener).setBulkActions(20).build();
    }

    protected void deleteIndices(DataRepository dataRepository, String description, String... indices) {
        System.out.println("Start removing " + description + ".");
        GetIndexRequestBuilder requestBuilder = new GetIndexRequestBuilder().setIndices(indices);
        GetIndexResponse indexResponse = dataRepository.indicesGet(requestBuilder);
        long lastPrint = 0, current = 0;
        long total = indexResponse.getIndices().length;
        if (indexResponse.getIndices() != null && indexResponse.getIndices().length > 0) {
            for (String index : indexResponse.getIndices()) {
                dataRepository.indicesDelete(new DeleteIndexRequestBuilder().setIndices(index));
                lastPrint = printPercentageWhenChanged(lastPrint, ++current, total);
            }
            printPercentageWhenChanged(lastPrint, ++current, total);
        }
        System.out.println("Done removing " + description + ".");
    }

    protected void reindexTemporaryIndicesToNew(DataRepository dataRepository, FailureDetectingBulkProcessorListener listener, String migrationIndexPrefix) {
        System.out.println("Start moving temporary indices to permanent indices.");
        GetIndexResponse indexResponse = dataRepository.indicesGet(new GetIndexRequestBuilder().setIndices(migrationIndexPrefix + "*"));
        if (indexResponse != null && indexResponse.getIndices() != null && indexResponse.getIndices().length > 0) {
            for (String index : indexResponse.getIndices()) {
                System.out.println("Migrating index '" + index + "'.");
                final BulkProcessor bulkProcessor = createBulkProcessor(dataRepository, listener);
                SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder()
                        .setIndices(index)
                        .setQuery(QueryBuilders.matchAllQuery())
                        .setTimeout(TimeValue.timeValueSeconds(30))
                        .setFetchSource(true);
                ScrollableSearch searchHits = new ScrollableSearch(dataRepository, searchRequestBuilder);
                long total = searchHits.getNumberOfHits();
                long current = 0, lastPrint = 0;
                for (SearchHit searchHit : searchHits) {
                    bulkProcessor.add(new IndexRequest()
                            .index(searchHit.getIndex().substring(migrationIndexPrefix.length()))
                            .type(searchHit.getType())
                            .id(searchHit.getId())
                            .source(searchHit.getSourceAsMap()));
                    lastPrint = printPercentageWhenChanged(lastPrint, ++current, total);
                }
                bulkProcessor.flush();
                try {
                    bulkProcessor.awaitClose(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                flushIndices(dataRepository, index);
                printPercentageWhenChanged(lastPrint, current, total);
            }
        }
        System.out.println("Done moving temporary indices to permanent indices.");
    }

    protected void createTemporaryIndexTemplate(DataRepository dataRepository, String indexPrefix, int shardsPerIndex) {
        deleteTemporaryIndexTemplate(dataRepository, indexPrefix);
        PutIndexTemplateRequestBuilder requestBuilder = new PutIndexTemplateRequestBuilder()
                .setName(indexPrefix)
                .setCreate(true)
                .setPatterns(Collections.singletonList(indexPrefix + "*"))
                .setSettings(Settings.builder()
                        .put("index.number_of_shards", shardsPerIndex)
                        .put("index.number_of_replicas", 0)
                );
        dataRepository.indicesPutTemplate(requestBuilder);
    }

    protected void deleteTemporaryIndexTemplate(DataRepository dataRepository, String indexPrefix) {
        boolean templatesExist = dataRepository.indicesTemplatesExist(new IndexTemplatesExistRequestBuilder(indexPrefix));
        if (templatesExist) {
            dataRepository.indicesDeleteTemplate(new DeleteIndexTemplateRequestBuilder().setName(indexPrefix));
        }
    }

    protected void checkAndCleanupPreviousRun(DataRepository dataRepository, String migrationIndexPrefix) {
        GetIndexResponse indexResponse = dataRepository.indicesGet(new GetIndexRequestBuilder().setIndices(migrationIndexPrefix + "*"));
        if (indexResponse != null && indexResponse.getIndices() != null && indexResponse.getIndices().length > 0) {
            System.out.println("Found migration indices from a previous run. Deleting those indices.");
            for (String index : indexResponse.getIndices()) {
                dataRepository.indicesDelete(new DeleteIndexRequestBuilder().setIndices(index));
                System.out.println("Removed migration index '" + index + "'.");
            }
            deleteTemporaryIndexTemplate(dataRepository, migrationIndexPrefix);
        }
        createTemporaryIndexTemplate(dataRepository, migrationIndexPrefix, 1);
    }

    protected boolean migrateEntity(SearchRequestBuilder searchRequestBuilder, String entityName, DataRepository dataRepository, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener, Function<SearchHit, DocWriteRequest> processor) {
        System.out.println("Start migrating " + entityName + " to temporary index.");
        listener.reset();
        ScrollableSearch searchHits = new ScrollableSearch(dataRepository, searchRequestBuilder);
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

    protected void flushIndices(DataRepository dataRepository, String... indices) {
        dataRepository.indicesFlush(new FlushIndexRequestBuilder().setIndices(indices).setForce(true));
    }

    public class FailureDetectingBulkProcessorListener implements BulkProcessor.Listener {

        private boolean hasFailures = false;

        public void reset() {
            this.hasFailures = false;
        }

        public boolean hasFailures() {
            return this.hasFailures;
        }

        @Override
        public void beforeBulk(long executionId, BulkRequest request) {

        }

        @Override
        public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
            if (response.hasFailures()) {
                this.hasFailures = true;
                System.out.println(response.buildFailureMessage());
            }
        }

        @Override
        public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
            this.hasFailures = true;
            failure.printStackTrace();
        }
    }

    protected void checkAndCreateIndexExistence(DataRepository dataRepository, String... indices) {
        for (String index : indices) {
            GetIndexRequestBuilder indexRequestBuilder = new GetIndexRequestBuilder().setIndices(index);
            if (!dataRepository.indicesExist(indexRequestBuilder)) {
                dataRepository.indicesCreate(new CreateIndexRequestBuilder().setIndex(index));
            }
        }
    }
}
