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

package com.jecstar.etm.launcher.migrations;

import com.jecstar.etm.launcher.ElasticsearchIndexTemplateCreator;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.*;
import com.jecstar.etm.server.core.persisting.RedactedSearchHit;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.client.indices.GetIndexTemplatesResponse;
import org.elasticsearch.client.indices.IndexTemplateMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;

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
        GetIndexRequestBuilder requestBuilder = new GetIndexRequestBuilder(indices);
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
        GetIndexResponse indexResponse = dataRepository.indicesGet(new GetIndexRequestBuilder(migrationIndexPrefix + "*"));
        if (indexResponse != null && indexResponse.getIndices() != null && indexResponse.getIndices().length > 0) {
            for (String index : indexResponse.getIndices()) {
                System.out.println("Migrating index '" + index + "'.");
                final BulkProcessor bulkProcessor = createBulkProcessor(dataRepository, listener);
                SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder()
                        .setIndices(index)
                        .setQuery(QueryBuilders.matchAllQuery())
                        .setTimeout(TimeValue.timeValueSeconds(30))
                        .setFetchSource(true);
                ScrollableSearch searchHits = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
                long total = searchHits.getNumberOfHits();
                long current = 0, lastPrint = 0;
                for (var searchHit : searchHits) {
                    bulkProcessor.add(new IndexRequest()
                            .index(searchHit.getIndex().substring(migrationIndexPrefix.length()))
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
                printPercentageWhenChanged(lastPrint, current, total);
                long startTime = System.currentTimeMillis();
                boolean indexExist = dataRepository.indicesExist(new GetIndexRequestBuilder(index.substring(migrationIndexPrefix.length())));
                if (!indexExist) {
                    if (System.currentTimeMillis() - startTime > 30_000L) {
                        // Wait for 15 seconds for the index to become available.
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    indexExist = dataRepository.indicesExist(new GetIndexRequestBuilder(index.substring(migrationIndexPrefix.length())));
                }
                if (indexExist) {
                    flushIndices(dataRepository, index.substring(migrationIndexPrefix.length()));
                }
            }
        }
        System.out.println("Done moving temporary indices to permanent indices.");
    }

    protected void createTemporaryIndexTemplates(DataRepository dataRepository, String migrationIndexPrefix, int shardsPerIndex) {
        PutIndexTemplateRequestBuilder requestBuilder = new PutIndexTemplateRequestBuilder(migrationIndexPrefix)
                .setCreate(true)
                .setOrder(0)
                .setPatterns(Collections.singletonList(migrationIndexPrefix + "*"))
                .setSettings(Settings.builder()
                        .put("index.number_of_shards", shardsPerIndex)
                        .put("index.number_of_replicas", 0)
                );
        dataRepository.indicesPutTemplate(requestBuilder);

        requestBuilder = new PutIndexTemplateRequestBuilder(migrationIndexPrefix + ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setCreate(true)
                .setOrder(1)
                .setPatterns(Collections.singletonList(migrationIndexPrefix + ElasticsearchLayout.CONFIGURATION_INDEX_NAME))
                .setSettings(Settings.builder()
                        .put("index.number_of_shards", shardsPerIndex)
                        .put("index.number_of_replicas", 0)
                ).setMapping(new ElasticsearchIndexTemplateCreator(null).createEtmConfigurationMapping(), XContentType.JSON);
        dataRepository.indicesPutTemplate(requestBuilder);
    }

    protected void deleteTemporaryIndexTemplates(DataRepository dataRepository, String migrationIndexPrefix) {
        GetIndexTemplatesResponse templatesResponse = dataRepository.indicesGetTemplate(new GetIndexTemplateRequestBuilder());
        if (templatesResponse.getIndexTemplates() == null) {
            return;
        }
        for (IndexTemplateMetaData indexTemplateMetaData : templatesResponse.getIndexTemplates()) {
            if (indexTemplateMetaData.name().startsWith(migrationIndexPrefix)) {
                dataRepository.indicesDeleteTemplate(new DeleteIndexTemplateRequestBuilder().setName(indexTemplateMetaData.name()));
            }
        }
    }

    protected void checkAndCleanupPreviousRun(DataRepository dataRepository, String migrationIndexPrefix) {
        GetIndexResponse indexResponse = dataRepository.indicesGet(new GetIndexRequestBuilder(migrationIndexPrefix + "*"));
        if (indexResponse != null) {
            System.out.println("Found migration indices from a previous run. Deleting those indices.");
            for (String index : indexResponse.getIndices()) {
                dataRepository.indicesDelete(new DeleteIndexRequestBuilder().setIndices(index));
                System.out.println("Removed migration index '" + index + "'.");
            }
        }
        deleteTemporaryIndexTemplates(dataRepository, migrationIndexPrefix);
        createTemporaryIndexTemplates(dataRepository, migrationIndexPrefix, 1);
    }

    protected boolean migrateEntity(SearchRequestBuilder searchRequestBuilder, String entityName, DataRepository dataRepository, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener, Function<RedactedSearchHit, DocWriteRequest<?>> processor) {
        System.out.println("Start migrating " + entityName + " to temporary index.");
        listener.reset();
        ScrollableSearch searchHits = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
        long total = searchHits.getNumberOfHits();
        long current = 0, lastPrint = 0;
        for (var searchHit : searchHits) {
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
            GetIndexRequestBuilder indexRequestBuilder = new GetIndexRequestBuilder(index);
            if (!dataRepository.indicesExist(indexRequestBuilder)) {
                dataRepository.indicesCreate(new CreateIndexRequestBuilder(index));
            }
        }
    }
}
