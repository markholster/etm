package com.jecstar.etm.launcher.migrations.v3;

import com.jecstar.etm.launcher.migrations.AbstractEtmMigrator;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.domain.parser.ExpressionParserField;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetIndexRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.GetStoredScriptRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.UpdateRequestBuilder;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import org.elasticsearch.action.admin.cluster.storedscripts.GetStoredScriptResponse;
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
    private final DataRepository dataRepository;

    public EndpointHandlerToSingleListMigrator(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    public boolean shouldBeExecuted() {
        GetStoredScriptResponse response = this.dataRepository.getStoredScript(new GetStoredScriptRequestBuilder("etm_update-event"));
        if (response != null && response.getSource() != null && (response.getSource().getSource().contains("\"reading_endpoint_handlers\"") || response.getSource().getSource().contains("\"writing_endpoint_handler\""))) {
            return true;
        }
        response = this.dataRepository.getStoredScript(new GetStoredScriptRequestBuilder("etm_update-request-with-response"));
        if (response != null && response.getSource() != null && (response.getSource().getSource().contains("\"reading_endpoint_handlers\"") || response.getSource().getSource().contains("\"writing_endpoint_handler\""))) {
            return true;
        }
        boolean indicesExists = this.dataRepository.indicesExist(new GetIndexRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        if (!indicesExists) {
            return false;
        }
        SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT))
                .setTimeout(TimeValue.timeValueMillis(30000));
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder);
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
        SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                .setFetchSource(true)
                .setTimeout(TimeValue.timeValueMillis(30000))
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT));
        ScrollableSearch scrollableSearch = new ScrollableSearch(this.dataRepository, searchRequestBuilder);
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
                this.dataRepository.update(new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, searchHit.getId())
                        .setDoc(sourceAsMap));
            }
        }
        System.out.println("Done migrating to endpoint_handlers.");
    }
}
