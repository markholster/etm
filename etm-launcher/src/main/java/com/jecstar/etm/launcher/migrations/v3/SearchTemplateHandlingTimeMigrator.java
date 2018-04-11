package com.jecstar.etm.launcher.migrations.v3;

import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import com.jecstar.etm.launcher.http.session.ElasticsearchSessionTags;
import com.jecstar.etm.launcher.http.session.ElasticsearchSessionTagsJsonImpl;
import com.jecstar.etm.launcher.migrations.AbstractEtmMigrator;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalTagsJsonImpl;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Class that migrates all writer and reader handling_time search templates to the new format.
 *
 * @since 3.2.0
 */
public class SearchTemplateHandlingTimeMigrator extends AbstractEtmMigrator {

    private final ElasticsearchSessionTags sessionTags = new ElasticsearchSessionTagsJsonImpl();
    private final EtmPrincipalTags principalTags = new EtmPrincipalTagsJsonImpl();
    private final Client client;
    private final JsonConverter jsonConverter = new JsonConverter();

    public SearchTemplateHandlingTimeMigrator(Client client) {
        this.client = client;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean shouldBeExecuted() {
        IndicesExistsResponse indicesExistsResponse = client.admin().indices().prepareExists(ElasticsearchLayout.CONFIGURATION_INDEX_NAME).get();
        if (!indicesExistsResponse.isExists()) {
            return false;
        }
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER))
                .setTimeout(TimeValue.timeValueMillis(30000));
        ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
        for (SearchHit searchHit : scrollableSearch) {
            Map<String, Object> sourceMap = searchHit.getSourceAsMap();
            Map<String, Object> userMap = this.jsonConverter.getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, sourceMap);
            if (userMap == null) {
                continue;
            }

            List<Map<String, Object>> templates = this.jsonConverter.getArray(this.principalTags.getSearchTemplatesTag(), userMap);
            if (templates != null) {
                for (Map<String, Object> template : templates) {
                    if ("endpoints.writing_endpoint_handler.handling_time".equalsIgnoreCase(this.jsonConverter.getString("sort_field", template))
                            || "endpoints.reading_endpoint_handlers.handling_time".equalsIgnoreCase(this.jsonConverter.getString("sort_field", template))
                            || "endpoints.endpoint_handlers.handling_time".equalsIgnoreCase(this.jsonConverter.getString("sort_field", template))) {
                        return true;
                    }
                    List<Map<String, Object>> fields = this.jsonConverter.getArray("fields", template);
                    if (fields != null) {
                        for (Map<String, Object> field : fields) {
                            if ("endpoints.writing_endpoint_handler.handling_time".equalsIgnoreCase(this.jsonConverter.getString("field", field))
                                    || "endpoints.reading_endpoint_handlers.handling_time".equalsIgnoreCase(this.jsonConverter.getString("field", field))
                                    || "endpoints.endpoint_handlers.handling_time".equalsIgnoreCase(this.jsonConverter.getString("sort_field", field))) {
                                return true;
                            }
                        }
                    }
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
        System.out.println("Start migrating search templates.");
        FailureDetectingBulkProcessorListener listener = new FailureDetectingBulkProcessorListener();
        BulkProcessor bulkProcessor = createBulkProcessor(this.client, listener);


        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER))
                .setTimeout(TimeValue.timeValueMillis(30000));
        ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
        long total = scrollableSearch.getNumberOfHits();
        long current = 0, lastPrint = 0;
        for (SearchHit searchHit : scrollableSearch) {
            Map<String, Object> sourceMap = searchHit.getSourceAsMap();
            Map<String, Object> userMap = this.jsonConverter.getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, sourceMap);
            if (userMap == null) {
                lastPrint = printPercentageWhenChanged(lastPrint, ++current, total);
                continue;
            }
            boolean updated = false;
            List<Map<String, Object>> templates = this.jsonConverter.getArray(this.principalTags.getSearchTemplatesTag(), userMap);
            if (templates != null) {
                for (Map<String, Object> template : templates) {
                    if ("endpoints.writing_endpoint_handler.handling_time".equalsIgnoreCase(this.jsonConverter.getString("sort_field", template))
                            || "endpoints.reading_endpoint_handlers.handling_time".equalsIgnoreCase(this.jsonConverter.getString("sort_field", template))
                            || "endpoints.endpoint_handlers.handling_time".equalsIgnoreCase(this.jsonConverter.getString("sort_field", template))) {
                        template.put("sort_field", "timestamp");
                        updated = true;
                    }
                    List<Map<String, Object>> fields = this.jsonConverter.getArray("fields", template);
                    if (fields != null) {
                        for (Map<String, Object> field : fields) {
                            if ("endpoints.writing_endpoint_handler.handling_time".equalsIgnoreCase(this.jsonConverter.getString("field", field))
                                    || "endpoints.reading_endpoint_handlers.handling_time".equalsIgnoreCase(this.jsonConverter.getString("field", field))
                                    || "endpoints.endpoint_handlers.handling_time".equalsIgnoreCase(this.jsonConverter.getString("field", field))) {
                                field.put("field", "timestamp");
                                updated = true;
                            }
                        }
                    }
                }
            }
            if (updated) {
                bulkProcessor.add(this.client.prepareUpdate(
                        ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, searchHit.getId())
                        .setDoc(sourceMap).request());
            }
            lastPrint = printPercentageWhenChanged(lastPrint, ++current, total);
        }
        bulkProcessor.flush();
        try {
            bulkProcessor.awaitClose(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        printPercentageWhenChanged(lastPrint, current, total);
        System.out.println("Done migrating search templates" + (listener.hasFailures() ? " with failures." : "."));

    }

}