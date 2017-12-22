package com.jecstar.etm.launcher.migrations;

import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import com.jecstar.etm.launcher.http.session.ElasticsearchSessionTagsJsonImpl;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexAction;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.update.UpdateAction;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Class that migrates all Elasticsearch _types to {@link com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout#ETM_DEFAULT_TYPE}.
 * <p>
 * In Elasticsearch 7.0 and later the _type field will be removed. This class migrates all used _types to the default one to be prepared for Elasticsearch 7.0.
 */
public class ReindexToSingleTypeMigration extends AbstractEtmMigrator {

    private final ElasticsearchSessionTagsJsonImpl sessionTags = new ElasticsearchSessionTagsJsonImpl();
    private final Client client;

    public ReindexToSingleTypeMigration(Client client) {
        this.client = client;
    }

    @Override
    public boolean shouldBeExecuted() {
        IndicesExistsResponse indicesExistsResponse = client.admin().indices().prepareExists(ElasticsearchLayout.STATE_INDEX_NAME,
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.METRICS_INDEX_ALIAS_ALL,
                ElasticsearchLayout.AUDIT_LOG_INDEX_ALIAS_ALL).get();
        if (!indicesExistsResponse.isExists()) {
            return false;
        }
        SearchHits searchHits = client.prepareSearch(ElasticsearchLayout.STATE_INDEX_NAME,
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.METRICS_INDEX_ALIAS_ALL,
                ElasticsearchLayout.AUDIT_LOG_INDEX_ALIAS_ALL)
                .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery())
                        .mustNot(QueryBuilders.termQuery("_type", ElasticsearchLayout.ETM_DEFAULT_TYPE)))
                .get().getHits();
        return searchHits.totalHits != 0;
    }

    @Override
    public void migrate() {
        if (!shouldBeExecuted()) {
            return;
        }

        FailureDetectingBulkProcessorListener listener = new FailureDetectingBulkProcessorListener();
        BulkProcessor bulkProcessor = BulkProcessor.builder(this.client, listener).setBulkActions(20).build();

        migrateMetrics(this.client, bulkProcessor, listener);
        migrateHttpSessions(this.client, bulkProcessor, listener);
        migrateAudits(this.client, bulkProcessor, listener);
        migrateLicense(this.client, bulkProcessor, listener);
        migrateUsers(this.client, bulkProcessor, listener);
        migrateGroups(this.client, bulkProcessor, listener);
        migrateParsers(this.client, bulkProcessor, listener);
        migrateEndpoints(this.client, bulkProcessor, listener);
        migrateLdap(this.client, bulkProcessor, listener);
        migrateNodes(this.client, bulkProcessor, listener);
        migrateIIBNodes(this.client, bulkProcessor, listener);
        migrateGraphs(this.client, bulkProcessor, listener);
        migrateDashboards(this.client, bulkProcessor, listener);
        bulkProcessor.close();
    }

    private void migrateMetrics(Client client, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener) {
        migrateEntity("metrics",
                ElasticsearchLayout.METRICS_INDEX_ALIAS_ALL,
                null,
                client,
                bulkProcessor,
                listener,
                new DefaultIndexRequestCreator(client, ElasticsearchLayout.METRICS_OBJECT_TYPE_ETM_NODE, null)
        );
    }

    private void migrateHttpSessions(Client client, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener) {
        Function<SearchHit, DocWriteRequest> requestBuilder = searchHit -> {
            Map<String, Object> sourceMap = searchHit.getSourceAsMap();
            sourceMap.put(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION);
            // Add the id attribute, because the original id is replaced with an prefix.
            sourceMap.put(sessionTags.getIdTag(), searchHit.getId());
            return new IndexRequestBuilder(this.client, IndexAction.INSTANCE)
                    .setIndex(searchHit.getIndex())
                    .setType(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setId(ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION_ID_PREFIX + searchHit.getId())
                    .setSource(sourceMap)
                    .request();
        };

        migrateEntity("http sessions",
                ElasticsearchLayout.STATE_INDEX_NAME,
                null,
                client,
                bulkProcessor,
                listener,
                requestBuilder
        );
    }

    private void migrateAudits(Client client, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener) {
        Function<SearchHit, DocWriteRequest> requestBuilder = searchHit -> {
            Map<String, Object> sourceMap = searchHit.getSourceAsMap();
            sourceMap.put(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, searchHit.getType());
            return new IndexRequestBuilder(this.client, IndexAction.INSTANCE)
                    .setIndex(searchHit.getIndex())
                    .setType(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setId(searchHit.getId())
                    .setSource(sourceMap)
                    .request();
        };

        migrateEntity("audit logs",
                ElasticsearchLayout.AUDIT_LOG_INDEX_ALIAS_ALL,
                null,
                client,
                bulkProcessor,
                listener,
                requestBuilder
        );
    }

    private void migrateLicense(Client client, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener) {
        migrateEntity("license",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "license",
                client,
                bulkProcessor,
                listener,
                new DefaultIndexRequestCreator(
                        client,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LICENSE,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LICENSE + "_"
                )
        );
    }

    private void migrateUsers(Client client, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener) {
        migrateEntity("users",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "user",
                client,
                bulkProcessor,
                listener,
                new DefaultIndexRequestCreator(
                        client,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX
                )
        );
    }

    private void migrateGroups(Client client, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener) {
        migrateEntity("groups",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "group",
                client,
                bulkProcessor,
                listener,
                new DefaultIndexRequestCreator(
                        client,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX
                )
        );
    }

    private void migrateParsers(Client client, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener) {
        migrateEntity("parsers",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "parser",
                client,
                bulkProcessor,
                listener,
                new DefaultIndexRequestCreator(
                        client,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER_ID_PREFIX
                )
        );
    }

    private void migrateEndpoints(Client client, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener) {
        migrateEntity("endpoints",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "endpoint",
                client,
                bulkProcessor,
                listener,
                new DefaultIndexRequestCreator(
                        client,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT_ID_PREFIX
                )
        );
    }

    private void migrateLdap(Client client, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener) {
        migrateEntity("ldap",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "ldap",
                client,
                bulkProcessor,
                listener,
                new DefaultIndexRequestCreator(
                        client,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LDAP,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LDAP_ID_PREFIX
                )
        );
    }

    private void migrateNodes(Client client, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener) {
        migrateEntity("nodes",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "node",
                client,
                bulkProcessor,
                listener,
                new DefaultIndexRequestCreator(
                        client,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE_ID_PREFIX
                )
        );
    }

    private void migrateIIBNodes(Client client, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener) {
        migrateEntity("IIB nodes",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "iib-node",
                client,
                bulkProcessor,
                listener,
                new DefaultIndexRequestCreator(
                        client,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE_ID_PREFIX
                )
        );
    }

    private void migrateGraphs(Client client, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener) {
        Function<SearchHit, DocWriteRequest> requestBuilder = searchHit -> {
            Map<String, Object> params = new HashMap<>();
            params.put("graphs", searchHit.getSourceAsMap().get("graphs"));
            return new UpdateRequestBuilder(client, UpdateAction.INSTANCE)
                    .setIndex(searchHit.getIndex())
                    .setType(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + searchHit.getId())
                    .setScript(new Script(ScriptType.INLINE, "painless",
                            "if (ctx._source.get(\"graphs\") == null) {\n" +
                                    "    ctx._source.put(\"graphs\", new ArrayList());\n" +
                                    "}\n" +
                                    "for (Map graph : ((List)params.graphs)) {\n" +
                                    "    if (! ((List)ctx._source.get(\"graphs\")).stream().anyMatch(p -> ((Map)p).get(\"name\").equals(graph.get(\"name\")))) {\n" +
                                    "        ctx._source.graphs.add(graph);\n" +
                                    "    }\n" +
                                    "}", params))
                    .request();
        };

        migrateEntity("graphs",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "graph",
                client,
                bulkProcessor,
                listener,
                requestBuilder
        );
    }

    private void migrateDashboards(Client client, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener) {
        Function<SearchHit, DocWriteRequest> requestBuilder = searchHit -> {
            Map<String, Object> params = new HashMap<>();
            params.put("dashboards", searchHit.getSourceAsMap().get("dashboards"));
            return new UpdateRequestBuilder(client, UpdateAction.INSTANCE)
                    .setIndex(searchHit.getIndex())
                    .setType(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + searchHit.getId())
                    .setScript(new Script(ScriptType.INLINE, "painless",
                            "if (ctx._source.get(\"dashboards\") == null) {\n" +
                                    "    ctx._source.put(\"dashboards\", new ArrayList());\n" +
                                    "}\n" +
                                    "for (Map dashboard : ((List)params.dashboards)) {\n" +
                                    "    if (! ((List)ctx._source.get(\"dashboards\")).stream().anyMatch(p -> ((Map)p).get(\"name\").equals(dashboard.get(\"name\")))) {\n" +
                                    "        ctx._source.dashboards.add(dashboard);\n" +
                                    "    }\n" +
                                    "}", params))
                    .setScriptedUpsert(true)
                    .request();
        };

        migrateEntity("dashboards",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "dashboard",
                client,
                bulkProcessor,
                listener,
                requestBuilder
        );
    }

    private void migrateEntity(String entityName, String index, String type, Client client, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener, Function<SearchHit, DocWriteRequest> processor) {
        System.out.println("Start migrating " + entityName + ".");
        listener.reset();
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(index)
                .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery())
                        .mustNot(QueryBuilders.termQuery("_type", ElasticsearchLayout.ETM_DEFAULT_TYPE)))
                .setTimeout(TimeValue.timeValueSeconds(30))
                .setFetchSource(true);
        if (type != null) {
            searchRequestBuilder.setTypes(type);
        }
        ScrollableSearch searchHits = new ScrollableSearch(client, searchRequestBuilder);
        long total = searchHits.getNumberOfHits();
        long current = 0, lastPrint = 0;
        for (SearchHit searchHit : searchHits) {
            if (!ElasticsearchLayout.ETM_DEFAULT_TYPE.equals((searchHit.getType()))) {
                bulkProcessor.add(processor.apply(searchHit));
            }
            lastPrint = printPercentageWhenChanged(lastPrint, ++current, total);
        }
        bulkProcessor.flush();
        printPercentageWhenChanged(lastPrint, current, total);
        System.out.println("Done migrating " + entityName + (listener.hasFailures() ? " with failures." : "."));

        if (listener.hasFailures() || total == 0) {
            return;
        }

        System.out.println("Removing old " + entityName + ".");
        searchHits = new ScrollableSearch(client, searchRequestBuilder);
        total = searchHits.getNumberOfHits();
        current = lastPrint = 0;
        for (SearchHit searchHit : searchHits) {
            if (!ElasticsearchLayout.ETM_DEFAULT_TYPE.equals((searchHit.getType()))) {
                bulkProcessor.add(client.prepareDelete(searchHit.getIndex(), searchHit.getType(), searchHit.getId())
                        .request());
            }
            lastPrint = printPercentageWhenChanged(lastPrint, ++current, total);
        }
        bulkProcessor.flush();
        printPercentageWhenChanged(lastPrint, current, total);
        System.out.println("Done removing old " + entityName + (listener.hasFailures() ? " with failures." : "."));
    }


    private class DefaultIndexRequestCreator implements Function<SearchHit, DocWriteRequest> {

        private final Client client;
        private final String etmTypeAttributeName;
        private final String idPrefix;

        private DefaultIndexRequestCreator(Client client, String etmTypeAttributeName, String idPrefix) {
            this.client = client;
            this.etmTypeAttributeName = etmTypeAttributeName;
            this.idPrefix = idPrefix;
        }

        @Override
        public IndexRequest apply(SearchHit searchHit) {
            Map<String, Object> sourceMap = searchHit.getSourceAsMap();
            if (this.etmTypeAttributeName != null) {
                sourceMap.put(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, this.etmTypeAttributeName);
            }
            IndexRequestBuilder builder = new IndexRequestBuilder(this.client, IndexAction.INSTANCE)
                    .setIndex(searchHit.getIndex())
                    .setType(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setSource(sourceMap);
            if (this.idPrefix != null) {
                builder.setId(this.idPrefix + searchHit.getId());
            } else {
                builder.setId(searchHit.getId());
            }
            return builder.request();

        }
    }

    private class FailureDetectingBulkProcessorListener implements BulkProcessor.Listener {

        private boolean hasFailures = false;

        void reset() {
            this.hasFailures = false;
        }

        boolean hasFailures() {
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
