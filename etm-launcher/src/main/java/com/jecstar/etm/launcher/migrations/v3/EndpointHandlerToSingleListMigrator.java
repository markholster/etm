package com.jecstar.etm.launcher.migrations.v3;

import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import com.jecstar.etm.launcher.migrations.AbstractEtmMigrator;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.domain.parser.ExpressionParserField;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import java.util.List;
import java.util.Map;

/**
 * Class migrating all items to make use of the new location of the <code>EndpointHandler</code>s on the <code>Endpoint</code>.
 *
 * @since 3.1.0
 */
public class EndpointHandlerToSingleListMigrator extends AbstractEtmMigrator {

    private final JsonConverter jsonConverter = new JsonConverter();
    private final Client client;

    public EndpointHandlerToSingleListMigrator(Client client) {
        this.client = client;
    }

    @Override
    public boolean shouldBeExecuted() {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT))
                .setTimeout(TimeValue.timeValueMillis(30000));
        ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
        for (SearchHit searchHit : scrollableSearch) {
            Map<String, Object> endpointMap = this.jsonConverter.getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT, searchHit.getSourceAsMap());
            if (endpointMap == null) {
                continue;
            }
            Map<String, Object> enhancerMap = this.jsonConverter.getObject("enhancer", endpointMap);
            if (enhancerMap == null) {
                continue;
            }
            if (!"DEFAULT".equals(this.jsonConverter.getString("type", enhancerMap))) {
                continue;
            }
            List<Map<String, Object>> fieldsMap = this.jsonConverter.getArray("fields", enhancerMap);
            if (fieldsMap == null) {
                continue;
            }
            for (Map<String, Object> fieldMap : fieldsMap) {
                String field = this.jsonConverter.getString("field", fieldMap);
                if (field == null) {
                    continue;
                }
                if (field.startsWith("endpoints.reading_endpoint_handler.") || field.startsWith("endpoints.writing_endpoint_handler.")) {
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
        System.out.println("Migrating reading_endpoint_handlers and writing_endpoint_handler to endpoint_handlers.");
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT));
        ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
        for (SearchHit searchHit : scrollableSearch) {
            boolean changed = false;
            Map<String, Object> sourceAsMap = searchHit.getSourceAsMap();
            Map<String, Object> endpointMap = this.jsonConverter.getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT, sourceAsMap);
            if (endpointMap == null) {
                continue;
            }
            Map<String, Object> enhancerMap = this.jsonConverter.getObject("enhancer", endpointMap);
            if (enhancerMap == null) {
                continue;
            }
            if (!"DEFAULT".equals(this.jsonConverter.getString("type", enhancerMap))) {
                continue;
            }
            List<Map<String, Object>> fieldsMap = this.jsonConverter.getArray("fields", enhancerMap);
            if (fieldsMap == null) {
                continue;
            }
            for (Map<String, Object> fieldMap : fieldsMap) {
                String field = this.jsonConverter.getString("field", fieldMap);
                if (field == null) {
                    continue;
                }
                if (field.equals("endpoints.writing_endpoint_handler.transaction_id")) {
                    fieldMap.put("field", ExpressionParserField.WRITER_TRANSACTION_ID.getJsonTag());
                    changed = true;
                } else if (field.equals("endpoints.reading_endpoint_handler.transaction_id")) {
                    fieldMap.put("field", ExpressionParserField.READER_TRANSACTION_ID.getJsonTag());
                    changed = true;
                } else if (field.startsWith("endpoints.writing_endpoint_handler.metadata.")) {
                    String key = field.substring("endpoints.writing_endpoint_handler.metadata.".length());
                    fieldMap.put("field", ExpressionParserField.WRITER_METADATA + key);
                    changed = true;
                } else if (field.startsWith("endpoints.reading_endpoint_handler.metadata.")) {
                    String key = field.substring("endpoints.reading_endpoint_handler.metadata.".length());
                    fieldMap.put("field", ExpressionParserField.READER_METADATA + key);
                    changed = true;
                }
            }
            if (changed) {
                client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, searchHit.getId())
                        .setDoc(sourceAsMap)
                        .get();
            }
        }
        System.out.println("Done migrating to endpoint_handlers.");
    }
}
