package com.jecstar.etm.launcher.migrations;

import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import com.jecstar.etm.gui.rest.services.search.DefaultSearchTemplates;
import com.jecstar.etm.launcher.ElasticBackedEtmConfiguration;
import com.jecstar.etm.launcher.ElasticsearchIndexTemplateCreator;
import com.jecstar.etm.launcher.http.session.ElasticsearchSessionTags;
import com.jecstar.etm.launcher.http.session.ElasticsearchSessionTagsJsonImpl;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalTagsJsonImpl;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Class that migrates all Elasticsearch _types to {@link com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout#ETM_DEFAULT_TYPE}.
 * <p>
 * In Elasticsearch 7.0 and later the _type field will be removed. This class migrates all used _types to the default one to be prepared for Elasticsearch 7.0.
 * Also the users and groups are migrated to the new roles.
 */
public class ReindexToSingleTypeMigration extends AbstractEtmMigrator {

    private final ElasticsearchSessionTags sessionTags = new ElasticsearchSessionTagsJsonImpl();
    private final EtmPrincipalTags principalTags = new EtmPrincipalTagsJsonImpl();
    private final Client client;
    private final String migrationIndexPrefix = "migetm_";

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

        GetIndexResponse indexResponse = this.client.admin().indices().prepareGetIndex().addIndices(this.migrationIndexPrefix + "*").get();
        if (indexResponse != null && indexResponse.getIndices() != null && indexResponse.getIndices().length > 0) {
            System.out.println("Found migration indices from a previous run. Deleting those indices.");
            for (String index : indexResponse.getIndices()) {
                this.client.admin().indices().prepareDelete(index).get();
                System.out.println("Removed migration index '" + index + "'.");
            }
        }

        FailureDetectingBulkProcessorListener listener = new FailureDetectingBulkProcessorListener();
        boolean succeeded = migrateMetrics(this.client, listener);
        if (succeeded) {
            succeeded = migrateHttpSessions(this.client, listener);
        }
        if (succeeded) {
            succeeded = migrateAudits(this.client, listener);
        }
        if (succeeded) {
            succeeded = migrateLicense(this.client, listener);
        }
        if (succeeded) {
            succeeded = migrateUsers(this.client, listener);
        }
        if (succeeded) {
            succeeded = migrateGroups(this.client, listener);
        }
        if (succeeded) {
            succeeded = migrateParsers(this.client, listener);
        }
        if (succeeded) {
            succeeded = migrateEndpoints(this.client, listener);
        }
        if (succeeded) {
            succeeded = migrateLdap(this.client, listener);
        }
        if (succeeded) {
            succeeded = migrateNodes(this.client, listener);
        }
        if (succeeded) {
            succeeded = migrateIIBNodes(this.client, listener);
        }
        if (succeeded) {
            succeeded = migrateGraphs(this.client, listener);
        }
        if (succeeded) {
            succeeded = migrateDashboards(this.client, listener);
        }
        if (!succeeded) {
            System.out.println("Errors detected. Quitting migration. Migrated indices are prefixed with '" + this.migrationIndexPrefix + "' and are still existent in your Elasticsearch cluster!");
            return;
        }
        deleteOldIndices(this.client);
        reindexTemporaryIndicesToNew(this.client, listener);
        deleteTemporaryIndices(this.client);
    }

    private void deleteOldIndices(Client client) {
        System.out.println("Start removing old indices.");

        ElasticsearchIndexTemplateCreator indexTemplateCreator = new ElasticsearchIndexTemplateCreator(client);
        indexTemplateCreator.addConfigurationChangeNotificationListener(new ElasticBackedEtmConfiguration(null, client, this.migrationIndexPrefix + ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        indexTemplateCreator.reinitialize();

        GetIndexResponse indexResponse = client.admin().indices().prepareGetIndex().addIndices(
                ElasticsearchLayout.METRICS_INDEX_ALIAS_ALL,
                ElasticsearchLayout.STATE_INDEX_NAME,
                ElasticsearchLayout.AUDIT_LOG_INDEX_ALIAS_ALL,
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME
        ).get();
        long lastPrint = 0, current = 0;
        long total = indexResponse.getIndices().length;
        if (indexResponse != null && indexResponse.getIndices() != null && indexResponse.getIndices().length > 0) {
            for (String index : indexResponse.getIndices()) {
                client.admin().indices().prepareDelete(index).get();
                lastPrint = printPercentageWhenChanged(lastPrint, ++current, total);
            }
            printPercentageWhenChanged(lastPrint, ++current, total);
        }
        System.out.println("Done removing old indices.");
    }

    private void reindexTemporaryIndicesToNew(Client client, FailureDetectingBulkProcessorListener listener) {
        System.out.println("Start moving temporary indices to permanent indices.");
        GetIndexResponse indexResponse = client.admin().indices().prepareGetIndex().addIndices(this.migrationIndexPrefix + "*").get();
        if (indexResponse != null && indexResponse.getIndices() != null && indexResponse.getIndices().length > 0) {
            for (String index : indexResponse.getIndices()) {
                System.out.println("Migrating index '" + index + "'.");
                final BulkProcessor bulkProcessor = createBulkProcessor(listener);
                SearchRequestBuilder searchRequestBuilder = client.prepareSearch(index)
                        .setQuery(QueryBuilders.matchAllQuery())
                        .setTimeout(TimeValue.timeValueSeconds(30))
                        .setFetchSource(true);
                ScrollableSearch searchHits = new ScrollableSearch(client, searchRequestBuilder);
                long total = searchHits.getNumberOfHits();
                long current = 0, lastPrint = 0;
                for (SearchHit searchHit : searchHits) {
                    bulkProcessor.add(new IndexRequestBuilder(this.client, IndexAction.INSTANCE)
                            .setIndex(searchHit.getIndex().substring(this.migrationIndexPrefix.length()))
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

    private void deleteTemporaryIndices(Client client) {
        System.out.println("Start removing temporary indices.");
        GetIndexResponse indexResponse = client.admin().indices().prepareGetIndex().addIndices(this.migrationIndexPrefix + "*").get();
        long lastPrint = 0, current = 0;
        long total = indexResponse.getIndices().length;
        if (indexResponse != null && indexResponse.getIndices() != null && indexResponse.getIndices().length > 0) {
            for (String index : indexResponse.getIndices()) {
                client.admin().indices().prepareDelete(index).get();
                lastPrint = printPercentageWhenChanged(lastPrint, ++current, total);
            }
            printPercentageWhenChanged(lastPrint, ++current, total);
        }
        System.out.println("Done removing temporary indices.");
    }

    private boolean migrateMetrics(Client client, FailureDetectingBulkProcessorListener listener) {
        return migrateEntity("metrics",
                ElasticsearchLayout.METRICS_INDEX_ALIAS_ALL,
                null,
                client,
                createBulkProcessor(listener),
                listener,
                new DefaultIndexRequestCreator(client, ElasticsearchLayout.METRICS_OBJECT_TYPE_ETM_NODE, null)
        );
    }

    private boolean migrateHttpSessions(Client client, FailureDetectingBulkProcessorListener listener) {
        Function<SearchHit, DocWriteRequest> requestBuilder = searchHit -> {
            Map<String, Object> sourceMap = searchHit.getSourceAsMap();
            sourceMap.put(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION);
            // Add the id attribute, because the original id is replaced with an prefix.
            sourceMap.put(sessionTags.getIdTag(), searchHit.getId());
            return new IndexRequestBuilder(this.client, IndexAction.INSTANCE)
                    .setIndex(this.migrationIndexPrefix + searchHit.getIndex())
                    .setType(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setId(ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION_ID_PREFIX + searchHit.getId())
                    .setSource(sourceMap)
                    .request();
        };

        return migrateEntity("http sessions",
                ElasticsearchLayout.STATE_INDEX_NAME,
                null,
                client,
                createBulkProcessor(listener),
                listener,
                requestBuilder
        );
    }

    private boolean migrateAudits(Client client, FailureDetectingBulkProcessorListener listener) {
        Function<SearchHit, DocWriteRequest> requestBuilder = searchHit -> {
            Map<String, Object> sourceMap = searchHit.getSourceAsMap();
            sourceMap.put(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, searchHit.getType());
            return new IndexRequestBuilder(this.client, IndexAction.INSTANCE)
                    .setIndex(this.migrationIndexPrefix + searchHit.getIndex())
                    .setType(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setId(searchHit.getId())
                    .setSource(sourceMap)
                    .request();
        };

        return migrateEntity("audit logs",
                ElasticsearchLayout.AUDIT_LOG_INDEX_ALIAS_ALL,
                null,
                client,
                createBulkProcessor(listener),
                listener,
                requestBuilder
        );
    }

    private boolean migrateLicense(Client client, FailureDetectingBulkProcessorListener listener) {
        return migrateEntity("license",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "license",
                client,
                createBulkProcessor(listener),
                listener,
                new DefaultIndexRequestCreator(
                        client,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LICENSE,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LICENSE + "_"
                )
        );
    }

    @SuppressWarnings("unchecked")
    private boolean migrateUsers(Client client, FailureDetectingBulkProcessorListener listener) {
        Function<SearchHit, DocWriteRequest> requestBuilder = searchHit -> {
            Map<String, Object> sourceMap = searchHit.getSourceAsMap();
            sourceMap.put(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER);
            List<String> roles = (List<String>) sourceMap.get(this.principalTags.getRolesTag());
            if (roles != null) {
                sourceMap.put(this.principalTags.getRolesTag(), mapOldRoles(roles));
                sourceMap.put(this.principalTags.getDefaultSearchRangeTag(), 86400000L);
                List<Map<String, Object>> templates = (List<Map<String, Object>>) sourceMap.get(this.principalTags.getSearchTemplatesTag());
                if (templates != null) {
                    for (Map<String, Object> template : templates) {
                        if (DefaultSearchTemplates.TEMPLATE_NAME_EVENTS_OF_LAST_60_MINS.equals(template.get("name"))) {
                            template.put("query", "*");
                            template.put("start_time", "now-1h");
                            template.put("end_time", "now");
                        } else if (DefaultSearchTemplates.TEMPLATE_NAME_EVENTS_OF_TODAY.equals(template.get("name"))) {
                            template.put("query", "*");
                            template.put("start_time", "now/d");
                            template.put("end_time", "now/d");
                        } else if (DefaultSearchTemplates.TEMPLATE_NAME_EVENTS_OF_YESTERDAY.equals(template.get("name"))) {
                            template.put("query", "*");
                            template.put("start_time", "now-1d/d");
                            template.put("end_time", "now-1d/d");
                        }
                    }
                }
            }
            IndexRequestBuilder builder = new IndexRequestBuilder(this.client, IndexAction.INSTANCE)
                    .setIndex(this.migrationIndexPrefix + searchHit.getIndex())
                    .setType(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setSource(sourceMap);
            builder.setId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + searchHit.getId());
            return builder.request();
        };

        return migrateEntity("users",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "user",
                client,
                createBulkProcessor(listener),
                listener,
                requestBuilder
        );
    }

    @SuppressWarnings("unchecked")
    private boolean migrateGroups(Client client, FailureDetectingBulkProcessorListener listener) {
        Function<SearchHit, DocWriteRequest> requestBuilder = searchHit -> {
            Map<String, Object> sourceMap = searchHit.getSourceAsMap();
            sourceMap.put(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP);
            List<String> roles = (List<String>) sourceMap.get(this.principalTags.getRolesTag());
            if (roles != null) {
                sourceMap.put(this.principalTags.getRolesTag(), mapOldRoles(roles));
            }
            IndexRequestBuilder builder = new IndexRequestBuilder(this.client, IndexAction.INSTANCE)
                    .setIndex(this.migrationIndexPrefix + searchHit.getIndex())
                    .setType(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setSource(sourceMap);
            builder.setId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + searchHit.getId());
            return builder.request();
        };
        return migrateEntity("groups",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "group",
                client,
                createBulkProcessor(listener),
                listener,
                requestBuilder
        );
    }

    private boolean migrateParsers(Client client, FailureDetectingBulkProcessorListener listener) {
        return migrateEntity("parsers",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "parser",
                client,
                createBulkProcessor(listener),
                listener,
                new DefaultIndexRequestCreator(
                        client,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER_ID_PREFIX
                )
        );
    }

    private boolean migrateEndpoints(Client client, FailureDetectingBulkProcessorListener listener) {
        return migrateEntity("endpoints",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "endpoint",
                client,
                createBulkProcessor(listener),
                listener,
                new DefaultIndexRequestCreator(
                        client,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT_ID_PREFIX
                )
        );
    }

    private boolean migrateLdap(Client client, FailureDetectingBulkProcessorListener listener) {
        return migrateEntity("ldap",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "ldap",
                client,
                createBulkProcessor(listener),
                listener,
                new DefaultIndexRequestCreator(
                        client,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LDAP,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LDAP_ID_PREFIX
                )
        );
    }

    private boolean migrateNodes(Client client, FailureDetectingBulkProcessorListener listener) {
        return migrateEntity("nodes",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "node",
                client,
                createBulkProcessor(listener),
                listener,
                new DefaultIndexRequestCreator(
                        client,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE_ID_PREFIX
                )
        );
    }

    private boolean migrateIIBNodes(Client client, FailureDetectingBulkProcessorListener listener) {
        return migrateEntity("IIB nodes",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "iib-node",
                client,
                createBulkProcessor(listener),
                listener,
                new DefaultIndexRequestCreator(
                        client,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE_ID_PREFIX
                )
        );
    }

    private boolean migrateGraphs(Client client, FailureDetectingBulkProcessorListener listener) {
        Function<SearchHit, DocWriteRequest> requestBuilder = searchHit -> {
            Map<String, Object> params = new HashMap<>();
            params.put("graphs", searchHit.getSourceAsMap().get("graphs"));
            return new UpdateRequestBuilder(client, UpdateAction.INSTANCE)
                    .setIndex(this.migrationIndexPrefix + searchHit.getIndex())
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

        return migrateEntity("graphs",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "graph",
                client,
                createBulkProcessor(listener),
                listener,
                requestBuilder
        );
    }

    private boolean migrateDashboards(Client client, FailureDetectingBulkProcessorListener listener) {
        Function<SearchHit, DocWriteRequest> requestBuilder = searchHit -> {
            Map<String, Object> params = new HashMap<>();
            params.put("dashboards", searchHit.getSourceAsMap().get("dashboards"));
            return new UpdateRequestBuilder(client, UpdateAction.INSTANCE)
                    .setIndex(this.migrationIndexPrefix + searchHit.getIndex())
                    .setType(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + searchHit.getId())
                    .setScript(new Script(ScriptType.INLINE, "painless",
                            "if (ctx._source.get(\"dashboards\") == null) {\n" +
                                    "    ctx._source.put(\"dashboards\", new ArrayList());\n" +
                                    "}\n" +
                                    "for (Map dashboard : ((List)params.dashboards)) {\n" +
                                    "    if (! ((List)ctx._source.get(\"dashboards\")).stream().anyMatch(p -> ((Map)p).get(\"name\").equals(dashboard.get(\"name\")))) {\n" +
                                    "        for (Map row : ((List)dashboard.get(\"rows\"))) {" +
                                    "            for (Map col : ((List)row.get(\"cols\"))) {" +
                                    "                col.put(\"name\", col.remove(\"graph\"));" +
                                    "            }\n" +
                                    "            if (row.get(\"height\") instanceof String) {" +
                                    "                row.put(\"height\", Integer.parseInt(row.get(\"height\")));" +
                                    "            }\n" +
                                    "        }" +
                                    "        ctx._source.dashboards.add(dashboard);\n" +
                                    "    }\n" +
                                    "}", params))
                    .setScriptedUpsert(true)
                    .request();
        };

        return migrateEntity("dashboards",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "dashboard",
                client,
                createBulkProcessor(listener),
                listener,
                requestBuilder
        );
    }

    private boolean migrateEntity(String entityName, String index, String type, Client client, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener, Function<SearchHit, DocWriteRequest> processor) {
        System.out.println("Start migrating " + entityName + " to temporary index.");
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
        try {
            bulkProcessor.awaitClose(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        printPercentageWhenChanged(lastPrint, current, total);
        System.out.println("Done migrating " + entityName + (listener.hasFailures() ? " temporary index with failures." : "."));
        return !listener.hasFailures();
    }

    /**
     * Map the old roles to the new ones.
     *
     * @param oldRoles A list with old roles.
     * @return A list with new roles that correspond to the old ones.
     */
    private List<String> mapOldRoles(List<String> oldRoles) {
        List<String> roles = new ArrayList<>();
        for (String oldRole : oldRoles) {
            if ("admin".equals(oldRole)) {
                addRoles(roles, SecurityRoles.ALL_READ_WRITE_SECURITY_ROLES);
            } else if ("searcher".equals(oldRole)) {
                addRoles(roles, SecurityRoles.ETM_EVENT_READ);
            } else if ("controller".equals(oldRole)) {
                addRoles(roles, SecurityRoles.USER_DASHBOARD_READ_WRITE);
            } else if ("processor".equals(oldRole)) {
                addRoles(roles, SecurityRoles.ETM_EVENT_WRITE);
            } else if ("iib-admin".equals(oldRole)) {
                addRoles(roles, SecurityRoles.IIB_EVENT_READ_WRITE, SecurityRoles.IIB_NODE_READ_WRITE);
            } else {
                // Unknown old role, pass it along just in case...
                roles.add(oldRole);
            }
        }
        return roles;
    }

    private void addRoles(List<String> currentRoles, String... roles) {
        for (String role : roles) {
            if (!currentRoles.contains(role)) {
                currentRoles.add(role);
            }
        }
    }

    private BulkProcessor createBulkProcessor(BulkProcessor.Listener listener) {
        return BulkProcessor.builder(this.client, listener).setBulkActions(20).build();
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
                    .setIndex(ReindexToSingleTypeMigration.this.migrationIndexPrefix + searchHit.getIndex())
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
