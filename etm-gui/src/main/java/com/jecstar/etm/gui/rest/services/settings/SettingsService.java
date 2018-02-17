package com.jecstar.etm.gui.rest.services.settings;

import com.jecstar.etm.gui.rest.AbstractJsonService;
import com.jecstar.etm.gui.rest.export.*;
import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import com.jecstar.etm.gui.rest.services.search.DefaultSearchTemplates;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.EndpointConfiguration;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.configuration.LdapConfiguration;
import com.jecstar.etm.server.core.domain.configuration.License;
import com.jecstar.etm.server.core.domain.configuration.converter.json.EtmConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.domain.configuration.converter.json.LdapConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.EndpointConfigurationConverter;
import com.jecstar.etm.server.core.domain.converter.json.EndpointConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.domain.parser.ExpressionParser;
import com.jecstar.etm.server.core.domain.parser.ExpressionParserField;
import com.jecstar.etm.server.core.domain.parser.converter.ExpressionParserConverter;
import com.jecstar.etm.server.core.domain.parser.converter.json.ExpressionParserConverterJsonImpl;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.server.core.enhancers.DefaultField;
import com.jecstar.etm.server.core.enhancers.DefaultTelemetryEventEnhancer;
import com.jecstar.etm.server.core.ldap.Directory;
import com.jecstar.etm.server.core.util.BCrypt;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.text.NumberFormat;
import java.util.*;
import java.util.Map.Entry;

@Path("/settings")
@DeclareRoles(SecurityRoles.ALL_ROLES)
public class SettingsService extends AbstractJsonService {

    private final EtmConfigurationConverterJsonImpl etmConfigurationConverter = new EtmConfigurationConverterJsonImpl();
    private final LdapConfigurationConverterJsonImpl ldapConfigurationConverter = new LdapConfigurationConverterJsonImpl();
    private final ExpressionParserConverter<String> expressionParserConverter = new ExpressionParserConverterJsonImpl();
    private final EndpointConfigurationConverter<String> endpointConfigurationConverter = new EndpointConfigurationConverterJsonImpl();
    private final EtmPrincipalConverterJsonImpl etmPrincipalConverter = new EtmPrincipalConverterJsonImpl();
    private final EtmPrincipalTags etmPrincipalTags = this.etmPrincipalConverter.getTags();

    private final FieldLayout[] exportPrincipalFields = new FieldLayout[]{
            new FieldLayout("Account ID", ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.etmPrincipalTags.getIdTag(), FieldType.PLAIN, MultiSelect.FIRST),
            new FieldLayout("Name", ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.etmPrincipalTags.getNameTag(), FieldType.PLAIN, MultiSelect.FIRST),
            new FieldLayout("E-mail", ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.etmPrincipalTags.getEmailTag(), FieldType.PLAIN, MultiSelect.FIRST)
    };

    private final String parserInEnpointTag = ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT +
            "." + this.endpointConfigurationConverter.getTags().getEnhancerTag() +
            "." + this.endpointConfigurationConverter.getTags().getFieldsTag() +
            "." + this.endpointConfigurationConverter.getTags().getParsersTag() +
            "." + this.endpointConfigurationConverter.getTags().getNameTag();

    private static Client client;
    private static EtmConfiguration etmConfiguration;

    public static void initialize(Client client, EtmConfiguration etmConfiguration) {
        SettingsService.client = client;
        SettingsService.etmConfiguration = etmConfiguration;
    }

    @GET
    @Path("/license")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.LICENSE_READ, SecurityRoles.LICENSE_READ_WRITE})
    public String getLicense() {
        EtmPrincipal etmPrincipal = getEtmPrincipal();
        License license = etmConfiguration.getLicense();
        if (license == null) {
            return null;
        }
        NumberFormat numberFormat = getEtmPrincipal().getNumberFormat();
        StringBuilder result = new StringBuilder();
        result.append("{");
        boolean added = addStringElementToJsonBuffer("owner", license.getOwner(), result, true);
        added = addLongElementToJsonBuffer("expiration_date", license.getExpiryDate().toEpochMilli(), result, !added) || added;
        added = addStringElementToJsonBuffer("time_zone", etmPrincipal.getTimeZone().getID(), result, !added) || added;
        added = addLongElementToJsonBuffer("max_events_per_day", license.getMaxEventsPerDay(), result, !added) || added;
        added = addStringElementToJsonBuffer("max_events_per_day_as_text", numberFormat.format(license.getMaxEventsPerDay()), result, !added) || added;
        added = addLongElementToJsonBuffer("max_size_per_day", license.getMaxSizePerDay(), result, !added) || added;
        added = addStringElementToJsonBuffer("max_size_per_day_as_text", numberFormat.format(license.getMaxSizePerDay()), result, !added) || added;
        result.append("}");
        return result.toString();
    }

    @PUT
    @Path("/license")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.LICENSE_READ_WRITE)
    public String setLicense(String json) {
        Map<String, Object> requestValues = toMap(json);
        String licenseKey = getString("key", requestValues);
        etmConfiguration.setLicenseKey(licenseKey);
        Map<String, Object> licenseObject = new HashMap<>();
        Map<String, Object> values = new HashMap<>();
        values.put(this.etmConfigurationConverter.getTags().getLicenseTag(), licenseKey);
        licenseObject.put(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LICENSE, values);
        client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LICENSE_DEFAULT)
                .setDoc(licenseObject)
                .setDocAsUpsert(true)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
                .get();
        // Because the access to the etmConfiguration in the above statement could cause a reload of the configuration
        // the old license may still be applied. To prevent this, we set the license again at this place.
        etmConfiguration.setLicenseKey(licenseKey);
        EtmPrincipal etmPrincipal = getEtmPrincipal();
        NumberFormat numberFormat = etmPrincipal.getNumberFormat();
        License license = etmConfiguration.getLicense();
        StringBuilder result = new StringBuilder();
        result.append("{");
        boolean added = addStringElementToJsonBuffer("status", "success", result, true);
        added = addStringElementToJsonBuffer("owner", license.getOwner(), result, !added) || added;
        added = addLongElementToJsonBuffer("expiration_date", license.getExpiryDate().toEpochMilli(), result, !added) || added;
        added = addStringElementToJsonBuffer("time_zone", etmPrincipal.getTimeZone().getID(), result, !added) || added;
        added = addLongElementToJsonBuffer("max_events_per_day", license.getMaxEventsPerDay(), result, !added) || added;
        added = addStringElementToJsonBuffer("max_events_per_day_as_text", numberFormat.format(license.getMaxEventsPerDay()), result, !added) || added;
        added = addLongElementToJsonBuffer("max_size_per_day", license.getMaxSizePerDay(), result, !added) || added;
        added = addStringElementToJsonBuffer("max_size_per_day_as_text", numberFormat.format(license.getMaxSizePerDay()), result, !added) || added;
        result.append("}");
        return result.toString();
    }

    @GET
    @Path("/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.CLUSTER_SETTINGS_READ, SecurityRoles.CLUSTER_SETTINGS_READ_WRITE})
    public String getClusterConfiguration() {
        GetResponse getResponse = client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_NODE_DEFAULT)
                .setFetchSource(true)
                .get();
        EtmConfiguration config = this.etmConfigurationConverter.read(null, getResponse.getSourceAsString(), null);
        return toStringWithoutNamespace(this.etmConfigurationConverter.write(null, config), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE);
    }

    @PUT
    @Path("/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.CLUSTER_SETTINGS_READ_WRITE)
    public String setClusterConfiguration(String json) {
        GetResponse getResponse = client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_NODE_DEFAULT)
                .setFetchSource(true)
                .get();
        Map<String, Object> currentNodeObject = getResponse.getSourceAsMap();
        Map<String, Object> currentValues = (Map<String, Object>) currentNodeObject.get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE);
        // Overwrite the values with the new values.
        currentValues.putAll(toMap(json));
        EtmConfiguration defaultConfig = this.etmConfigurationConverter.read(null, currentNodeObject, null);

        client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_NODE_DEFAULT)
                .setDoc(this.etmConfigurationConverter.write(null, defaultConfig), XContentType.JSON)
                .setDocAsUpsert(true)
                .setDetectNoop(true)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
                .get();
        return "{\"status\":\"success\"}";
    }

    @GET
    @Path("/ldap")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.CLUSTER_SETTINGS_READ, SecurityRoles.CLUSTER_SETTINGS_READ_WRITE})
    public String getLdapConfiguration() {
        GetResponse getResponse = client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LDAP_DEFAULT)
                .setFetchSource(true)
                .get();
        if (!getResponse.isExists()) {
            return null;
        }
        LdapConfiguration config = this.ldapConfigurationConverter.read(getResponse.getSourceAsString());
        return toStringWithoutNamespace(this.ldapConfigurationConverter.write(config), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LDAP);
    }

    @PUT
    @Path("/ldap")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.CLUSTER_SETTINGS_READ_WRITE)
    public String setLdapConfiguration(String json) {
        LdapConfiguration config = this.ldapConfigurationConverter.read(toStringWithNamespace(json, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LDAP));
        testLdapConnection(config);
        client.prepareIndex(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LDAP_DEFAULT)
                .setSource(this.ldapConfigurationConverter.write(config), XContentType.JSON)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .get();
        if (etmConfiguration.getDirectory() != null) {
            etmConfiguration.getDirectory().merge(config);
        } else {
            Directory directory = new Directory(config);
            etmConfiguration.setDirectory(directory);
        }
        return "{\"status\":\"success\"}";
    }

    @PUT
    @Path("/ldap/test")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.CLUSTER_SETTINGS_READ_WRITE)
    public String testLdapConfiguration(String json) {
        LdapConfiguration config = this.ldapConfigurationConverter.read(toStringWithNamespace(json, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LDAP));
        testLdapConnection(config);
        return "{\"status\":\"success\"}";
    }

    @DELETE
    @Path("/ldap")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.CLUSTER_SETTINGS_READ_WRITE)
    public String deleteLdapConfiguration() {
        client.prepareDelete(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LDAP_DEFAULT)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .get();
        etmConfiguration.setDirectory(null);
        return "{\"status\":\"success\"}";
    }

    /**
     * Test a configuration by setting up a connection.
     *
     * @param config The configuration to test.
     */
    private void testLdapConnection(LdapConfiguration config) {
        try (Directory directory = new Directory(config)) {
            directory.test();
        }
    }

    @GET
    @Path("/nodes")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.NODE_SETTINGS_READ, SecurityRoles.NODE_SETTINGS_READ_WRITE})
    public String getNodes() {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
        ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
        if (!scrollableSearch.hasNext()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        result.append("{\"nodes\": [");
        boolean first = true;
        for (SearchHit searchHit : scrollableSearch) {
            if (searchHit.getId().equalsIgnoreCase(ElasticsearchLayout.CONFIGURATION_OBJECT_ID_NODE_DEFAULT)) {
                continue;
            }
            if (!first) {
                result.append(",");
            }
            result.append(toStringWithoutNamespace(searchHit.getSourceAsMap(), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE));
            first = false;
        }
        result.append("]}");
        return result.toString();
    }

    @PUT
    @Path("/node/{nodeName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.NODE_SETTINGS_READ_WRITE)
    public String addNode(@PathParam("nodeName") String nodeName, String json) {
        // Do a read and write of the node to make sure it's valid.
        GetResponse defaultSettingsResponse = client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_NODE_DEFAULT)
                .setFetchSource(true)
                .get();
        EtmConfiguration defaultConfig = this.etmConfigurationConverter.read(null, defaultSettingsResponse.getSourceAsString(), null);
        EtmConfiguration nodeConfig = this.etmConfigurationConverter.read(toStringWithNamespace(json, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE), defaultSettingsResponse.getSourceAsString(), nodeName);

        client.prepareIndex(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE_ID_PREFIX + nodeName)
                .setSource(this.etmConfigurationConverter.write(nodeConfig, defaultConfig), XContentType.JSON)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .get();
        return "{ \"status\": \"success\" }";
    }

    @DELETE
    @Path("/node/{nodeName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.NODE_SETTINGS_READ_WRITE)
    public String deleteNode(@PathParam("nodeName") String nodeName) {
        if (ElasticsearchLayout.ETM_OBJECT_NAME_DEFAULT.equalsIgnoreCase(nodeName)) {
            return "{\"status\":\"failed\"}";
        }
        client.prepareDelete(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE_ID_PREFIX + nodeName)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .get();
        return "{\"status\":\"success\"}";
    }


    @GET
    @Path("/indicesstats")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.INDEX_STATISTICS_READ)
    public String getIndexStatistics() {
        IndicesStatsResponse indicesStatsResponse = client.admin().indices().prepareStats("etm_event_*")
                .clear()
                .setStore(true)
                .setDocs(true)
                .setIndexing(true)
                .setSearch(true)
                .get(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
        NumberFormat numberFormat = getEtmPrincipal().getNumberFormat();
        StringBuilder result = new StringBuilder();
        result.append("{");
        result.append("\"d3_formatter\": ").append(getD3Formatter());
        result.append(",\"totals\": {");
        addLongElementToJsonBuffer("document_count", indicesStatsResponse.getTotal().docs.getCount() - indicesStatsResponse.getTotal().docs.getDeleted(), result, true);
        addStringElementToJsonBuffer("document_count_as_string", numberFormat.format(indicesStatsResponse.getTotal().docs.getCount() - indicesStatsResponse.getTotal().docs.getDeleted()), result, false);
        addLongElementToJsonBuffer("size_in_bytes", indicesStatsResponse.getTotal().store.getSizeInBytes(), result, false);
        addStringElementToJsonBuffer("size_in_bytes_as_string", numberFormat.format(indicesStatsResponse.getTotal().store.getSizeInBytes()), result, false);
        result.append("}, \"indices\": [");
        Map<String, IndexStats> indices = indicesStatsResponse.getIndices();
        boolean firstEntry = true;
        for (Entry<String, IndexStats> entry : indices.entrySet()) {
            if (!firstEntry) {
                result.append(",");
            }
            result.append("{");
            addStringElementToJsonBuffer("name", entry.getKey(), result, true);
            addLongElementToJsonBuffer("document_count", entry.getValue().getTotal().docs.getCount(), result, false);
            addStringElementToJsonBuffer("document_count_as_string", numberFormat.format(entry.getValue().getTotal().docs.getCount()), result, false);
            addLongElementToJsonBuffer("size_in_bytes", entry.getValue().getTotal().store.getSizeInBytes(), result, false);
            addStringElementToJsonBuffer("size_in_bytes_as_string", numberFormat.format(entry.getValue().getTotal().store.getSizeInBytes()), result, false);

            long count = entry.getValue().getTotal().indexing.getTotal().getIndexCount();
            if (count != 0) {
                long averageIndexTime = entry.getValue().getTotal().indexing.getTotal().getIndexTime().millis() / count;
                addLongElementToJsonBuffer("average_index_time", averageIndexTime, result, false);
                addStringElementToJsonBuffer("average_index_time_as_string", numberFormat.format(averageIndexTime), result, false);
            }

            count = entry.getValue().getTotal().search.getTotal().getQueryCount();
            if (count != 0) {
                long averageSearchTime = entry.getValue().getTotal().search.getTotal().getQueryTimeInMillis() / count;
                addLongElementToJsonBuffer("average_search_time", averageSearchTime, result, false);
                addStringElementToJsonBuffer("average_search_time_as_string", numberFormat.format(averageSearchTime), result, false);
            }

            result.append("}");
            firstEntry = false;
        }
        result.append("]}");
        return result.toString();
    }

    @GET
    @Path("/parsers")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.PARSER_SETTINGS_READ, SecurityRoles.PARSER_SETTINGS_READ_WRITE, SecurityRoles.ENDPOINT_SETTINGS_READ, SecurityRoles.ENDPOINT_SETTINGS_READ_WRITE})
    public String getParsers() {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
        ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
        if (!scrollableSearch.hasNext()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        result.append("{\"parsers\": [");
        boolean first = true;
        for (SearchHit searchHit : scrollableSearch) {
            if (!first) {
                result.append(",");
            }
            result.append(toStringWithoutNamespace(searchHit.getSourceAsMap(), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER));
            first = false;
        }
        result.append("]}");
        return result.toString();
    }

    @GET
    @Path("/parserfields")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.PARSER_SETTINGS_READ, SecurityRoles.PARSER_SETTINGS_READ_WRITE, SecurityRoles.ENDPOINT_SETTINGS_READ, SecurityRoles.ENDPOINT_SETTINGS_READ_WRITE})
    public String getParserFields() {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        result.append("{\"parserfields\": [");
        for (ExpressionParserField field : ExpressionParserField.values()) {
            if (!first) {
                result.append(",");
            }
            result.append("{");
            addStringElementToJsonBuffer("name", field.getJsonTag(), result, true);
            result.append("}");
            first = false;
        }
        result.append("]}");
        return result.toString();
    }


    @DELETE
    @Path("/parser/{parserName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.PARSER_SETTINGS_READ_WRITE)
    public String deleteParser(@PathParam("parserName") String parserName) {
        BulkRequestBuilder bulkRequestBuilder = client.prepareBulk()
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration));
        bulkRequestBuilder.add(client.prepareDelete(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER_ID_PREFIX + parserName)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
        );
        removeParserFromEndpoints(bulkRequestBuilder, parserName);
        bulkRequestBuilder.get();
        return "{\"status\":\"success\"}";
    }

    private void removeParserFromEndpoints(BulkRequestBuilder bulkRequestBuilder, String parserName) {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                .setFetchSource(true)
                .setQuery(QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT))
                        .must(QueryBuilders.termQuery(parserInEnpointTag, parserName)))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
        ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
        for (SearchHit searchHit : scrollableSearch) {
            boolean updated = false;
            EndpointConfiguration endpointConfig = this.endpointConfigurationConverter.read(searchHit.getSourceAsString());
            if (endpointConfig.eventEnhancer instanceof DefaultTelemetryEventEnhancer) {
                DefaultTelemetryEventEnhancer enhancer = (DefaultTelemetryEventEnhancer) endpointConfig.eventEnhancer;
                for (DefaultField field : enhancer.getFields()) {
                    Iterator<ExpressionParser> it = field.getParsers().iterator();
                    while (it.hasNext()) {
                        ExpressionParser parser = it.next();
                        if (parser.getName().equals(parserName)) {
                            it.remove();
                            updated = true;
                        }
                    }
                }
            }
            if (updated) {
                bulkRequestBuilder.add(createEndpointUpdateRequest(endpointConfig));
            }
        }
    }

    @PUT
    @Path("/parser/{parserName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.PARSER_SETTINGS_READ_WRITE)
    public String addParser(@PathParam("parserName") String parserName, String json) {
        BulkRequestBuilder bulkRequestBuilder = client.prepareBulk()
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration));
        // Do a read and write of the parser to make sure it's valid.
        ExpressionParser expressionParser = this.expressionParserConverter.read(toStringWithNamespace(json, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER));

        bulkRequestBuilder.add(client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.ETM_DEFAULT_TYPE,
                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER_ID_PREFIX + parserName)
                .setDoc(this.expressionParserConverter.write(expressionParser), XContentType.JSON)
                .setDocAsUpsert(true)
                .setDetectNoop(true)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
        );
        updateParserInEndpoints(bulkRequestBuilder, expressionParser);
        bulkRequestBuilder.get();
        return "{ \"status\": \"success\" }";
    }

    private void updateParserInEndpoints(BulkRequestBuilder bulkRequestBuilder, ExpressionParser expressionParser) {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                .setFetchSource(true)
                .setQuery(QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT))
                        .must(QueryBuilders.termQuery(parserInEnpointTag, expressionParser.getName()))
                )
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
        ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
        for (SearchHit searchHit : scrollableSearch) {
            boolean updated = false;
            EndpointConfiguration endpointConfig = this.endpointConfigurationConverter.read(searchHit.getSourceAsString());
            if (endpointConfig.eventEnhancer instanceof DefaultTelemetryEventEnhancer) {
                DefaultTelemetryEventEnhancer enhancer = (DefaultTelemetryEventEnhancer) endpointConfig.eventEnhancer;
                for (DefaultField field : enhancer.getFields()) {
                    ListIterator<ExpressionParser> it = field.getParsers().listIterator();
                    while (it.hasNext()) {
                        ExpressionParser parser = it.next();
                        if (parser.getName().equals(expressionParser.getName())) {
                            it.set(expressionParser);
                            updated = true;
                        }
                    }
                }
            }
            if (updated) {
                bulkRequestBuilder.add(createEndpointUpdateRequest(endpointConfig));
            }
        }
    }

    @GET
    @Path("/endpoints")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ENDPOINT_SETTINGS_READ, SecurityRoles.ENDPOINT_SETTINGS_READ_WRITE})
    public String getEndpoints() {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
        ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
        StringBuilder result = new StringBuilder();
        result.append("{\"endpoints\": [");
        boolean first = true;
        boolean defaultFound = false;
        for (SearchHit searchHit : scrollableSearch) {
            if (!first) {
                result.append(",");
            }
            result.append(toStringWithoutNamespace(searchHit.getSourceAsMap(), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT));
            if (ElasticsearchLayout.CONFIGURATION_OBJECT_ID_ENDPOINT_DEFAULT.equals(searchHit.getId())) {
                defaultFound = true;
            }
            first = false;
        }
        if (!defaultFound) {
            if (!first) {
                result.append(",");
            }
            EndpointConfiguration defaultEndpointConfiguration = new EndpointConfiguration();
            defaultEndpointConfiguration.name = "*";
            defaultEndpointConfiguration.eventEnhancer = new DefaultTelemetryEventEnhancer();
            result.append(this.endpointConfigurationConverter.write(defaultEndpointConfiguration));
        }

        result.append("]}");
        return result.toString();
    }


    @DELETE
    @Path("/endpoint/{endpointName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.ENDPOINT_SETTINGS_READ_WRITE)
    public String deleteEndpoint(@PathParam("endpointName") String endpointName) {
        client.prepareDelete(ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.ETM_DEFAULT_TYPE,
                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT_ID_PREFIX + endpointName)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .get();
        return "{\"status\":\"success\"}";
    }

    @PUT
    @Path("/endpoint/{endpointName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.ENDPOINT_SETTINGS_READ_WRITE)
    public String addEndpoint(@PathParam("endpointName") String endpointName, String json) {
        // Do a read and write of the endpoint to make sure it's valid.
        EndpointConfiguration endpointConfiguration = this.endpointConfigurationConverter.read(toStringWithNamespace(json, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT));
        createEndpointUpdateRequest(endpointConfiguration).get();
        return "{ \"status\": \"success\" }";
    }

    private UpdateRequestBuilder createEndpointUpdateRequest(EndpointConfiguration endpointConfiguration) {
        return client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.ETM_DEFAULT_TYPE,
                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT_ID_PREFIX + endpointConfiguration.name)
                .setDoc(this.endpointConfigurationConverter.write(endpointConfiguration), XContentType.JSON)
                .setDocAsUpsert(true)
                .setDetectNoop(true)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .setRetryOnConflict(etmConfiguration.getRetryOnConflictCount());
    }

    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.USER_SETTINGS_READ, SecurityRoles.USER_SETTINGS_READ_WRITE})
    public String getUsers() {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                .setFetchSource(new String[]{"*"}, new String[]{this.etmPrincipalTags.getPasswordHashTag(), this.etmPrincipalTags.getSearchTemplatesTag(), this.etmPrincipalTags.getSearchHistoryTag()})
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
        ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
        if (!scrollableSearch.hasNext()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        result.append("{\"users\": [");
        boolean first = true;
        for (SearchHit searchHit : scrollableSearch) {
            if (!first) {
                result.append(",");
            }
            result.append(toStringWithoutNamespace(searchHit.getSourceAsMap(), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER));
            first = false;
        }
        result.append("], \"has_ldap\": ").append(etmConfiguration.getDirectory() != null).append("}");
        return result.toString();
    }

    @GET
    @Path("/download/users")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RolesAllowed({SecurityRoles.USER_SETTINGS_READ, SecurityRoles.USER_SETTINGS_READ_WRITE})
    public Response getDownloadUsers(@QueryParam("q") String json) {
        EtmPrincipal etmPrincipal = getEtmPrincipal();
        Map<String, Object> valueMap = toMap(json);
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                .setFetchSource(Arrays.stream(this.exportPrincipalFields).map(FieldLayout::getField).toArray(String[]::new), null)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
        ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
        if (!scrollableSearch.hasNext()) {
            return null;
        }
        FileType fileType = FileType.valueOf(getString("fileType", valueMap).toUpperCase());
        File result = new QueryExporter().exportToFile(scrollableSearch, fileType, Integer.MAX_VALUE, etmPrincipal, this.exportPrincipalFields);
        scrollableSearch.clearScrollIds();
        Response.ResponseBuilder response = Response.ok(result);
        response.header("Content-Disposition", "attachment; filename=etm-users." + fileType.name().toLowerCase());
        response.encoding(System.getProperty("file.encoding"));
        response.header("Content-Type", fileType.getContentType());
        return response.build();
    }

    @GET
    @Path("/user/{userId}/ldap/groups")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.USER_SETTINGS_READ, SecurityRoles.USER_SETTINGS_READ_WRITE})
    public String getLdapGroupsOfUser(@PathParam("userId") String userId) {
        if (etmConfiguration.getDirectory() == null) {
            return null;
        }
        EtmPrincipal principal = etmConfiguration.getDirectory().getPrincipal(userId, true);
        StringBuilder result = new StringBuilder();
        result.append("{\"groups\": [");
        boolean first = true;
        Set<EtmGroup> notImportedLdapGroups = getNotImportedLdapGroups();
        for (EtmGroup etmGroup : principal.getGroups()) {
            if (notImportedLdapGroups.contains(etmGroup)) {
                continue;
            }
            if (!first) {
                result.append(",");
            }
            result.append(toStringWithoutNamespace(this.etmPrincipalConverter.writeGroup(etmGroup), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP));
            first = false;
        }
        result.append("]}");
        return result.toString();

    }

    @POST
    @Path("/users/ldap/search")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_SETTINGS_READ_WRITE)
    public String searchLdapUsers(String json) {
        String query = getString("query", toMap(json));
        if (query == null || etmConfiguration.getDirectory() == null) {
            return null;
        }
        List<EtmPrincipal> principals = etmConfiguration.getDirectory().searchPrincipal(query.endsWith("*") ? query : query + "*");
        StringBuilder result = new StringBuilder();
        result.append("[");
        boolean first = true;
        for (EtmPrincipal principal : principals) {
            if (!first) {
                result.append(",");
            }
            result.append("{");
            addStringElementToJsonBuffer("id", principal.getId(), result, true);
            addStringElementToJsonBuffer("label", principal.getName() != null ? principal.getId() + " - " + principal.getName() : principal.getId(), result, false);
            addStringElementToJsonBuffer("value", principal.getId(), result, false);
            result.append("}");
            first = false;
        }
        result.append("]");
        return result.toString();
    }

    @PUT
    @Path("/users/ldap/import/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_SETTINGS_READ_WRITE)
    public String importLdapUser(@PathParam("userId") String userId) {
        if (etmConfiguration.getDirectory() == null) {
            return null;
        }
        EtmPrincipal principal = etmConfiguration.getDirectory().getPrincipal(userId, false);
        if (principal == null) {
            throw new EtmException(EtmException.INVALID_LDAP_USER);
        }
        EtmPrincipal currentPrincipal = loadPrincipal(principal.getId());
        if (currentPrincipal != null) {
            if (currentPrincipal.isLdapBase()) {
                // LDAP user already present. No need to import the user again.
                return null;
            }
            // Merge the current and the LDAP principal.
            currentPrincipal.setPasswordHash(null);
            currentPrincipal.setName(principal.getName());
            currentPrincipal.setEmailAddress(principal.getEmailAddress());
            currentPrincipal.setChangePasswordOnLogon(false);
            principal = currentPrincipal;
        }
        principal.setLdapBase(true);
        client.prepareIndex(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + principal.getId())
                .setSource(this.etmPrincipalConverter.writePrincipal(principal), XContentType.JSON)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .get();
        return this.etmPrincipalConverter.writePrincipal(principal);
    }

    @DELETE
    @Path("/user/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_SETTINGS_READ_WRITE)
    public String deleteUser(@PathParam("userId") String userId) {
        EtmPrincipal principal = loadPrincipal(userId);
        if (principal == null) {
            return null;
        }
        if (principal.isInRole(SecurityRoles.USER_SETTINGS_READ_WRITE)) {
            // The user was and user admin. Check if he/she is the latest admin. If so, block this operation because we don't want a user without the user admin role.
            // This check should be skipped/changed when LDAP is supported.
            if (getNumberOfUsersWithUserAdminRole() <= 1) {
                throw new EtmException(EtmException.NO_MORE_USER_ADMINS_LEFT);
            }
        }
        client.prepareDelete(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .get();
        return "{\"status\":\"success\"}";
    }

    @PUT
    @Path("/user/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_SETTINGS_READ_WRITE)
    public String addUser(@PathParam("userId") String userId, String json) {
        // TODO pagina geeft geen error als een nieuwe gebruiker wordt opgevoerd en geen wachtwoorden worden in gegeven.
        // Do a read and write of the user to make sure it's valid.
        Map<String, Object> valueMap = toMap(json);
        EtmPrincipal newPrincipal = loadPrincipal(toMapWithNamespace(json, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER));
        String newPassword = getString("new_password", valueMap);
        if (newPassword != null) {
            newPrincipal.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        }
        EtmPrincipal currentPrincipal = loadPrincipal(userId);
        if (currentPrincipal != null && currentPrincipal.isInRole(SecurityRoles.USER_SETTINGS_READ_WRITE) && !newPrincipal.isInRole(SecurityRoles.USER_SETTINGS_READ_WRITE)) {
            // The user was admin, but that role is revoked. Check if he/she is the latest admin. If so, block this operation because we don't want a user without the admin role.
            // This check should be skipped/changed when LDAP is supported.
            if (getNumberOfUsersWithUserAdminRole() <= 1) {
                throw new EtmException(EtmException.NO_MORE_USER_ADMINS_LEFT);
            }
        }
        if (currentPrincipal != null) {
            // Copy the ldap base of the original user because it may never be overwritten.
            newPrincipal.setLdapBase(currentPrincipal.isLdapBase());
        }
        client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId)
                .setDoc(this.etmPrincipalConverter.writePrincipal(newPrincipal), XContentType.JSON)
                .setDocAsUpsert(true)
                .setDetectNoop(true)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
                .get();
        if (currentPrincipal == null && etmConfiguration.getMaxSearchTemplateCount() >= 3) {
            // Add some default templates to the user if he/she is able to search.
            client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId)
                    .setDoc(new DefaultSearchTemplates().toJson(), XContentType.JSON)
                    .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                    .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                    .setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
                    .get();
        }
        if (userId.equals(getEtmPrincipal().getId())) {
            getEtmPrincipal().forceReload = true;
        }
        return "{ \"status\": \"success\" }";
    }

    @GET
    @Path("/groups")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.GROUP_SETTINGS_READ, SecurityRoles.GROUP_SETTINGS_READ_WRITE})
    public String getGroups() {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
        ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
        StringBuilder result = new StringBuilder();
        result.append("{\"groups\": [");
        boolean first = true;
        for (SearchHit searchHit : scrollableSearch) {
            if (!first) {
                result.append(",");
            }
            result.append(toStringWithoutNamespace(searchHit.getSourceAsMap(), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP));
            first = false;
        }
        result.append("], \"has_ldap\": ").append(etmConfiguration.getDirectory() != null).append("}");
        return result.toString();
    }

    /**
     * Gives the ldap groups that are not imported yet.
     *
     * @return The non imported ldap groups
     */
    @GET
    @Path("/groups/ldap")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.GROUP_SETTINGS_READ_WRITE)
    public String getLdapGroups() {
        Set<EtmGroup> groups = getNotImportedLdapGroups();
        StringBuilder result = new StringBuilder();
        result.append("{\"groups\": [");
        boolean first = true;
        for (EtmGroup etmGroup : groups) {
            if (!first) {
                result.append(",");
            }
            result.append(this.etmPrincipalConverter.writeGroup(etmGroup));
            first = false;
        }
        result.append("]}");
        return result.toString();
    }


    private Set<EtmGroup> getNotImportedLdapGroups() {
        Set<EtmGroup> groups = etmConfiguration.getDirectory().getGroups();
        Iterator<EtmGroup> iterator = groups.iterator();
        while (iterator.hasNext()) {
            EtmGroup group = iterator.next();
            GetResponse getResponse = client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + group.getName())
                    .get();
            if (getResponse.isExists()) {
                Map<String, Object> sourceAsMap = getResponse.getSourceAsMap();
                if (getBoolean(this.etmPrincipalTags.getLdapBaseTag(), sourceAsMap, Boolean.FALSE)) {
                    iterator.remove();
                }
            }
        }
        return groups;
    }

    @PUT
    @Path("/groups/ldap/import/{groupName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.GROUP_SETTINGS_READ_WRITE)
    public String importLdapGroup(@PathParam("groupName") String groupName) {
        EtmGroup group = etmConfiguration.getDirectory().getGroup(groupName);
        if (group == null) {
            throw new EtmException(EtmException.INVALID_LDAP_GROUP);
        }
        EtmGroup currentGroup = loadGroup(groupName);
        if (currentGroup != null) {
            // Merge the existing group with the new one.
            // Currently no properties need to be merged
            group = currentGroup;
        }
        group.setLdapBase(true);
        client.prepareIndex(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName)
                .setSource(this.etmPrincipalConverter.writeGroup(group), XContentType.JSON)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .get();
        return this.etmPrincipalConverter.writeGroup(group);
    }


    @PUT
    @Path("/group/{groupName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.GROUP_SETTINGS_READ_WRITE)
    public String addGroup(@PathParam("groupName") String groupName, String json) {
        // Do a read and write of the group to make sure it's valid.
        Map<String, Object> objectMap = toMapWithNamespace(json, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP);
        EtmGroup newGroup = this.etmPrincipalConverter.readGroup(objectMap);

        EtmGroup currentGroup = loadGroup(groupName);
        if (currentGroup != null && currentGroup.isInRole(SecurityRoles.USER_SETTINGS_READ_WRITE) && !newGroup.isInRole(SecurityRoles.USER_SETTINGS_READ_WRITE)) {
            // The group had the admin role, but that role is revoked. Check if this removes all the admins. If so, block this operation because we don't want a user without the admin role.
            if (getNumberOfUsersWithUserAdminRole() <= 1) {
                throw new EtmException(EtmException.NO_MORE_USER_ADMINS_LEFT);
            }
        }
        if (currentGroup != null) {
            // Copy the ldap base of the original group because it may never be overwritten.
            newGroup.setLdapBase(currentGroup.isLdapBase());
        }
        client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName)
                .setDoc(this.etmPrincipalConverter.writeGroup(newGroup), XContentType.JSON)
                .setDocAsUpsert(true)
                .setDetectNoop(true)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
                .get();
        // Force a reload of the principal if he/she is in the updated group.
        EtmPrincipal principal = getEtmPrincipal();
        if (principal.isInGroup(groupName)) {
            principal.forceReload = true;
        }
        return "{ \"status\": \"success\" }";
    }

    @DELETE
    @Path("/group/{groupName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.GROUP_SETTINGS_READ_WRITE)
    public String deleteGroup(@PathParam("groupName") String groupName) {
        List<String> adminGroups = getGroupsWithRole(SecurityRoles.USER_SETTINGS_READ_WRITE);
        if (adminGroups.contains(groupName)) {
            // Check if there are admins left if this group is removed.
            // This check should be skipped/changed when LDAP is supported.
            adminGroups.remove(groupName);
            if (getNumberOfUsersWithUserAdminRole(adminGroups) < 1) {
                throw new EtmException(EtmException.NO_MORE_USER_ADMINS_LEFT);
            }
        }
        BulkRequestBuilder bulkRequestBuilder = client.prepareBulk()
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration));
        bulkRequestBuilder.add(client.prepareDelete(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
        );
        removeGroupFromPrincipal(bulkRequestBuilder, groupName);
        bulkRequestBuilder.get();
        // Force a reload of the principal if he/she is in the deleted group.
        EtmPrincipal principal = getEtmPrincipal();
        if (principal.isInGroup(groupName)) {
            principal.forceReload = true;
        }
        return "{\"status\":\"success\"}";
    }

    private void removeGroupFromPrincipal(BulkRequestBuilder bulkRequestBuilder, String groupName) {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                .setFetchSource(false)
                .setQuery(QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP))
                        .must(QueryBuilders.termQuery(this.etmPrincipalTags.getGroupsTag(), groupName)))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
        ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
        for (SearchHit searchHit : scrollableSearch) {
            boolean updated = false;
            EtmPrincipal principal = loadPrincipal(searchHit.getId());
            if (principal != null && principal.isInGroup(groupName)) {
                Iterator<EtmGroup> it = principal.getGroups().iterator();
                while (it.hasNext()) {
                    EtmGroup group = it.next();
                    if (group.getName().equals(groupName)) {
                        it.remove();
                        updated = true;
                    }
                }
            }
            if (updated) {
                bulkRequestBuilder.add(createPrincipalUpdateRequest(principal));
            }
        }
    }

    private UpdateRequestBuilder createPrincipalUpdateRequest(EtmPrincipal principal) {
        return client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + principal.getId())
                .setDoc(this.etmPrincipalConverter.writePrincipal(principal), XContentType.JSON)
                .setDocAsUpsert(true)
                .setDetectNoop(true)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .setRetryOnConflict(etmConfiguration.getRetryOnConflictCount());
    }

    /**
     * Load an <code>EtmPrincipal</code> based on the user id.
     *
     * @param userId The id of the user to load.
     * @return A fully loaded <code>EtmPrincipal</code>, or <code>null</code> if no user with the given userId exists.
     */
    private EtmPrincipal loadPrincipal(String userId) {
        GetResponse getResponse = client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId).get();
        if (!getResponse.isExists()) {
            return null;
        }
        return loadPrincipal(getResponse.getSourceAsMap());
    }

    /**
     * Converts a map with json key/value pairs to an <code>EtmPrincipal</code>
     * and reads the corresponding groups from the database.
     *
     * @param principalValues The map with json key/value pairs.
     * @return A fully loaded <code>EtmPrincipal</code>
     */
    private EtmPrincipal loadPrincipal(Map<String, Object> principalValues) {
        EtmPrincipal principal = this.etmPrincipalConverter.readPrincipal(principalValues);
        Collection<String> groups = getArray(this.etmPrincipalTags.getGroupsTag(), principalValues);
        if (groups != null && !groups.isEmpty()) {
            MultiGetRequestBuilder multiGetBuilder = client.prepareMultiGet();
            for (String group : groups) {
                multiGetBuilder.add(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + group);
            }
            MultiGetResponse multiGetResponse = multiGetBuilder.get();
            for (MultiGetItemResponse item : multiGetResponse) {
                EtmGroup etmGroup = this.etmPrincipalConverter.readGroup(item.getResponse().getSourceAsString());
                principal.addGroup(etmGroup);
            }
        }
        return principal;
    }

    /**
     * Load an <code>EtmGroup</code> based on the group name.
     *
     * @param groupName The name of the group.
     * @return The <code>EtmGroup</code> with the given name, or <code>null</code> when no such group exists.
     */
    private EtmGroup loadGroup(String groupName) {
        GetResponse getResponse = client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName).get();
        if (!getResponse.isExists()) {
            return null;
        }
        return this.etmPrincipalConverter.readGroup(getResponse.getSourceAsMap());
    }

    /**
     * Gives a list with group names that have a gives role.
     *
     * @param roles The roles to search for.
     * @return A list with group names that have one of the given roles, or an
     * empty list if non of the groups has any of the given roles.
     */
    private List<String> getGroupsWithRole(String... roles) {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                .setFetchSource(true)
                .setQuery(QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP))
                        .must(QueryBuilders.termsQuery(this.etmPrincipalTags.getRolesTag(), roles)))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
        ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
        List<String> groups = new ArrayList<>();
        if (scrollableSearch.hasNext()) {
            for (SearchHit searchHit : scrollableSearch) {
                String group = searchHit.getId();
                if (!groups.contains(group)) {
                    groups.add(group);
                }
            }
        }
        return groups;
    }

    /**
     * Gives the number of users with the admin role. This is the number of
     * users with the direct admin role added with the users that are in a group
     * with the admin role.
     *
     * @return The number of users with the user admin role.
     */
    private long getNumberOfUsersWithUserAdminRole() {
        List<String> adminGroups = getGroupsWithRole(SecurityRoles.USER_SETTINGS_READ_WRITE);
        return getNumberOfUsersWithUserAdminRole(adminGroups);
    }

    /**
     * Gives the number of users with the admin role for a given collection of
     * admin groups. This is the number of users with the direct admin role
     * added with the users that are in a group with the admin role.
     *
     * @return The number of users with the user admin role.
     */
    private long getNumberOfUsersWithUserAdminRole(Collection<String> adminGroups) {
        BoolQueryBuilder query = QueryBuilders.boolQuery().must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER));
        if (adminGroups == null || adminGroups.isEmpty()) {
            query.must(QueryBuilders.termQuery(this.etmPrincipalTags.getRolesTag(), SecurityRoles.USER_SETTINGS_READ_WRITE));
        } else {
            query.should(QueryBuilders.termsQuery(this.etmPrincipalTags.getRolesTag(), SecurityRoles.USER_SETTINGS_READ_WRITE))
                    .should(QueryBuilders.termsQuery(this.etmPrincipalTags.getGroupsTag(), adminGroups))
                    .minimumShouldMatch(1);
        }
        SearchResponse response = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                .setFetchSource(false)
                .setSize(0)
                .setQuery(query)
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .get();
        return response.getHits().getTotalHits();
    }
}