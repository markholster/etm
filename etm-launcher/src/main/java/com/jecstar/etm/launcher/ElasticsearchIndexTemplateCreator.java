package com.jecstar.etm.launcher;

import com.jecstar.etm.domain.writer.TelemetryEventTags;
import com.jecstar.etm.domain.writer.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.gui.rest.services.dashboard.domain.Data;
import com.jecstar.etm.launcher.http.session.ElasticsearchSessionTags;
import com.jecstar.etm.launcher.http.session.ElasticsearchSessionTagsJsonImpl;
import com.jecstar.etm.server.core.domain.audit.AuditLog;
import com.jecstar.etm.server.core.domain.configuration.ConfigurationChangeListener;
import com.jecstar.etm.server.core.domain.configuration.ConfigurationChangedEvent;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.configuration.converter.EtmConfigurationConverter;
import com.jecstar.etm.server.core.domain.configuration.converter.EtmConfigurationTags;
import com.jecstar.etm.server.core.domain.configuration.converter.json.EtmConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.domain.configuration.converter.json.EtmConfigurationTagsJsonImpl;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalConverter;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.*;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.util.BCrypt;
import com.jecstar.etm.signaler.domain.Signal;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.common.xcontent.json.JsonXContent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

public class ElasticsearchIndexTemplateCreator implements ConfigurationChangeListener {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(ElasticsearchIndexTemplateCreator.class);

    private final TelemetryEventTags eventTags = new TelemetryEventTagsJsonImpl();
    private final MetricConverterTags metricTags = new MetricConverterTagsJsonImpl();
    private final EtmConfigurationTags configurationTags = new EtmConfigurationTagsJsonImpl();
    private final ElasticsearchSessionTags sessionTags = new ElasticsearchSessionTagsJsonImpl();
    private final EtmConfigurationConverter<String> etmConfigurationConverter = new EtmConfigurationConverterJsonImpl();
    private final EtmPrincipalConverter<String> etmPrincipalConverter = new EtmPrincipalConverterJsonImpl();
    private final DataRepository dataRepository;
    private EtmConfiguration etmConfiguration;

    public ElasticsearchIndexTemplateCreator(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    public void createTemplates() {
        try {
            boolean templatesExist = this.dataRepository.indicesTemplatesExist(new IndexTemplatesExistRequestBuilder(ElasticsearchLayout.EVENT_TEMPLATE_NAME));
            if (!templatesExist) {
                creatEtmEventIndexTemplate(true, 5, 0);
                insertPainlessScripts();
            }
        } catch (IllegalArgumentException e) {
        } catch (IOException e) {
            if (log.isFatalLevelEnabled()) {
                log.logFatalMessage("Failed to create painless scripts. Cluster will not be usable.", e);
            }
        }

        try {
            boolean templatesExist = this.dataRepository.indicesTemplatesExist(new IndexTemplatesExistRequestBuilder(ElasticsearchLayout.AUDIT_LOG_TEMPLATE_NAME));
            if (!templatesExist) {
                creatEtmAuditLogIndexTemplate(true, 0);
            }
        } catch (IllegalArgumentException e) {
        }

        try {
            boolean templatesExist = this.dataRepository.indicesTemplatesExist(new IndexTemplatesExistRequestBuilder(ElasticsearchLayout.METRICS_TEMPLATE_NAME));
            if (!templatesExist) {
                creatEtmMetricsIndexTemplate(true, 0);
            }
        } catch (IllegalArgumentException e) {
        }

        try {
            boolean templatesExist = this.dataRepository.indicesTemplatesExist(new IndexTemplatesExistRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
            if (!templatesExist) {
                creatEtmConfigurationIndexTemplate(true, 0);
            }
        } catch (IllegalArgumentException e) {
        }

        try {
            boolean templatesExist = this.dataRepository.indicesTemplatesExist(new IndexTemplatesExistRequestBuilder(ElasticsearchLayout.STATE_INDEX_NAME));
            if (!templatesExist) {
                creatEtmStateIndexTemplate(true, 0);
                boolean indicesExist = this.dataRepository.indicesExist(new GetIndexRequestBuilder(ElasticsearchLayout.STATE_INDEX_NAME));
                if (!indicesExist) {
                    // Create the index over here because the first thing we do is getting a session from the index. If index insn't already present that get will fail with an exception.
                    CreateIndexRequestBuilder createIndexRequestBuilder = new CreateIndexRequestBuilder(ElasticsearchLayout.STATE_INDEX_NAME).setSettings(Settings.builder()
                            .put("index.number_of_shards", 1)
                            .put("index.number_of_replicas", 0));
                    this.dataRepository.indicesCreate(createIndexRequestBuilder);
                }
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void insertPainlessScripts() throws IOException {
        PutStoredScriptRequestBuilder builder = new PutStoredScriptRequestBuilder()
                .setId("etm_update-search-template").setContent(BytesReference.bytes(JsonXContent.contentBuilder().startObject()
                        .field("script").startObject()
                        .field("lang", "painless")
                        .field("source", createUpdateSearchTemplateScript())
                        .endObject()
                        .endObject()), XContentType.JSON);
        this.dataRepository.putStoredScript(builder);
        builder = new PutStoredScriptRequestBuilder()
                .setId("etm_remove-search-template").setContent(BytesReference.bytes(JsonXContent.contentBuilder().startObject()
                .field("script").startObject()
                .field("lang", "painless")
                .field("source", createRemoveSearchTemplateScript())
                .endObject()
                        .endObject()), XContentType.JSON);
        this.dataRepository.putStoredScript(builder);
        builder = new PutStoredScriptRequestBuilder()
                .setId("etm_update-search-history").setContent(BytesReference.bytes(JsonXContent.contentBuilder().startObject()
                .field("script").startObject()
                .field("lang", "painless")
                .field("source", createUpdateSearchHistoryScript())
                .endObject()
                        .endObject()), XContentType.JSON);
        this.dataRepository.putStoredScript(builder);
        builder = new PutStoredScriptRequestBuilder()
                .setId("etm_update-event").setContent(BytesReference.bytes(JsonXContent.contentBuilder().startObject()
                .field("script").startObject()
                .field("lang", "painless")
                .field("source", createUpdateEventScript())
                .endObject()
                        .endObject()), XContentType.JSON);
        this.dataRepository.putStoredScript(builder);
        builder = new PutStoredScriptRequestBuilder()
                .setId("etm_update-event-with-correlation").setContent(BytesReference.bytes(JsonXContent.contentBuilder().startObject()
                .field("script").startObject()
                .field("lang", "painless")
                .field("source", createUpdateEventWithCorrelationScript())
                .endObject()
                        .endObject()), XContentType.JSON);
        this.dataRepository.putStoredScript(builder);
        builder = new PutStoredScriptRequestBuilder()
                .setId("etm_update-request-with-response").setContent(BytesReference.bytes(JsonXContent.contentBuilder().startObject()
                        .field("script").startObject()
                        .field("lang", "painless")
                        .field("source", createUpdateRequestWithResponseScript())
                        .endObject()
                        .endObject()), XContentType.JSON);
        this.dataRepository.putStoredScript(builder);
        builder = new PutStoredScriptRequestBuilder()
                .setId("etm_update-node").setContent(BytesReference.bytes(JsonXContent.contentBuilder().startObject()
                        .field("script").startObject()
                        .field("lang", "painless")
                        .field("source", createUpdateNodeScript())
                        .endObject()
                        .endObject()), XContentType.JSON);
        this.dataRepository.putStoredScript(builder);
    }

    /**
     * Reinitializes all Elasticsearch templates, scripts etc.
     */
    public void reinitialize() {
        reinitializeTemplates();
        try {
            insertPainlessScripts();
        } catch (IOException e) {
            if (log.isFatalLevelEnabled()) {
                log.logFatalMessage("Failed to reinitialize painless scripts. Cluster will not be usable.", e);
            }
        }
    }

    private void creatEtmEventIndexTemplate(boolean create, int shardsPerIndex, int replicasPerIndex) {
        PutIndexTemplateRequestBuilder builder = new PutIndexTemplateRequestBuilder(ElasticsearchLayout.EVENT_TEMPLATE_NAME)
                .setCreate(create)
                .setPatterns(Collections.singletonList(ElasticsearchLayout.EVENT_INDEX_PREFIX + "*"))
                .setSettings(Settings.builder()
                        .put("index.number_of_shards", shardsPerIndex)
                        .put("index.number_of_replicas", replicasPerIndex)
                        .put("index.translog.durability", "async")
                )
                .setMapping(createEventMapping(), XContentType.JSON)
                .setAlias(new Alias(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL));
        this.dataRepository.indicesPutTemplate(builder);
    }

    private void creatEtmMetricsIndexTemplate(boolean create, int replicasPerIndex) {
        PutIndexTemplateRequestBuilder builder = new PutIndexTemplateRequestBuilder(ElasticsearchLayout.METRICS_TEMPLATE_NAME)
                .setCreate(create)
                .setPatterns(Collections.singletonList(ElasticsearchLayout.METRICS_INDEX_PREFIX + "*"))
                .setSettings(Settings.builder()
                        .put("number_of_shards", 2)
                        .put("number_of_replicas", replicasPerIndex))
                .setMapping(createMetricsMapping(), XContentType.JSON)
                .setAlias(new Alias(ElasticsearchLayout.METRICS_INDEX_ALIAS_ALL));
        this.dataRepository.indicesPutTemplate(builder);
    }

    private void creatEtmAuditLogIndexTemplate(boolean create, int replicasPerIndex) {
        PutIndexTemplateRequestBuilder builder = new PutIndexTemplateRequestBuilder(ElasticsearchLayout.AUDIT_LOG_TEMPLATE_NAME)
                .setCreate(create)
                .setPatterns(Collections.singletonList(ElasticsearchLayout.AUDIT_LOG_INDEX_PREFIX + "*"))
                .setSettings(Settings.builder()
                        .put("index.number_of_shards", 2)
                        .put("index.number_of_replicas", replicasPerIndex)
                )
                .setMapping(createAuditMapping(), XContentType.JSON)
                .setAlias(new Alias(ElasticsearchLayout.AUDIT_LOG_INDEX_ALIAS_ALL));
        this.dataRepository.indicesPutTemplate(builder);
    }

    private void creatEtmConfigurationIndexTemplate(boolean create, int replicasPerIndex) {
        PutIndexTemplateRequestBuilder builder = new PutIndexTemplateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setCreate(create)
                .setPatterns(Collections.singletonList(ElasticsearchLayout.CONFIGURATION_INDEX_NAME))
                .setSettings(Settings.builder()
                        .put("number_of_shards", 1)
                        .put("number_of_replicas", replicasPerIndex))
                .setMapping(createEtmConfigurationMapping(), XContentType.JSON);
        this.dataRepository.indicesPutTemplate(builder);
        if (create) {
            insertDefaultEtmConfiguration(this.dataRepository);
            insertAdminUser(this.dataRepository);
        }
    }

    private void creatEtmStateIndexTemplate(boolean create, int replicasPerIndex) {
        PutIndexTemplateRequestBuilder builder = new PutIndexTemplateRequestBuilder(ElasticsearchLayout.STATE_INDEX_NAME)
                .setCreate(create)
                .setPatterns(Collections.singletonList(ElasticsearchLayout.STATE_INDEX_NAME))
                .setSettings(Settings.builder()
                        .put("number_of_shards", 1)
                        .put("number_of_replicas", replicasPerIndex))
                .setMapping(createEtmStateMapping(), XContentType.JSON);
        this.dataRepository.indicesPutTemplate(builder);
    }

    private String createEventMapping() {
        return "{\"dynamic_templates\": ["
                + "{ \"" + this.eventTags.getEndpointHandlerLocationTag() + "\": { \"match\": \"" + this.eventTags.getEndpointHandlerLocationTag() + "\", \"mapping\": {\"type\": \"geo_point\"}}}"
                + ", { \"" + this.eventTags.getEndpointHandlerHandlingTimeTag() + "\": { \"match\": \"" + this.eventTags.getEndpointHandlerHandlingTimeTag() + "\", \"mapping\": {\"type\": \"date\"}}}"
                + ", { \"" + this.eventTags.getApplicationHostAddressTag() + "\": { \"match\": \"" + this.eventTags.getApplicationHostAddressTag() + "\", \"mapping\": {\"type\": \"ip\"}}}"
                + ", { \"" + this.eventTags.getExpiryTag() + "\": { \"match\": \"" + this.eventTags.getExpiryTag() + "\", \"mapping\": {\"type\": \"date\"}}}"
                + ", { \"" + this.eventTags.getTimestampTag() + "\": { \"match\": \"" + this.eventTags.getTimestampTag() + "\", \"mapping\": {\"type\": \"date\"}}}"
                + ", { \"string_as_text\": { \"match_mapping_type\": \"string\", \"mapping\": {\"type\": \"text\", \"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}}}}"
                + "]}";
    }

    private String createMetricsMapping() {
        return "{\"dynamic_templates\": ["
                + "{ \"" + this.metricTags.getTimestampTag() + "\": { \"match\": \"" + this.metricTags.getTimestampTag() + "\", \"mapping\": {\"type\": \"date\"}}}"
                + "]}";
    }

    private String createAuditMapping() {
        return "{\"dynamic_templates\": ["
                + "{ \"" + AuditLog.TIMESTAMP + "\": { \"match\": \"" + AuditLog.TIMESTAMP + "\", \"mapping\": {\"type\": \"date\"}}}"
                + ", { \"" + AuditLog.HANDLING_TIME + "\": { \"match\": \"" + AuditLog.HANDLING_TIME + "\", \"mapping\": {\"type\": \"date\"}}}"
                + "]}";
    }

    public String createEtmConfigurationMapping() {
        return "{\"dynamic_templates\": ["
                + "{ \"" + Signal.LAST_EXECUTED + "\": { \"match\": \"" + Signal.LAST_EXECUTED + "\", \"mapping\": {\"type\": \"date\"}}}"
                + ", { \"" + Signal.LAST_FAILED + "\": { \"match\": \"" + Signal.LAST_FAILED + "\", \"mapping\": {\"type\": \"date\"}}}"
                + ", { \"" + Signal.LAST_PASSED + "\": { \"match\": \"" + Signal.LAST_PASSED + "\", \"mapping\": {\"type\": \"date\"}}}"
                + ", { \"" + Data.FROM + "\": { \"match\": \"" + Data.FROM + "\", \"mapping\": {\"type\": \"keyword\"}}}"
                + ", { \"" + Data.TILL + "\": { \"match\": \"" + Data.TILL + "\", \"mapping\": {\"type\": \"keyword\"}}}"
                + ", { \"" + this.configurationTags.getStartTimeTag() + "\": { \"match\": \"" + this.configurationTags.getStartTimeTag() + "\", \"mapping\": {\"type\": \"keyword\"}}}"
                + ", { \"" + this.configurationTags.getEndTimeTag() + "\": { \"match\": \"" + this.configurationTags.getEndTimeTag() + "\", \"mapping\": {\"type\": \"keyword\"}}}"
                + ", { \"string_as_keyword\": { \"match_mapping_type\": \"string\", \"mapping\": {\"type\": \"keyword\"}}}"
                + "]}";
    }

    private String createEtmStateMapping() {
        return "{\"dynamic_templates\": ["
                + "{ \"" + this.configurationTags.getStartTimeTag() + "\": { \"path_match\": \"" + ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION + "." + this.sessionTags.getLastAccessedTag() + "\", \"mapping\": {\"type\": \"date\"}}}"
                + "]}";
    }

    private void insertDefaultEtmConfiguration(DataRepository dataRepository) {
        IndexRequestBuilder builder = new IndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_NODE_DEFAULT)
                .setWaitForActiveShards(ActiveShardCount.ALL)
                .setSource(this.etmConfigurationConverter.write(null, new EtmConfiguration("temp-for-creating-default")), XContentType.JSON);
        dataRepository.index(builder);
    }

    private void insertAdminUser(DataRepository dataRepository) {
        EtmPrincipal adminUser = new EtmPrincipal("admin", BCrypt.hashpw("password", BCrypt.gensalt()));
        adminUser.addRoles(Arrays.stream(SecurityRoles.ALL_READ_WRITE_SECURITY_ROLES).collect(Collectors.toCollection(ArrayList::new)));
        adminUser.addSignalDatasources(Arrays.stream(ElasticsearchLayout.ALL_DATASOURCES).collect(Collectors.toCollection(ArrayList::new)));
        adminUser.addDashboardDatasources(Arrays.stream(ElasticsearchLayout.ALL_DATASOURCES).collect(Collectors.toCollection(ArrayList::new)));
        adminUser.setApiKey(UUID.randomUUID().toString());
        adminUser.setChangePasswordOnLogon(true);
        IndexRequestBuilder builder = new IndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + adminUser.getId())
                .setWaitForActiveShards(ActiveShardCount.ALL)
                .setSource(this.etmPrincipalConverter.writePrincipal(adminUser), XContentType.JSON);
        dataRepository.index(builder);
    }


    private String createUpdateSearchTemplateScript() {
        return "if (params.template != null) {\n" +
                "    if (ctx._source.user.search_templates != null) {\n" +
                "        boolean found = false;\n" +
                "        for (int i=0; i < ctx._source.user.search_templates.size(); i++) {\n" +
                "            if (ctx._source.user.search_templates[i].name.equals(params.template.name)) {\n" +
                "                ctx._source.user.search_templates[i] = params.template;\n" +
                "                found = true;\n" +
                "             }\n" +
                "        }\n" +
                "        if (!found && ctx._source.user.search_templates.size() < params.max_templates) {\n" +
                "            ctx._source.user.search_templates.add(params.template);\n" +
                "        }\n" +
                "    } else if (params.max_templates > 0) {\n" +
                "        ctx._source.user.search_templates = new ArrayList();\n" +
                "        ctx._source.user.search_templates.add(params.template);\n" +
                "    }\n" +
                "}\n";
    }

    private String createRemoveSearchTemplateScript() {
        return "if (params.name != null) {\n" +
                "    if (ctx._source.user.search_templates != null) {\n" +
                "		 Iterator it = ctx._source.user.search_templates.iterator();\n" +
                "        while (it.hasNext()) {\n" +
                "            def item = it.next()\n;" +
                "            if (item.name.equals(params.name)) {\n" +
                "                it.remove();\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
    }

    private String createUpdateSearchHistoryScript() {
        return "if (params.query != null) {\n" +
                "    if (ctx._source.user.search_history != null) {\n" +
                "        for (int i=0; i < ctx._source.user.search_history.size(); i++) {\n" +
                "            if (ctx._source.user.search_history[i].query.equals(params.query.query)) {\n" +
                "                ctx._source.user.search_history.remove(i);\n" +
                "            }\n" +
                "        }\n" +
                "        ctx._source.user.search_history.add(params.query);\n" +
                "    } else {\n" +
                "        ctx._source.user.search_history = new ArrayList();\n" +
                "        ctx._source.user.search_history.add(params.query);\n" +
                "    }\n" +
                "}\n" +
                "if (ctx._source.user.search_history != null && params.history_size != null) {\n" +
                "    int removeCount = ctx._source.user.search_history.size() - params.history_size;\n" +
                "    for (int i=0; i < removeCount; i++) {\n" +
                "        ctx._source.user.search_history.remove(0);\n" +
                "    }\n" +
                "}\n";
    }

    private String createUpdateEventWithCorrelationScript() {
        return "String correlatingId = (String)params.get(\"correlating_id\");\n" +
                "Map targetSource = (Map)ctx.get(\"_source\");\n" +
                "\n" +
                "List correlations = (List)targetSource.get(\"correlations\");\n" +
                "if (correlations == null) {\n" +
                "	correlations = new ArrayList();\n" +
                "	targetSource.put(\"correlations\", correlations);\n" +
                "}\n" +
                "if (!correlations.contains(correlatingId)) {\n" +
                "	correlations.add(correlatingId);\n" +
                "}";
    }

    private String createUpdateEventScript() {
        return "Map findEndpointByName(List endpoints, String name) {\n" +
                "\tif (endpoints == null) {\n" +
                "\t\treturn null;\n" +
                "\t}\n" +
                "\tfor (Map endpoint : endpoints) {\n" +
                "\t\tString endpointName = (String)endpoint.get(\"name\");\n" +
                "\t\tif (endpointName != null && endpointName.equals(name)) {\n" +
                "\t\t\treturn endpoint;\n" +
                "\t\t}\n" +
                "\t}\n" +
                "\treturn null;\n" +
                "}\n" +
                "\n" +
                "boolean hasWritingEndpointHandler(List endpointHandlers) {\n" +
                "    if (endpointHandlers == null) {\n" +
                "        return false;\n" +
                "    }\n" +
                "    for (Map endpointHandler : endpointHandlers) {\n" +
                "        if (\"WRITER\".equals(endpointHandler.get(\"type\"))) {\n" +
                "            return true;\n" +
                "        }\n" +
                "    }\n" +
                "    return false;\n" +
                "}\n" +
                "\n" +
                "boolean mergeEntity(Map targetSource, Map inputSource, String entityName) {\n" +
                "\tObject sourceEntity = inputSource.get(entityName);\n" +
                "\tObject targetEntity = targetSource.get(entityName);\n" +
                "\tif (sourceEntity == null) {\n" +
                "\t\treturn false;\n" +
                "\t}\n" +
                "\tif (sourceEntity instanceof List) {\n" +
                "\t\t// merge a list, adding all source items to the target list\n" +
                "\t\tif (targetEntity == null) {\n" +
                "\t\t\ttargetEntity = sourceEntity;\n" +
                "\t\t} else {\n" +
                "\t\t\t((List)targetEntity).addAll((List)sourceEntity);\n" +
                "\t\t}\n" +
                "\t} else if (sourceEntity instanceof Map) {\n" +
                "\t\t// merge a map, adding all items to the map if the target map didn't contain them.\n" +
                "\t\tif (targetEntity == null) {\n" +
                "\t\t\ttargetEntity = sourceEntity;\n" +
                "\t\t} else {\n" +
                "\t\t\tMap original = (Map)targetEntity;\n" +
                "\t\t\ttargetEntity = new HashMap((Map)sourceEntity);\n" +
                "\t\t\t((Map)targetEntity).putAll(original);\n" +
                "\t\t}\t\n" +
                "\t} else {\n" +
                "\t\t// Not a collection, add the value of the target is not null.\n" +
                "\t\tif (targetEntity != null) {\n" +
                "\t\t\treturn false;\n" +
                "\t\t}\n" +
                "\t\ttargetEntity = sourceEntity;\n" +
                "\t}\n" +
                "\ttargetSource.put(entityName, targetEntity);\t\n" +
                "\treturn true;\n" +
                "}\n" +
                "\n" +
                "boolean updateResponseTimeInEndpointHandlers(List endpoints, String applicationName, long handlingTime, String handlerType) {\n" +
                "\tif (endpoints == null) {\n" +
                "\t\treturn false;\n" +
                "\t}\n" +
                "\tboolean updated = false;\n" +
                "\tfor (Map endpoint : endpoints) {\n" +
                "\t\tif (endpoint.get(\"endpoint_handlers\") != null) {\n" +
                "\t\t\tList endpointHandlers = (List)endpoint.get(\"endpoint_handlers\");\n" +
                "\t\t\tfor (Map endpointHandler : endpointHandlers) {\n" +
                "\t\t\t    if (!handlerType.equals(endpointHandler.get(\"type\"))) {\n" +
                "\t\t\t        continue;\n" +
                "\t\t\t    }\n" +
                "\t\t\t\tif (applicationName == null && (endpointHandler.get(\"application\") == null || ((Map)endpointHandler.get(\"application\")).get(\"name\") == null)) {\n" +
                "\t\t\t\t\tlong responseTime = handlingTime - (long)endpointHandler.get(\"handling_time\");\n" +
                "\t\t\t\t\tif (responseTime >= 0) {\n" +
                "\t\t\t\t\t    endpointHandler.put(\"response_time\", responseTime);\n" +
                "\t\t\t\t\t\tupdated = true;\n" +
                "\t\t\t\t\t}\n" +
                "\t\t\t\t} else if (applicationName != null && endpointHandler.get(\"application\") != null && applicationName.equals(((Map)endpointHandler.get(\"application\")).get(\"name\"))) {\n" +
                "\t\t\t\t\tlong responseTime = handlingTime - (long)endpointHandler.get(\"handling_time\");\n" +
                "\t\t\t\t\tif (responseTime >= 0) {\n" +
                "\t\t\t\t\t    endpointHandler.put(\"response_time\", responseTime);\n" +
                "\t\t\t\t\t\tupdated = true;\n" +
                "\t\t\t\t\t}\n" +
                "\t\t\t\t}\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t}\n" +
                "\treturn updated;\n" +
                "}\n" +
                "\n" +
                "boolean updateResponseTimeInReaders(List endpoints, String applicationName, long handlingTime) {\n" +
                "    return updateResponseTimeInEndpointHandlers(endpoints, applicationName, handlingTime, \"READER\");\n" +
                "}\n" +
                "\n" +
                "boolean updateResponseTimeInWriters(List endpoints, String applicationName, long handlingTime) {\n" +
                "    return updateResponseTimeInEndpointHandlers(endpoints, applicationName, handlingTime, \"WRITER\");\n" +
                "}\n" +
                "\n" +
                "boolean mainMethod(Map ctx, Map params) {\n" +
                "    Map inputSource = (Map)params.get(\"source\");\n" +
                "    Map targetSource = (Map)ctx.get(\"_source\");\n" +
                "\n" +
                "    // Check if we have handled this message before. Each message has an unique hashcode. We keep track of this hashcode in the elasticsearch db.\n" +
                "    // If the provided hashcode is already stored with the document we assume a duplicate offering for storage and quit processing.\n" +
                "    long inputHash = ((List)inputSource.get(\"event_hashes\")).get(0);\n" +
                "    List targetHashes = (List)targetSource.get(\"event_hashes\");\n" +
                "    if (targetHashes != null) {\n" +
                "        if (targetHashes.indexOf(inputHash) != -1) {\n" +
                "            return false;\n" +
                "        }\n" +
                "        targetHashes.add(inputHash);\n" +
                "    }\n" +
                "\n" +
                "    Map tempForCorrelations = (Map)targetSource.get(\"temp_for_correlations\");\n" +
                "    boolean correlatedBeforeInserted = false;\n" +
                "    if (targetSource.get(\"payload\") == null &&\n" +
                "        targetSource.get(\"correlations\") != null) {\n" +
                "        // The correlation to this event is stored before the event itself is stored. Merge the entire event.\n" +
                "        correlatedBeforeInserted = true;\n" +
                "        List correlations = (List)targetSource.get(\"correlations\");\n" +
                "\n" +
                "        targetSource = inputSource;\n" +
                "        targetSource.put(\"correlations\", correlations);\n" +
                "        if (tempForCorrelations != null) {\n" +
                "            targetSource.put(\"temp_for_correlations\", tempForCorrelations);\n" +
                "        }\n" +
                "        ctx.put(\"_source\", targetSource);\n" +
                "    }\n" +
                "\n" +
                "    // Merge several fields\n" +
                "    if (!correlatedBeforeInserted) {\n" +
                "        for (String field : inputSource.keySet()) {\n" +
                "            if (!\"endpoints\".equals(field)\n" +
                "                && !\"event_hashes\".equals(field)\n" +
                "               ) {\n" +
                "                    mergeEntity(targetSource, inputSource, field);\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    // Merge the endpoints.\n" +
                "    List inputEndpoints = (List)inputSource.get(\"endpoints\");\n" +
                "    List targetEndpoints = (List)targetSource.get(\"endpoints\");\n" +
                "    if (inputEndpoints != null && !correlatedBeforeInserted) {\n" +
                "        // Merge endpoints\n" +
                "        for (Map inputEndpoint : inputEndpoints) {\n" +
                "            // Try to find if an endpoint with a given name is present.\n" +
                "            Map targetEndpoint = findEndpointByName(targetEndpoints, (String)inputEndpoint.get(\"name\"));\n" +
                "            if (targetEndpoint == null) {\n" +
                "                // This endpoint was not present.\n" +
                "                if (targetEndpoints == null) {\n" +
                "                    targetEndpoints = new ArrayList();\n" +
                "                    targetSource.put(\"endpoints\", targetEndpoints);\n" +
                "                }\n" +
                "                targetEndpoints.add(inputEndpoint);\n" +
                "            } else {\n" +
                "                // Endpoint was present. Copy endpoint handlers to target.\n" +
                "                List inputEndpointHandlers = (List)inputEndpoint.get(\"endpoint_handlers\");\n" +
                "                if (inputEndpointHandlers != null) {\n" +
                "                    // Add endpoint handlers to target.\n" +
                "                    List targetEndpointHandlers = (List)targetEndpoint.get(\"endpoint_handlers\");\n" +
                "                    if (targetEndpointHandlers == null) {\n" +
                "                        targetEndpointHandlers = new ArrayList();\n" +
                "                        targetEndpointHandlers.addAll(inputEndpointHandlers);\n" +
                "                        targetEndpoint.put(\"endpoint_handlers\", targetEndpointHandlers);\n" +
                "                    } else {\n" +
                "                        boolean writerPresent = hasWritingEndpointHandler(targetEndpointHandlers);\n" +
                "                        for (Map inputEndpointHandler : inputEndpointHandlers) {\n" +
                "                            if (\"WRITER\".equals(inputEndpointHandler.get(\"type\")) && writerPresent) {\n" +
                "                                continue;\n" +
                "                            }\n" +
                "                            targetEndpointHandlers.add(inputEndpointHandler);\n" +
                "                        }\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "     }\n" +
                "\n" +
                "    // Recalculate latencies\n" +
                "    if (targetEndpoints != null) {\n" +
                "        for (Map targetEndpoint : targetEndpoints) {\n" +
                "            Map targetWritingEndpointHandler = null;\n" +
                "            List endpointHandlers = (List)targetEndpoint.get(\"endpoint_handlers\");\n" +
                "            if (endpointHandlers != null) {\n" +
                "                for (Map endpointHandler : endpointHandlers) {\n" +
                "                    if (\"WRITER\".equals(endpointHandler.get(\"type\"))) {\n" +
                "                        targetWritingEndpointHandler = endpointHandler;\n" +
                "                        break;\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "            if (targetWritingEndpointHandler != null && targetWritingEndpointHandler.get(\"handling_time\") != null) {\n" +
                "                long writeTime = (long)targetWritingEndpointHandler.get(\"handling_time\");\n" +
                "                endpointHandlers = (List)targetEndpoint.get(\"endpoint_handlers\");\n" +
                "                if (endpointHandlers != null) {\n" +
                "                    for (Map endpointHandler : endpointHandlers) {\n" +
                "                        if (\"READER\".equals(endpointHandler.get(\"type\")) && endpointHandler.get(\"handling_time\") != null) {\n" +
                "                            endpointHandler.put(\"latency\",  ((long)endpointHandler.get(\"handling_time\")) - writeTime);\n" +
                "                        }\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "    // Check for response times to be updated\n" +
                "    if (tempForCorrelations != null) {\n" +
                "        List dataForReaders = (List)tempForCorrelations.get(\"data_for_readers\");\n" +
                "        if (dataForReaders != null && targetEndpoints != null) {\n" +
                "            Iterator it = dataForReaders.iterator();\n" +
                "            while (it.hasNext()) {\n" +
                "                Map dataForReader = (Map)it.next();\n" +
                "                String appName = (String)dataForReader.get(\"name\");\n" +
                "                long handlingTime = (long)dataForReader.get(\"handling_time\");\n" +
                "                boolean updated = updateResponseTimeInReaders(targetEndpoints, appName, handlingTime);\n" +
                "                if (!updated && appName == null) {\n" +
                "                    // When the appName == null also update the writers\n" +
                "                    updated = updateResponseTimeInWriters(targetEndpoints, appName, handlingTime);\n" +
                "                }\n" +
                "                if (updated) {\n" +
                "                    it.remove();\n" +
                "                }\n" +
                "            }\n" +
                "            if (dataForReaders.isEmpty()) {\n" +
                "                tempForCorrelations.remove(\"data_for_readers\");\n" +
                "            }\n" +
                "        }\n" +
                "        List dataForWriters = (List)tempForCorrelations.get(\"data_for_writers\");\n" +
                "        if (dataForWriters != null && targetEndpoints != null) {\n" +
                "            Iterator it = dataForWriters.iterator();\n" +
                "            while (it.hasNext()) {\n" +
                "                Map dataForWriter = (Map)it.next();\n" +
                "                String appName = (String)dataForWriter.get(\"name\");\n" +
                "                long handlingTime = (long)dataForWriter.get(\"handling_time\");\n" +
                "                boolean updated = updateResponseTimeInWriters(targetEndpoints, appName, handlingTime);\n" +
                "                if (!updated && appName == null) {\n" +
                "                    // When the appName == null also update the readers\n" +
                "                    updated = updateResponseTimeInReaders(targetEndpoints, appName, handlingTime);\n" +
                "                }\n" +
                "                if (updated) {\n" +
                "                    it.remove();\n" +
                "                }\n" +
                "            }\n" +
                "            if (dataForWriters.isEmpty()) {\n" +
                "                tempForCorrelations.remove(\"data_for_writers\");\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        if (tempForCorrelations.isEmpty()) {\n" +
                "            targetSource.remove(\"temp_for_correlations\");\n" +
                "        }\n" +
                "    }\n" +
                "    return true;\n" +
                "}\n" +
                "mainMethod(ctx, params);";
    }

    private String createUpdateRequestWithResponseScript() {
        return "boolean setResponseTimeOnEndpointHandlers(List endpointHandlers, String applicationName, long handlingTime, String handlerType) {\n" +
                "\tif (endpointHandlers == null) {\n" +
                "\t\treturn false;\n" +
                "\t}\n" +
                "\tboolean updated = false;\n" +
                "\tfor (Map endpointHandler : endpointHandlers) {\n" +
                "\t    if (!handlerType.equals(endpointHandler.get(\"type\"))) {\n" +
                "            continue;\n" +
                "        }\n" +
                "\t\tif (applicationName == null && (endpointHandler.get(\"application\") == null || ((Map)endpointHandler.get(\"application\")).get(\"name\") == null)) {\n" +
                "\t\t\tlong responseTime = handlingTime - (long)endpointHandler.get(\"handling_time\");\n" +
                "\t\t\tif (responseTime >=0) {\n" +
                "\t\t\t\tendpointHandler.put(\"response_time\", responseTime);\n" +
                "\t\t\t\tupdated = true;\n" +
                "\t\t\t}\n" +
                "\t\t} else if (applicationName != null && endpointHandler.get(\"application\") != null && applicationName.equals(((Map)endpointHandler.get(\"application\")).get(\"name\"))) {\n" +
                "\t\t\tlong responseTime = handlingTime - (long)endpointHandler.get(\"handling_time\");\n" +
                "\t\t\tif (responseTime >=0) {\n" +
                "\t\t\t\tendpointHandler.put(\"response_time\", responseTime);\n" +
                "\t\t\t\tupdated = true;\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t}\n" +
                "\treturn updated;\n" +
                "}\n" +
                "\n" +
                "boolean setResponseTimeOnReadingEndpointHandlers(List endpointHandlers, String applicationName, long writerHandlingTime) {\n" +
                "    return setResponseTimeOnEndpointHandlers(endpointHandlers, applicationName, writerHandlingTime, \"READER\");\n" +
                "}\n" +
                "\n" +
                "boolean setResponseTimeOnWritingEndpointHandlers(List endpoints, String applicationName, long readerHandlingTime) {\n" +
                "\tif (endpoints == null) {\n" +
                "\t\treturn false;\n" +
                "\t}\n" +
                "\tboolean writerUpdated = false;\n" +
                "\tfor (Map endpoint : endpoints) {\n" +
                "\t    writerUpdated = setResponseTimeOnEndpointHandlers((List)endpoint.get(\"endpoint_handlers\"), applicationName, readerHandlingTime, \"WRITER\") || writerUpdated;\n" +
                "\t}\n" +
                "\treturn writerUpdated;\n" +
                "}\n" +
                "\n" +
                "Map getWritingEndpointHandler(List endpointHandlers) {\n" +
                "    if (endpointHandlers == null) {\n" +
                "        return null;\n" +
                "    }\n" +
                "    for (Map endpointHandler : endpointHandlers) {\n" +
                "        if (\"WRITER\".equals(endpointHandler.get(\"type\"))) {\n" +
                "            return endpointHandler;\n" +
                "        }\n" +
                "    }\n" +
                "    return null;\n" +
                "}\n" +
                "\n" +
                "void addDataForReaders(Map source, long handlingTime, String applicationName) {\n" +
                "\tMap tempCorrelation = (Map)source.get(\"temp_for_correlations\");\n" +
                "\tif (tempCorrelation == null) {\n" +
                "\t\ttempCorrelation = new HashMap();\n" +
                "\t\tsource.put(\"temp_for_correlations\", tempCorrelation);\n" +
                "\t}\n" +
                "\tList dataForReaders = tempCorrelation.get(\"data_for_readers\");\n" +
                "\tif (dataForReaders == null) {\n" +
                "\t\tdataForReaders = new ArrayList();\n" +
                "\t\ttempCorrelation.put(\"data_for_readers\", dataForReaders);\n" +
                "\t}        \t\t\t\n" +
                "\tMap reader = new HashMap();\n" +
                "\tif (applicationName != null) {\n" +
                "\t\treader.put(\"name\", applicationName);\n" +
                "\t}\n" +
                "\treader.put(\"handling_time\", handlingTime);\n" +
                "\tdataForReaders.add(reader);\n" +
                "}\n" +
                "\n" +
                "void addDataForWriter(Map source, long handlingTime, String applicationName) {\n" +
                "\tMap tempCorrelation = (Map)source.get(\"temp_for_correlations\");\n" +
                "\tif (tempCorrelation == null) {\n" +
                "\t\ttempCorrelation = new HashMap();\n" +
                "\t\tsource.put(\"temp_for_correlations\", tempCorrelation);\n" +
                "\t}\n" +
                "\tList dataForWriters = tempCorrelation.get(\"data_for_writers\");\n" +
                "\tif (dataForWriters == null) {\n" +
                "\t\tdataForWriters = new ArrayList();\n" +
                "\t\ttempCorrelation.put(\"data_for_writers\", dataForWriters);\n" +
                "\t}        \t\t\t\n" +
                "\tMap writer = new HashMap();\n" +
                "\tif (applicationName != null) {\n" +
                "\t\twriter.put(\"name\", applicationName);\n" +
                "\t}\n" +
                "\twriter.put(\"handling_time\", handlingTime);\n" +
                "\tdataForWriters.add(writer);\n" +
                "}\n" +
                "\n" +
                "boolean mainMethod(Map ctx, Map params) {\n" +
                "    Map inputSource = (Map)params.get(\"source\");\n" +
                "    Map targetSource = (Map)ctx.get(\"_source\");\n" +
                "    String inputEventId = (String)params.get(\"event_id\");\n" +
                "\n" +
                "    List correlations = (List)targetSource.get(\"correlations\");\n" +
                "    // Add the ID as a correlation.\n" +
                "    if (correlations == null) {\n" +
                "        correlations = new ArrayList();\n" +
                "        targetSource.put(\"correlations\", correlations);\n" +
                "    }\n" +
                "    if (!correlations.contains(inputEventId)) {\n" +
                "        correlations.add(inputEventId);\n" +
                "    }\n" +
                "    // Merge the response times back in the endpoints.\n" +
                "    List inputEndpoints = (List)inputSource.get(\"endpoints\");\n" +
                "    List targetEndpoints = (List)targetSource.get(\"endpoints\");\n" +
                "    if (inputEndpoints != null) {\n" +
                "        for (Map inputEndpoint : inputEndpoints) {\n" +
                "            Map inputWritingEndpointHandler = getWritingEndpointHandler( (List)inputEndpoint.get(\"endpoint_handlers\") );\n" +
                "            if (inputWritingEndpointHandler != null &&\n" +
                "                (inputWritingEndpointHandler.get(\"application\") == null || ((Map)inputWritingEndpointHandler.get(\"application\")).get(\"name\") == null)) {\n" +
                "                // This is a writer on an endpoint without an application defined. Update the response time on target readers + writers without an application defined.\n" +
                "                long writerHandlingTime = (long)inputWritingEndpointHandler.get(\"handling_time\");\n" +
                "                boolean readerFound = false;\n" +
                "                if (targetEndpoints != null) {\n" +
                "                    for (Map targetEndpoint : targetEndpoints) {\n" +
                "                        readerFound = setResponseTimeOnReadingEndpointHandlers((List)targetEndpoint.get(\"endpoint_handlers\"), null, writerHandlingTime) || readerFound;\n" +
                "                    }\n" +
                "                }\n" +
                "                boolean writerFound = false;\n" +
                "                if (!readerFound && targetEndpoints != null) {\n" +
                "                    for (Map targetEndpoint : targetEndpoints) {\n" +
                "                        Map targetWritingEndpointHandler = getWritingEndpointHandler( (List)targetEndpoint.get(\"endpoint_handlers\") );\n" +
                "                        if (targetWritingEndpointHandler != null && targetWritingEndpointHandler.get(\"application\") == null) {\n" +
                "                            long responseTime = writerHandlingTime - (long)targetWritingEndpointHandler.get(\"handling_time\");\n" +
                "                            if (responseTime >=0 ) {\n" +
                "                                targetWritingEndpointHandler.put(\"response_time\", responseTime);\n" +
                "                                writerFound = true;\n" +
                "                            }\n" +
                "                        }\n" +
                "                    }\n" +
                "                }\n" +
                "                if (!readerFound && !writerFound) {\n" +
                "                    addDataForReaders(targetSource, writerHandlingTime, null);\n" +
                "                }\n" +
                "            } else if (inputWritingEndpointHandler != null &&\n" +
                "                inputWritingEndpointHandler.get(\"application\") != null &&\n" +
                "                ((Map)inputWritingEndpointHandler.get(\"application\")).get(\"name\") != null) {\n" +
                "                // This is a writer on an endpoint with an application defined. Update the response time on target readers with the same application defined.\n" +
                "                String writerAppName = (String)((Map)inputWritingEndpointHandler.get(\"application\")).get(\"name\");\n" +
                "                long writerHandlingTime = (long)inputWritingEndpointHandler.get(\"handling_time\");\n" +
                "                // The name of the application that has written the response is found. Now try to find that application in the reading endpoint handlers of the request.\n" +
                "                boolean readerFound = false;\n" +
                "                if (targetEndpoints != null) {\n" +
                "                    for (Map targetEndpoint : targetEndpoints) {\n" +
                "                        readerFound = setResponseTimeOnReadingEndpointHandlers((List)targetEndpoint.get(\"endpoint_handlers\"), writerAppName, writerHandlingTime) || readerFound;\n" +
                "                    }\n" +
                "                }\n" +
                "                if (!readerFound) {\n" +
                "                    addDataForReaders(targetSource, writerHandlingTime, writerAppName);\n" +
                "                }\n" +
                "            }\n" +
                "            List inputEndpointHandlers = (List)inputEndpoint.get(\"endpoint_handlers\");\n" +
                "            if (inputEndpointHandlers != null) {\n" +
                "                for (Map inputEndpointHandler : inputEndpointHandlers) {\n" +
                "                    if (!\"READER\".equals(inputEndpointHandler.get(\"type\"))) {\n" +
                "                        continue;\n" +
                "                    }\n" +
                "                    if (inputEndpointHandler.get(\"application\") == null || ((Map)inputEndpointHandler.get(\"application\")).get(\"name\") == null) {\n" +
                "                        // This is a reader on an endpoint without an application defined. Update the response time on target readers + writers without an application defined.\n" +
                "                        long readerHandlingTime = (long)inputEndpointHandler.get(\"handling_time\");\n" +
                "                        boolean writerFound = setResponseTimeOnWritingEndpointHandlers(targetEndpoints, null, readerHandlingTime);\n" +
                "                        boolean readerFound = false;\n" +
                "                        if (!writerFound && targetEndpoints != null) {\n" +
                "                            for (Map targetEndpoint : targetEndpoints) {\n" +
                "                                readerFound = setResponseTimeOnReadingEndpointHandlers((List)targetEndpoint.get(\"endpoint_handlers\"), null, readerHandlingTime) || readerFound;\n" +
                "                            }\n" +
                "                        }\n" +
                "                        if (!readerFound && !writerFound) {\n" +
                "                            addDataForWriter(targetSource, readerHandlingTime, null);\n" +
                "                        }\n" +
                "                    } else if (inputEndpointHandler.get(\"application\") != null && ((Map)inputEndpointHandler.get(\"application\")).get(\"name\") != null) {\n" +
                "                        // This is a reader on an endpoint with an application defined. Update the response time on target writer with the same application defined.\n" +
                "                        String readerAppName = (String)((Map)inputEndpointHandler.get(\"application\")).get(\"name\");\n" +
                "                        long readerHandlingTime = (long)inputEndpointHandler.get(\"handling_time\");\n" +
                "                        // The name of the application that has read the response is found. Now try to find that application in the writing endpoint handlers of the request.\n" +
                "                        boolean writerFound = setResponseTimeOnWritingEndpointHandlers(targetEndpoints, readerAppName, readerHandlingTime);\n" +
                "                        if (!writerFound) {\n" +
                "                            addDataForWriter(targetSource, readerHandlingTime, readerAppName);\n" +
                "                        }\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "    return true;\n" +
                "}\n" +
                "mainMethod(ctx, params);";
    }

    private String createUpdateNodeScript() {
        return "String nodeName = (String)params.get(\"node_name\");\n" +
                "String instanceName = (String)params.get(\"instance\");\n" +
                "\n" +
                "if (ctx._source.node == null) {\n" +
                "    ctx._source.object_type = 'node';\n" +
                "    ctx._source.node = new HashMap();\n" +
                "    ctx._source.node.name = nodeName;\n" +
                "}\n" +
                "\n" +
                "if (ctx._source.node.instances == null) {\n" +
                "    ctx._source.node.instances = new ArrayList();\n" +
                "}\n" +
                "\n" +
                "List instances = (List)ctx._source.node.instances;\n" +
                "Optional optional = instances.stream().filter(i -> instanceName.equals(i.name)).findFirst();\n" +
                "if (optional.isPresent()) {\n" +
                "    optional.get().put('last_seen', System.currentTimeMillis());\n" +
                "} else {\n" +
                "    Map instance = new HashMap();\n" +
                "    instance.put('name', instanceName);\n" +
                "    instance.put('last_seen', System.currentTimeMillis());\n" +
                "    instances.add(instance);\n" +
                "}\n" +
                "\n" +
                "Iterator iterator = instances.iterator();\n" +
                "while (iterator.hasNext()) {\n" +
                "    Map instance = iterator.next();\n" +
                "    if (instance.get('last_seen') < System.currentTimeMillis() - 3600000) {\n" +
                "        iterator.remove();\n" +
                "    }\n" +
                "}";
    }

    public void addConfigurationChangeNotificationListener(EtmConfiguration etmConfiguration) {
        if (this.etmConfiguration != null) {
            throw new IllegalStateException("Listener already added");
        }
        this.etmConfiguration = etmConfiguration;
        this.etmConfiguration.addConfigurationChangeListener(this);
    }

    public void removeConfigurationChangeNotificationListener() {
        this.etmConfiguration.removeConfigurationChangeListener(this);
    }

    private void reinitializeTemplates() {
        creatEtmEventIndexTemplate(false, this.etmConfiguration.getShardsPerIndex(), this.etmConfiguration.getReplicasPerIndex());
        creatEtmMetricsIndexTemplate(false, this.etmConfiguration.getReplicasPerIndex());
        creatEtmAuditLogIndexTemplate(false, this.etmConfiguration.getReplicasPerIndex());
        creatEtmConfigurationIndexTemplate(false, this.etmConfiguration.getReplicasPerIndex());
        creatEtmStateIndexTemplate(false, this.etmConfiguration.getReplicasPerIndex());
    }

    @Override
    public void configurationChanged(ConfigurationChangedEvent event) {
        if (event.isAnyChanged(EtmConfiguration.CONFIG_KEY_SHARDS_PER_INDEX, EtmConfiguration.CONFIG_KEY_REPLICAS_PER_INDEX)) {
            reinitializeTemplates();
        }
        if (event.isChanged(EtmConfiguration.CONFIG_KEY_REPLICAS_PER_INDEX)) {
            GetIndexResponse response = this.dataRepository.indicesGet(new GetIndexRequestBuilder(ElasticsearchLayout.ETM_INDEX_PREFIX + "*"));
            String[] indices = response.getIndices();
            if (indices != null && indices.length > 0) {

                UpdateIndexSettingsRequestBuilder builder = new UpdateIndexSettingsRequestBuilder().setIndices(indices)
                        .setSettings(Settings.builder().put("index.number_of_replicas", this.etmConfiguration.getReplicasPerIndex()));
                this.dataRepository.indicesUpdateSettings(builder);
            }
        }
    }

}
