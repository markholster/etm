package com.jecstar.etm.launcher.migrations;

import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateAction;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequestBuilder;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesAction;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesRequestBuilder;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateAction;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequestBuilder;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexAction;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

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

    protected BulkProcessor createBulkProcessor(Client client, BulkProcessor.Listener listener) {
        return BulkProcessor.builder(client, listener).setBulkActions(20).build();
    }

    protected void deleteIndices(Client client, String description, String... indices) {
        System.out.println("Start removing " + description + ".");

        GetIndexResponse indexResponse = client.admin().indices().prepareGetIndex().addIndices(indices).get();
        long lastPrint = 0, current = 0;
        long total = indexResponse.getIndices().length;
        if (indexResponse != null && indexResponse.getIndices() != null && indexResponse.getIndices().length > 0) {
            for (String index : indexResponse.getIndices()) {
                client.admin().indices().prepareDelete(index).get();
                lastPrint = printPercentageWhenChanged(lastPrint, ++current, total);
            }
            printPercentageWhenChanged(lastPrint, ++current, total);
        }
        System.out.println("Done removing " + description + ".");
    }

    protected void reindexTemporaryIndicesToNew(Client client, FailureDetectingBulkProcessorListener listener, String migrationIndexPrefix) {
        System.out.println("Start moving temporary indices to permanent indices.");
        GetIndexResponse indexResponse = client.admin().indices().prepareGetIndex().addIndices(migrationIndexPrefix + "*").get();
        if (indexResponse != null && indexResponse.getIndices() != null && indexResponse.getIndices().length > 0) {
            for (String index : indexResponse.getIndices()) {
                System.out.println("Migrating index '" + index + "'.");
                final BulkProcessor bulkProcessor = createBulkProcessor(client, listener);
                SearchRequestBuilder searchRequestBuilder = client.prepareSearch(index)
                        .setQuery(QueryBuilders.matchAllQuery())
                        .setTimeout(TimeValue.timeValueSeconds(30))
                        .setFetchSource(true);
                ScrollableSearch searchHits = new ScrollableSearch(client, searchRequestBuilder);
                long total = searchHits.getNumberOfHits();
                long current = 0, lastPrint = 0;
                for (SearchHit searchHit : searchHits) {
                    bulkProcessor.add(new IndexRequestBuilder(client, IndexAction.INSTANCE)
                            .setIndex(searchHit.getIndex().substring(migrationIndexPrefix.length()))
                            .setType(searchHit.getType())
                            .setId(searchHit.getId())
                            .setSource(searchHit.getSourceAsMap())
                            .request());
                    lastPrint = printPercentageWhenChanged(lastPrint, ++current, total);
                }
                bulkProcessor.flush();
                try {
                    bulkProcessor.awaitClose(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                printPercentageWhenChanged(lastPrint, current, total);
            }
        }
        System.out.println("Done moving temporary indices to permanent indices.");
    }

    protected void createTemporaryIndexTemplate(Client client, String indexPrefix, int shardsPerIndex) {
        deleteTemporaryIndexTemplate(client, indexPrefix);
        new PutIndexTemplateRequestBuilder(client, PutIndexTemplateAction.INSTANCE, indexPrefix)
                .setCreate(true)
                .setPatterns(Collections.singletonList(indexPrefix + "*"))
                .setSettings(Settings.builder()
                        .put("index.number_of_shards", shardsPerIndex)
                        .put("index.number_of_replicas", 0)
                )
                .get();
    }

    protected void deleteTemporaryIndexTemplate(Client client, String indexPrefix) {
        GetIndexTemplatesResponse getResponse = new GetIndexTemplatesRequestBuilder(client, GetIndexTemplatesAction.INSTANCE, indexPrefix).get();
        if (getResponse.getIndexTemplates().size() > 0) {
            new DeleteIndexTemplateRequestBuilder(client, DeleteIndexTemplateAction.INSTANCE, indexPrefix).get();
        }
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
}
