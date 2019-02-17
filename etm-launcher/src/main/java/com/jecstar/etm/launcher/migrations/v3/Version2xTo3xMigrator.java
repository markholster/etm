package com.jecstar.etm.launcher.migrations.v3;

import com.jecstar.etm.gui.rest.services.search.DefaultSearchTemplates;
import com.jecstar.etm.launcher.ElasticBackedEtmConfiguration;
import com.jecstar.etm.launcher.ElasticsearchIndexTemplateCreator;
import com.jecstar.etm.launcher.http.session.ElasticsearchSessionTags;
import com.jecstar.etm.launcher.http.session.ElasticsearchSessionTagsJsonImpl;
import com.jecstar.etm.launcher.migrations.AbstractEtmMigrator;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalTagsJsonImpl;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.*;
import com.jecstar.etm.server.core.util.DateUtils;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;

/**
 * Class that migrates all Elasticsearch _types to {@link com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout#ETM_DEFAULT_TYPE}.
 * <p>
 * In Elasticsearch 7.0 and later the _type field will be removed. This class migrates all used _types to the default one to be prepared for Elasticsearch 7.0.
 * Also the users and groups are migrated to the new roles.
 *
 * @since 3.0.0
 */
public class Version2xTo3xMigrator extends AbstractEtmMigrator {

    private final ElasticsearchSessionTags sessionTags = new ElasticsearchSessionTagsJsonImpl();
    private final EtmPrincipalTags principalTags = new EtmPrincipalTagsJsonImpl();
    private final DataRepository dataRepository;
    private final String migrationIndexPrefix = "migetm_";

    public Version2xTo3xMigrator(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    public boolean shouldBeExecuted() {
        GetIndexRequestBuilder getIndexRequestBuilder = new GetIndexRequestBuilder().setIndices(ElasticsearchLayout.STATE_INDEX_NAME,
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.METRICS_INDEX_ALIAS_ALL,
                ElasticsearchLayout.AUDIT_LOG_INDEX_ALIAS_ALL);

        boolean exists = this.dataRepository.indicesExist(getIndexRequestBuilder);
        if (!exists) {
            return false;
        }
        SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder()
                .setIndices((ElasticsearchLayout.STATE_INDEX_NAME),
                        ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                        ElasticsearchLayout.METRICS_INDEX_ALIAS_ALL,
                        ElasticsearchLayout.AUDIT_LOG_INDEX_ALIAS_ALL)
                .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery())
                        .mustNot(QueryBuilders.termQuery("_type", ElasticsearchLayout.ETM_DEFAULT_TYPE)));
        SearchHits searchHits = dataRepository.search(searchRequestBuilder).getHits();
        return searchHits.totalHits != 0;
    }

    @Override
    public void migrate() {
        if (!shouldBeExecuted()) {
            return;
        }
        checkAndCleanupPreviousRun(this.dataRepository, this.migrationIndexPrefix);

        FailureDetectingBulkProcessorListener listener = new FailureDetectingBulkProcessorListener();
        boolean succeeded = migrateMetrics(this.dataRepository, listener);
        if (succeeded) {
            succeeded = migrateHttpSessions(this.dataRepository, listener);
        }
        if (succeeded) {
            succeeded = migrateAudits(this.dataRepository, listener);
        }
        if (succeeded) {
            succeeded = migrateLicense(this.dataRepository, listener);
        }
        if (succeeded) {
            succeeded = migrateUsers(this.dataRepository, listener);
        }
        if (succeeded) {
            succeeded = migrateGroups(this.dataRepository, listener);
        }
        if (succeeded) {
            succeeded = migrateParsers(this.dataRepository, listener);
        }
        if (succeeded) {
            succeeded = migrateEndpoints(this.dataRepository, listener);
        }
        if (succeeded) {
            succeeded = migrateLdap(this.dataRepository, listener);
        }
        if (succeeded) {
            succeeded = migrateNodes(this.dataRepository, listener);
        }
        if (succeeded) {
            succeeded = migrateIIBNodes(this.dataRepository, listener);
        }
        if (succeeded) {
            succeeded = migrateGraphs(this.dataRepository, listener);
        }
        if (succeeded) {
            succeeded = migrateDashboards(this.dataRepository, listener);
        }
        if (!succeeded) {
            System.out.println("Errors detected. Quitting migration. Migrated indices are prefixed with '" + this.migrationIndexPrefix + "' and are still existent in your Elasticsearch cluster!");
            return;
        }
        // Reinitialize template
        ElasticsearchIndexTemplateCreator indexTemplateCreator = new ElasticsearchIndexTemplateCreator(dataRepository);
        indexTemplateCreator.addConfigurationChangeNotificationListener(new ElasticBackedEtmConfiguration(null, dataRepository, this.migrationIndexPrefix + ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        indexTemplateCreator.reinitialize();

        deleteIndices(this.dataRepository, "old indices", ElasticsearchLayout.METRICS_INDEX_ALIAS_ALL,
                ElasticsearchLayout.STATE_INDEX_NAME,
                ElasticsearchLayout.AUDIT_LOG_INDEX_ALIAS_ALL,
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME);
        reindexTemporaryIndicesToNew(this.dataRepository, listener, this.migrationIndexPrefix);
        deleteIndices(this.dataRepository, "temporary indices", this.migrationIndexPrefix + "*");
        deleteTemporaryIndexTemplate(this.dataRepository, this.migrationIndexPrefix);
        updateDocMappingForCurrentEventIndex(this.dataRepository);
        checkAndCreateIndexExistence(dataRepository,
                ElasticsearchLayout.STATE_INDEX_NAME,
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.METRICS_INDEX_ALIAS_ALL,
                ElasticsearchLayout.AUDIT_LOG_INDEX_ALIAS_ALL
        );
    }

    private boolean migrateMetrics(DataRepository dataRepository, FailureDetectingBulkProcessorListener listener) {
        Function<SearchHit, DocWriteRequest> requestBuilder = searchHit -> {
            Map<String, Object> sourceMap = new HashMap<>(searchHit.getSourceAsMap());
            sourceMap.put(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.METRICS_OBJECT_TYPE_ETM_NODE);
            removeNanValues(sourceMap);
            IndexRequestBuilder builder = new IndexRequestBuilder().
                    setIndex(Version2xTo3xMigrator.this.migrationIndexPrefix + searchHit.getIndex())
                    .setType(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setSource(sourceMap);
            return builder.build();
        };

        return migrateEntity("metrics",
                ElasticsearchLayout.METRICS_INDEX_ALIAS_ALL,
                null,
                dataRepository,
                createBulkProcessor(dataRepository, listener),
                listener,
                requestBuilder
        );
    }

    @SuppressWarnings("unchecked")
    private void removeNanValues(Map<String, Object> sourceMap) {
        Iterator<Map.Entry<String, Object>> iterator = sourceMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            if (entry.getValue() instanceof Double && Double.isNaN((Double) entry.getValue())) {
                iterator.remove();
            } else if (entry.getValue() instanceof Float && Float.isNaN((Float) entry.getValue())) {
                iterator.remove();
            } else if (entry.getValue() instanceof String && "NaN" .equals(entry.getValue())) {
                iterator.remove();
            } else if (entry.getValue() instanceof Map) {
                removeNanValues((Map<String, Object>) entry.getValue());
            }
        }
    }

    private boolean migrateHttpSessions(DataRepository dataRepository, FailureDetectingBulkProcessorListener listener) {
        Function<SearchHit, DocWriteRequest> requestBuilder = searchHit -> {
            Map<String, Object> valueMap = new HashMap<>();
            valueMap.put(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION);
            Map<String, Object> sourceMap = searchHit.getSourceAsMap();
            // Add the id attribute, because the original id is replaced with an prefix.
            sourceMap.put(sessionTags.getIdTag(), searchHit.getId());
            valueMap.put(ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION, sourceMap);
            return new IndexRequestBuilder()
                    .setIndex(this.migrationIndexPrefix + searchHit.getIndex())
                    .setType(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setId(ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION_ID_PREFIX + searchHit.getId())
                    .setSource(valueMap)
                    .build();
        };

        return migrateEntity("http sessions",
                ElasticsearchLayout.STATE_INDEX_NAME,
                null,
                dataRepository,
                createBulkProcessor(dataRepository, listener),
                listener,
                requestBuilder
        );
    }

    private boolean migrateAudits(DataRepository dataRepository, FailureDetectingBulkProcessorListener listener) {
        Function<SearchHit, DocWriteRequest> requestBuilder = searchHit -> {
            Map<String, Object> sourceMap = searchHit.getSourceAsMap();
            sourceMap.put(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, searchHit.getType());
            return new IndexRequestBuilder()
                    .setIndex(this.migrationIndexPrefix + searchHit.getIndex())
                    .setType(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setId(searchHit.getId())
                    .setSource(sourceMap)
                    .build();
        };

        return migrateEntity("audit logs",
                ElasticsearchLayout.AUDIT_LOG_INDEX_ALIAS_ALL,
                null,
                dataRepository,
                createBulkProcessor(dataRepository, listener),
                listener,
                requestBuilder
        );
    }

    private boolean migrateLicense(DataRepository dataRepository, FailureDetectingBulkProcessorListener listener) {
        return migrateEntity("license",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "license",
                dataRepository,
                createBulkProcessor(dataRepository, listener),
                listener,
                new DefaultIndexRequestCreator(
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LICENSE,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LICENSE + "_",
                        true
                )
        );
    }

    @SuppressWarnings("unchecked")
    private boolean migrateUsers(DataRepository dataRepository, FailureDetectingBulkProcessorListener listener) {
        Function<SearchHit, DocWriteRequest> requestBuilder = searchHit -> {
            Map<String, Object> valueMap = new HashMap<>();
            valueMap.put(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER);
            Map<String, Object> sourceMap = searchHit.getSourceAsMap();
            List<String> roles = (List<String>) sourceMap.get(this.principalTags.getRolesTag());
            if (roles != null) {
                sourceMap.put(this.principalTags.getRolesTag(), mapOldRoles(roles));
            }
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
            valueMap.put(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, sourceMap);
            IndexRequestBuilder builder = new IndexRequestBuilder()
                    .setIndex(this.migrationIndexPrefix + searchHit.getIndex())
                    .setType(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setSource(valueMap);
            builder.setId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + searchHit.getId());
            return builder.build();
        };

        return migrateEntity("users",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "user",
                dataRepository,
                createBulkProcessor(dataRepository, listener),
                listener,
                requestBuilder
        );
    }

    @SuppressWarnings("unchecked")
    private boolean migrateGroups(DataRepository dataRepository, FailureDetectingBulkProcessorListener listener) {
        Function<SearchHit, DocWriteRequest> requestBuilder = searchHit -> {
            Map<String, Object> valueMap = new HashMap<>();
            Map<String, Object> sourceMap = searchHit.getSourceAsMap();
            valueMap.put(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP);
            List<String> roles = (List<String>) sourceMap.get(this.principalTags.getRolesTag());
            if (roles != null) {
                sourceMap.put(this.principalTags.getRolesTag(), mapOldRoles(roles));
            }
            valueMap.put(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP, sourceMap);
            IndexRequestBuilder builder = new IndexRequestBuilder()
                    .setIndex(this.migrationIndexPrefix + searchHit.getIndex())
                    .setType(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setSource(valueMap);
            builder.setId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + searchHit.getId());
            return builder.build();
        };
        return migrateEntity("groups",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "group",
                dataRepository,
                createBulkProcessor(dataRepository, listener),
                listener,
                requestBuilder
        );
    }

    private boolean migrateParsers(DataRepository dataRepository, FailureDetectingBulkProcessorListener listener) {
        return migrateEntity("parsers",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "parser",
                dataRepository,
                createBulkProcessor(dataRepository, listener),
                listener,
                new DefaultIndexRequestCreator(
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER_ID_PREFIX,
                        true
                )
        );
    }

    private boolean migrateEndpoints(DataRepository dataRepository, FailureDetectingBulkProcessorListener listener) {
        return migrateEntity("endpoints",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "endpoint",
                dataRepository,
                createBulkProcessor(dataRepository, listener),
                listener,
                new DefaultIndexRequestCreator(
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT_ID_PREFIX,
                        true
                )
        );
    }

    private boolean migrateLdap(DataRepository dataRepository, FailureDetectingBulkProcessorListener listener) {
        return migrateEntity("ldap",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "ldap",
                dataRepository,
                createBulkProcessor(dataRepository, listener),
                listener,
                new DefaultIndexRequestCreator(
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LDAP,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LDAP_ID_PREFIX
                        , true
                )
        );
    }

    private boolean migrateNodes(DataRepository dataRepository, FailureDetectingBulkProcessorListener listener) {
        return migrateEntity("nodes",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "node",
                dataRepository,
                createBulkProcessor(dataRepository, listener),
                listener,
                new DefaultIndexRequestCreator(
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE_ID_PREFIX,
                        true
                )
        );
    }

    private boolean migrateIIBNodes(DataRepository dataRepository, FailureDetectingBulkProcessorListener listener) {
        return migrateEntity("IIB nodes",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "iib-node",
                dataRepository,
                createBulkProcessor(dataRepository, listener),
                listener,
                new DefaultIndexRequestCreator(
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE_ID_PREFIX,
                        true
                )
        );
    }

    private boolean migrateGraphs(DataRepository dataRepository, FailureDetectingBulkProcessorListener listener) {
        Function<SearchHit, DocWriteRequest> requestBuilder = searchHit -> {
            Map<String, Object> params = new HashMap<>();
            params.put("graphs", searchHit.getSourceAsMap().get("graphs"));
            return new UpdateRequestBuilder()
                    .setIndex(this.migrationIndexPrefix + searchHit.getIndex())
                    .setType(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + searchHit.getId())
                    .setScript(new Script(ScriptType.INLINE, "painless",
                            "if (ctx._source.user.get(\"graphs\") == null) {\n" +
                                    "    ctx._source.user.put(\"graphs\", new ArrayList());\n" +
                                    "}\n" +
                                    "for (Map graph : ((List)params.graphs)) {\n" +
                                    "    if (! ((List)ctx._source.user.get(\"graphs\")).stream().anyMatch(p -> ((Map)p).get(\"name\").equals(graph.get(\"name\")))) {\n" +
                                    "        ctx._source.user.graphs.add(graph);\n" +
                                    "    }\n" +
                                    "}", params))
                    .build();
        };

        return migrateEntity("graphs",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "graph",
                dataRepository,
                createBulkProcessor(dataRepository, listener),
                listener,
                requestBuilder
        );
    }

    private boolean migrateDashboards(DataRepository dataRepository, FailureDetectingBulkProcessorListener listener) {
        Function<SearchHit, DocWriteRequest> requestBuilder = searchHit -> {
            Map<String, Object> params = new HashMap<>();
            params.put("dashboards", searchHit.getSourceAsMap().get("dashboards"));
            return new UpdateRequestBuilder()
                    .setIndex(this.migrationIndexPrefix + searchHit.getIndex())
                    .setType(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + searchHit.getId())
                    .setScript(new Script(ScriptType.INLINE, "painless",
                            "if (ctx._source.user.get(\"dashboards\") == null) {\n" +
                                    "    ctx._source.user.put(\"dashboards\", new ArrayList());\n" +
                                    "}\n" +
                                    "for (Map dashboard : ((List)params.dashboards)) {\n" +
                                    "    if (! ((List)ctx._source.user.get(\"dashboards\")).stream().anyMatch(p -> ((Map)p).get(\"name\").equals(dashboard.get(\"name\")))) {\n" +
                                    "        for (Map row : ((List)dashboard.get(\"rows\"))) {" +
                                    "            for (Map col : ((List)row.get(\"cols\"))) {" +
                                    "                col.put(\"name\", col.remove(\"graph\"));" +
                                    "            }\n" +
                                    "            if (row.get(\"height\") instanceof String) {" +
                                    "                row.put(\"height\", Integer.parseInt(row.get(\"height\")));" +
                                    "            }\n" +
                                    "        }" +
                                    "        ctx._source.user.dashboards.add(dashboard);\n" +
                                    "    }\n" +
                                    "}", params))
                    .setScriptedUpsert(true)
                    .build();
        };

        return migrateEntity("dashboards",
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                "dashboard",
                dataRepository,
                createBulkProcessor(dataRepository, listener),
                listener,
                requestBuilder
        );
    }

    /**
     * The event index will not work with the new index template so we need to make it compatible over here.
     *
     * @param dataRepository The <code>DataRepository</code>.
     */
    private void updateDocMappingForCurrentEventIndex(DataRepository dataRepository) {
        final String currentIndex = ElasticsearchLayout.EVENT_INDEX_PREFIX + DateUtils.getIndexPerDayFormatter().format(ZonedDateTime.now());
        boolean indexExists = dataRepository.indicesExist(new GetIndexRequestBuilder().setIndices(currentIndex));
        if (!indexExists) {
            // Index of today does not exists.
            return;
        }
        PutMappingRequestBuilder builder = new PutMappingRequestBuilder().setIndices(currentIndex)
                .setType("doc").setSource(
                "{\n" +
                        "  \"dynamic_templates\": [\n" +
                        "    {\n" +
                        "      \"location\": {\n" +
                        "        \"match\": \"location\",\n" +
                        "        \"mapping\": {\n" +
                        "          \"type\": \"geo_point\"\n" +
                        "        }\n" +
                        "      }\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"handling_time\": {\n" +
                        "        \"match\": \"handling_time\",\n" +
                        "        \"mapping\": {\n" +
                        "          \"type\": \"date\"\n" +
                        "        }\n" +
                        "      }\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"host_address\": {\n" +
                        "        \"match\": \"host_address\",\n" +
                        "        \"mapping\": {\n" +
                        "          \"type\": \"ip\"\n" +
                        "        }\n" +
                        "      }\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"expiry\": {\n" +
                        "        \"match\": \"expiry\",\n" +
                        "        \"mapping\": {\n" +
                        "          \"type\": \"date\"\n" +
                        "        }\n" +
                        "      }\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"timestamp\": {\n" +
                        "        \"match\": \"timestamp\",\n" +
                        "        \"mapping\": {\n" +
                        "          \"type\": \"date\"\n" +
                        "        }\n" +
                        "      }\n" +
                        "    },\n" +
                        "    {\n" +
                        "        \"payload\": {\n" +
                        "            \"match\": \"payload\",\n" +
                        "            \"mapping\": {\n" +
                        "                \"type\": \"text\",\n" +
                        "                \"index\": \"analyzed\"\n" +
                        "            }\n" +
                        "        }\n" +
                        "    },    \n" +
                        "    {\n" +
                        "      \"string_as_text\": {\n" +
                        "        \"match_mapping_type\": \"string\",\n" +
                        "        \"mapping\": {\n" +
                        "          \"type\": \"keyword\"\n" +
                        "        }\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}",
                XContentType.JSON
                );
        dataRepository.indicesPutMapping(builder);
    }


    private boolean migrateEntity(String entityName, String index, String type, DataRepository dataRepository, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener, Function<SearchHit, DocWriteRequest> processor) {
        SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder().setIndices(index)
                .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery())
                        .mustNot(QueryBuilders.termQuery("_type", ElasticsearchLayout.ETM_DEFAULT_TYPE)))
                .setTimeout(TimeValue.timeValueSeconds(30))
                .setFetchSource(true);
        if (type != null) {
            searchRequestBuilder.setTypes(type);
        }
        return migrateEntity(searchRequestBuilder, entityName, dataRepository, bulkProcessor, listener, processor);
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

    private class DefaultIndexRequestCreator implements Function<SearchHit, DocWriteRequest> {

        private final String etmTypeAttributeName;
        private final String idPrefix;
        private final boolean ownNamespace;

        private DefaultIndexRequestCreator(String etmTypeAttributeName, String idPrefix, boolean ownNamespace) {
            this.etmTypeAttributeName = etmTypeAttributeName;
            this.idPrefix = idPrefix;
            this.ownNamespace = ownNamespace;
        }

        @Override
        public IndexRequest apply(SearchHit searchHit) {
            Map<String, Object> sourceMap = new HashMap<>();
            if (this.ownNamespace) {
                sourceMap.put(this.etmTypeAttributeName, searchHit.getSourceAsMap());
            } else {
                sourceMap.putAll(searchHit.getSourceAsMap());
            }
            if (this.etmTypeAttributeName != null) {
                sourceMap.put(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, this.etmTypeAttributeName);
            }
            IndexRequestBuilder builder = new IndexRequestBuilder()
                    .setIndex(Version2xTo3xMigrator.this.migrationIndexPrefix + searchHit.getIndex())
                    .setType(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setSource(sourceMap);
            if (this.idPrefix != null) {
                builder.setId(this.idPrefix + searchHit.getId());
            } else {
                builder.setId(searchHit.getId());
            }
            return builder.build();
        }
    }
}
