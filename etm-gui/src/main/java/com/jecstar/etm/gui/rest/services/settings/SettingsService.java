package com.jecstar.etm.gui.rest.services.settings;

import com.jecstar.etm.gui.rest.AbstractGuiService;
import com.jecstar.etm.gui.rest.export.*;
import com.jecstar.etm.gui.rest.services.search.DefaultSearchTemplates;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.ImportProfile;
import com.jecstar.etm.server.core.domain.cluster.notifier.Notifier;
import com.jecstar.etm.server.core.domain.cluster.notifier.converter.NotifierConverter;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.configuration.LdapConfiguration;
import com.jecstar.etm.server.core.domain.configuration.License;
import com.jecstar.etm.server.core.domain.configuration.converter.LdapConfigurationConverter;
import com.jecstar.etm.server.core.domain.configuration.converter.json.EtmConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.ImportProfileConverter;
import com.jecstar.etm.server.core.domain.converter.json.ImportProfileConverterJsonImpl;
import com.jecstar.etm.server.core.domain.parser.ExpressionParser;
import com.jecstar.etm.server.core.domain.parser.ExpressionParserField;
import com.jecstar.etm.server.core.domain.parser.converter.ExpressionParserConverter;
import com.jecstar.etm.server.core.domain.parser.converter.json.ExpressionParserConverterJsonImpl;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.*;
import com.jecstar.etm.server.core.elasticsearch.domain.IndexStats;
import com.jecstar.etm.server.core.elasticsearch.domain.IndicesStatsResponse;
import com.jecstar.etm.server.core.enhancers.DefaultField;
import com.jecstar.etm.server.core.enhancers.DefaultTelemetryEventEnhancer;
import com.jecstar.etm.server.core.enhancers.DefaultTransformation;
import com.jecstar.etm.server.core.ldap.Directory;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import com.jecstar.etm.server.core.util.BCrypt;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchResponse;
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
public class SettingsService extends AbstractGuiService {

    private final EtmConfigurationConverterJsonImpl etmConfigurationConverter = new EtmConfigurationConverterJsonImpl();
    private final LdapConfigurationConverter ldapConfigurationConverter = new LdapConfigurationConverter();
    private final NotifierConverter notifierConverter = new NotifierConverter();
    private final ExpressionParserConverter<String> expressionParserConverter = new ExpressionParserConverterJsonImpl();
    private final ImportProfileConverter<String> importProfileConverter = new ImportProfileConverterJsonImpl();
    private final EtmPrincipalConverterJsonImpl etmPrincipalConverter = new EtmPrincipalConverterJsonImpl();
    private final EtmPrincipalTags principalTags = this.etmPrincipalConverter.getTags();

    private final List<FieldLayout> exportPrincipalFields = Arrays.asList(
            new FieldLayout("Account ID", ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.principalTags.getIdTag(), FieldType.PLAIN, MultiSelect.FIRST),
            new FieldLayout("Name", ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.principalTags.getNameTag(), FieldType.PLAIN, MultiSelect.FIRST),
            new FieldLayout("E-mail", ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.principalTags.getEmailTag(), FieldType.PLAIN, MultiSelect.FIRST)
    );

    private final String parserInImportProfileFieldsTag = ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE +
            "." + this.importProfileConverter.getTags().getEnhancerTag() +
            "." + this.importProfileConverter.getTags().getFieldsTag() +
            "." + this.importProfileConverter.getTags().getParsersTag() +
            "." + this.importProfileConverter.getTags().getNameTag();

    private final String parserInImportProfileTransformationTag = ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE +
            "." + this.importProfileConverter.getTags().getEnhancerTag() +
            "." + this.importProfileConverter.getTags().getTransformationsTag() +
            "." + this.importProfileConverter.getTags().getParserTag() +
            "." + this.importProfileConverter.getTags().getNameTag();


    private static DataRepository dataRepository;
    private static EtmConfiguration etmConfiguration;

    public static void initialize(DataRepository dataRepository, EtmConfiguration etmConfiguration) {
        SettingsService.dataRepository = dataRepository;
        SettingsService.etmConfiguration = etmConfiguration;
    }

    @GET
    @Path("/license")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.LICENSE_READ, SecurityRoles.LICENSE_READ_WRITE})
    public String getLicense() {
        License license = etmConfiguration.getLicense();
        if (license == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        result.append("{");
        return addLicenseData(license, result, true).toString();
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
        UpdateRequestBuilder updateRequestBuilder = enhanceRequest(
                new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LICENSE_DEFAULT),
                etmConfiguration
        )
                .setDoc(licenseObject)
                .setDocAsUpsert(true);
        dataRepository.update(updateRequestBuilder);

        // Because the access to the etmConfiguration in the above statement could cause a reload of the configuration
        // the old license may still be applied. To prevent this, we set the license again at this place.
        etmConfiguration.setLicenseKey(licenseKey);
        License license = etmConfiguration.getLicense();
        StringBuilder result = new StringBuilder();
        result.append("{");
        boolean added = addStringElementToJsonBuffer("status", "success", result, true);
        return addLicenseData(license, result, !added).toString();
    }

    /**
     * Add the common license data as json
     *
     * @param license      The <code>License</code> to add the data from.
     * @param buffer       The buffer to add the data to.
     * @param firstElement Is this the first json element?
     * @return The buffer with the license data added.
     */
    private StringBuilder addLicenseData(License license, StringBuilder buffer, boolean firstElement) {
        EtmPrincipal etmPrincipal = getEtmPrincipal();
        boolean added = !firstElement;
        added = addStringElementToJsonBuffer("owner", license.getOwner(), buffer, !added) || added;
        added = addLongElementToJsonBuffer("expiration_date", license.getExpiryDate().toEpochMilli(), buffer, !added) || added;
        added = addStringElementToJsonBuffer("time_zone", etmPrincipal.getTimeZone().getID(), buffer, !added) || added;
        added = addLongElementToJsonBuffer("max_events_per_day", license.getMaxEventsPerDay(), buffer, !added) || added;
        added = addStringElementToJsonBuffer("max_events_per_day_as_text", etmPrincipal.getNumberFormat().format(license.getMaxEventsPerDay()), buffer, !added) || added;
        added = addLongElementToJsonBuffer("max_size_per_day", license.getMaxSizePerDay(), buffer, !added) || added;
        added = addStringElementToJsonBuffer("max_size_per_day_as_text", etmPrincipal.getNumberFormat().format(license.getMaxSizePerDay()), buffer, !added) || added;
        buffer.append("}");
        return buffer;
    }

    @GET
    @Path("/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.CLUSTER_SETTINGS_READ, SecurityRoles.CLUSTER_SETTINGS_READ_WRITE})
    public String getClusterConfiguration() {
        GetResponse getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_NODE_DEFAULT)
                .setFetchSource(true)
        );
        EtmConfiguration config = this.etmConfigurationConverter.read(null, getResponse.getSourceAsString(), null);
        return toStringWithoutNamespace(this.etmConfigurationConverter.write(null, config), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE);
    }

    @SuppressWarnings("unchecked")
    @PUT
    @Path("/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.CLUSTER_SETTINGS_READ_WRITE)
    public String setClusterConfiguration(String json) {
        GetResponse getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_NODE_DEFAULT)
                .setFetchSource(true)
        );
        Map<String, Object> currentNodeObject = getResponse.getSourceAsMap();
        Map<String, Object> currentValues = (Map<String, Object>) currentNodeObject.get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE);
        // Overwrite the values with the new values.
        currentValues.putAll(toMap(json));
        EtmConfiguration defaultConfig = this.etmConfigurationConverter.read(null, currentNodeObject, null);

        UpdateRequestBuilder builder = enhanceRequest(
                new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_NODE_DEFAULT),
                etmConfiguration
        )
                .setDoc(this.etmConfigurationConverter.write(null, defaultConfig), XContentType.JSON)
                .setDocAsUpsert(true)
                .setDetectNoop(true);
        dataRepository.update(builder);
        return "{\"status\":\"success\"}";
    }

    @GET
    @Path("/ldap")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.CLUSTER_SETTINGS_READ, SecurityRoles.CLUSTER_SETTINGS_READ_WRITE})
    public String getLdapConfiguration() {
        GetResponse getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LDAP_DEFAULT)
                .setFetchSource(true)
        );
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
        IndexRequestBuilder indexRequestBuilder = enhanceRequest(
                new IndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LDAP_DEFAULT),
                etmConfiguration
        ).setSource(this.ldapConfigurationConverter.write(config), XContentType.JSON);
        dataRepository.index(indexRequestBuilder);
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
        DeleteRequestBuilder builder = enhanceRequest(
                new DeleteRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LDAP_DEFAULT),
                etmConfiguration
        );
        dataRepository.delete(builder);
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
        SearchRequestBuilder searchRequestBuilder = enhanceRequest(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME), etmConfiguration)
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE));
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder);
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
        GetResponse defaultSettingsResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_NODE_DEFAULT)
                .setFetchSource(true)
        );
        EtmConfiguration defaultConfig = this.etmConfigurationConverter.read(null, defaultSettingsResponse.getSourceAsString(), null);
        EtmConfiguration nodeConfig = this.etmConfigurationConverter.read(toStringWithNamespace(json, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE), defaultSettingsResponse.getSourceAsString(), nodeName);
        IndexRequestBuilder builder = enhanceRequest(
                new IndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE_ID_PREFIX + nodeName),
                etmConfiguration
        ).setSource(this.etmConfigurationConverter.write(nodeConfig, defaultConfig), XContentType.JSON);
        dataRepository.index(builder);
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
        DeleteRequestBuilder builder = enhanceRequest(
                new DeleteRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE_ID_PREFIX + nodeName),
                etmConfiguration
        );
        dataRepository.delete(builder);
        return "{\"status\":\"success\"}";
    }


    @GET
    @Path("/indicesstats")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.INDEX_STATISTICS_READ)
    public String getIndexStatistics() {
        IndicesStatsResponse indicesStatsResponse = dataRepository.indicesGetStats(new IndicesStatsRequestBuilder().setIndices("etm_event_*")
                .clear()
                .setStore(true)
                .setDocs(true)
                .setIndexing(true)
                .setSearch(true)
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout())));
        NumberFormat numberFormat = getEtmPrincipal().getNumberFormat();
        StringBuilder result = new StringBuilder();
        result.append("{");
        result.append("\"locale\": ").append(getLocalFormatting(getEtmPrincipal()));
        result.append(",\"totals\": {");
        final long totalCount = indicesStatsResponse.getPrimaries().getDocs().getCount() - indicesStatsResponse.getPrimaries().getDocs().getDeleted();
        addLongElementToJsonBuffer("document_count", totalCount, result, true);
        addStringElementToJsonBuffer("document_count_as_string", numberFormat.format(totalCount), result, false);
        addLongElementToJsonBuffer("size_in_bytes", indicesStatsResponse.getTotal().getStore().getSizeInBytes(), result, false);
        addStringElementToJsonBuffer("size_in_bytes_as_string", numberFormat.format(indicesStatsResponse.getTotal().getStore().getSizeInBytes()), result, false);
        result.append("}, \"indices\": [");
        Map<String, IndexStats> indices = indicesStatsResponse.getIndices();
        boolean firstEntry = true;
        for (Entry<String, IndexStats> entry : indices.entrySet()) {
            if (!firstEntry) {
                result.append(",");
            }
            result.append("{");
            final long count = entry.getValue().getPrimaries().getDocs().getCount() - entry.getValue().getPrimaries().getDocs().getDeleted();
            addStringElementToJsonBuffer("name", entry.getKey(), result, true);
            addLongElementToJsonBuffer("document_count", count, result, false);
            addStringElementToJsonBuffer("document_count_as_string", numberFormat.format(count), result, false);
            addLongElementToJsonBuffer("size_in_bytes", entry.getValue().getTotal().getStore().getSizeInBytes(), result, false);
            addStringElementToJsonBuffer("size_in_bytes_as_string", numberFormat.format(entry.getValue().getTotal().getStore().getSizeInBytes()), result, false);

            long indexCount = entry.getValue().getTotal().getIndexing().getIndexCount();
            if (indexCount != 0) {
                long averageIndexTime = entry.getValue().getTotal().getIndexing().getIndexTime().millis() / indexCount;
                addLongElementToJsonBuffer("average_index_time", averageIndexTime, result, false);
                addStringElementToJsonBuffer("average_index_time_as_string", numberFormat.format(averageIndexTime), result, false);
            }

            indexCount = entry.getValue().getTotal().getSearch().getQueryCount();
            if (indexCount != 0) {
                long averageSearchTime = entry.getValue().getTotal().getSearch().getQueryTimeInMillis() / indexCount;
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
    @RolesAllowed({SecurityRoles.PARSER_SETTINGS_READ, SecurityRoles.PARSER_SETTINGS_READ_WRITE, SecurityRoles.IMPORT_PROFILES_READ, SecurityRoles.IMPORT_PROFILES_READ_WRITE})
    public String getParsers() {
        SearchRequestBuilder searchRequestBuilder = enhanceRequest(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME), etmConfiguration)
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER));
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder);
        if (!scrollableSearch.hasNext()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        result.append("{\"parsers\": [");
        boolean first = true;
        for (SearchHit searchHit : scrollableSearch) {
            ExpressionParser expressionParser = expressionParserConverter.read(searchHit.getSourceAsString());
            Map<String, Object> parserMap = toMapWithoutNamespace(searchHit.getSourceAsMap(), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER);
            parserMap.put("capable_of_replacing", expressionParser.isCapableOfReplacing());
            if (!first) {
                result.append(",");
            }
            result.append(toString(parserMap));
            first = false;
        }
        result.append("]}");
        return result.toString();
    }

    @GET
    @Path("/parserfields")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.PARSER_SETTINGS_READ, SecurityRoles.PARSER_SETTINGS_READ_WRITE, SecurityRoles.IMPORT_PROFILES_READ, SecurityRoles.IMPORT_PROFILES_READ_WRITE})
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
        BulkRequestBuilder bulkRequestBuilder = enhanceRequest(new BulkRequestBuilder(), etmConfiguration);
        bulkRequestBuilder.add(
                enhanceRequest(
                        new DeleteRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER_ID_PREFIX + parserName),
                        etmConfiguration
                ).build()
        );
        removeParserFromImportProfiles(bulkRequestBuilder, parserName);
        dataRepository.bulk(bulkRequestBuilder);
        return "{\"status\":\"success\"}";
    }

    private void removeParserFromImportProfiles(BulkRequestBuilder bulkRequestBuilder, String parserName) {
        SearchRequestBuilder searchRequestBuilder = enhanceRequest(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME), etmConfiguration)
                .setFetchSource(true)
                .setQuery(QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE))
                        .should(QueryBuilders.termQuery(parserInImportProfileFieldsTag, parserName))
                        .should(QueryBuilders.termQuery(parserInImportProfileTransformationTag, parserName))
                        .minimumShouldMatch(1)
                );
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder);
        for (SearchHit searchHit : scrollableSearch) {
            boolean updated = false;
            ImportProfile endpointConfig = this.importProfileConverter.read(searchHit.getSourceAsString());
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
                Iterator<DefaultTransformation> transformationIt = enhancer.getTransformations().iterator();
                while (transformationIt.hasNext()) {
                    DefaultTransformation transformation = transformationIt.next();
                    if (transformation.getExpressionParser().getName().equals(parserName)) {
                        transformationIt.remove();
                        updated = true;
                    }
                }
            }
            if (updated) {
                bulkRequestBuilder.add(createImportProfileUpdateRequest(endpointConfig).build());
            }
        }
    }

    @PUT
    @Path("/parser/{parserName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.PARSER_SETTINGS_READ_WRITE)
    public String addParser(@PathParam("parserName") String parserName, String json) {
        BulkRequestBuilder bulkRequestBuilder = enhanceRequest(new BulkRequestBuilder(), etmConfiguration);
        // Do a read and write of the parser to make sure it's valid.
        ExpressionParser expressionParser = this.expressionParserConverter.read(toStringWithNamespace(json, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER));

        bulkRequestBuilder.add(
                enhanceRequest(
                        new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER_ID_PREFIX + parserName),
                        etmConfiguration
                )
                .setDoc(this.expressionParserConverter.write(expressionParser), XContentType.JSON)
                .setDocAsUpsert(true)
                .setDetectNoop(true)
                        .build()
        );
        updateParserInImportProfiles(bulkRequestBuilder, expressionParser);
        dataRepository.bulk(bulkRequestBuilder);
        return "{ \"status\": \"success\" }";
    }

    private void updateParserInImportProfiles(BulkRequestBuilder bulkRequestBuilder, ExpressionParser expressionParser) {
        SearchRequestBuilder searchRequestBuilder = enhanceRequest(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME), etmConfiguration)
                .setFetchSource(true)
                .setQuery(QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE))
                        .must(QueryBuilders.termQuery(parserInImportProfileFieldsTag, expressionParser.getName()))
                        .should(QueryBuilders.termQuery(parserInImportProfileTransformationTag, expressionParser.getName()))
                        .minimumShouldMatch(1)
                );
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder);
        for (SearchHit searchHit : scrollableSearch) {
            boolean updated = false;
            ImportProfile endpointConfig = this.importProfileConverter.read(searchHit.getSourceAsString());
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
                for (DefaultTransformation transformation : enhancer.getTransformations()) {
                    if (transformation.getExpressionParser().getName().equals(expressionParser.getName())) {
                        transformation.setExpressionParser(expressionParser);
                        updated = true;
                    }
                }
            }
            if (updated) {
                bulkRequestBuilder.add(createImportProfileUpdateRequest(endpointConfig).build());
            }
        }
    }

    @GET
    @Path("/import_profiles")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.IMPORT_PROFILES_READ, SecurityRoles.IMPORT_PROFILES_READ_WRITE})
    public String getImportProfiles() {
        SearchRequestBuilder searchRequestBuilder = enhanceRequest(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME), etmConfiguration)
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE));
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder);
        StringBuilder result = new StringBuilder();
        result.append("{\"import_profiles\": [");
        boolean first = true;
        boolean defaultFound = false;
        for (SearchHit searchHit : scrollableSearch) {
            if (!first) {
                result.append(",");
            }
            result.append(toStringWithoutNamespace(searchHit.getSourceAsMap(), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE));
            if (ElasticsearchLayout.CONFIGURATION_OBJECT_ID_IMPORT_PROFILE_DEFAULT.equals(searchHit.getId())) {
                defaultFound = true;
            }
            first = false;
        }
        if (!defaultFound) {
            if (!first) {
                result.append(",");
            }
            ImportProfile defaultImportProfile = new ImportProfile();
            defaultImportProfile.name = "*";
            defaultImportProfile.eventEnhancer = new DefaultTelemetryEventEnhancer();
            result.append(toStringWithoutNamespace(this.importProfileConverter.write(defaultImportProfile), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE));
        }

        result.append("]}");
        return result.toString();
    }


    @DELETE
    @Path("/import_profile/{importProfileName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.IMPORT_PROFILES_READ_WRITE)
    public String deleteImportProfile(@PathParam("importProfileName") String importProfileName) {
        DeleteRequestBuilder builder = enhanceRequest(
                new DeleteRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE_ID_PREFIX + importProfileName),
                etmConfiguration
        );
        dataRepository.delete(builder);
        return "{\"status\":\"success\"}";
    }

    @PUT
    @Path("/import_profile/{importProfileName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.IMPORT_PROFILES_READ_WRITE)
    public String addImportProfile(@PathParam("importProfileName") String importProfileName, String json) {
        // Do a read and write of the import profile to make sure it's valid.
        ImportProfile importProfile = this.importProfileConverter.read(toStringWithNamespace(json, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE));
        dataRepository.update(createImportProfileUpdateRequest(importProfile));
        return "{ \"status\": \"success\" }";
    }

    private UpdateRequestBuilder createImportProfileUpdateRequest(ImportProfile importProfile) {
        return enhanceRequest(new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE_ID_PREFIX + importProfile.name),
                etmConfiguration
        )
                .setDoc(this.importProfileConverter.write(importProfile), XContentType.JSON)
                .setDocAsUpsert(true)
                .setDetectNoop(true);
    }

    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.USER_SETTINGS_READ, SecurityRoles.USER_SETTINGS_READ_WRITE})
    public String getUsers() {
        SearchRequestBuilder searchRequestBuilder = enhanceRequest(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME), etmConfiguration)
                .setFetchSource(new String[]{"*"}, new String[]{this.principalTags.getPasswordHashTag(), this.principalTags.getSearchTemplatesTag(), this.principalTags.getSearchHistoryTag()})
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER));
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder);
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
        SearchRequestBuilder searchRequestBuilder = enhanceRequest(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME), etmConfiguration)
                .setFetchSource(this.exportPrincipalFields.stream().map(FieldLayout::getField).toArray(String[]::new), null)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER));
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder);
        if (!scrollableSearch.hasNext()) {
            return null;
        }
        FileType fileType = FileType.valueOf(getString("fileType", valueMap).toUpperCase());
        File result = new QueryExporter().exportToFile(
                scrollableSearch,
                fileType,
                Integer.MAX_VALUE,
                etmPrincipal,
                this.exportPrincipalFields,
                null
        );
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
        IndexRequestBuilder builder = enhanceRequest(
                new IndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + principal.getId()),
                etmConfiguration
        )
                .setSource(this.etmPrincipalConverter.writePrincipal(principal), XContentType.JSON);
        dataRepository.index(builder);
        return toStringWithoutNamespace(this.etmPrincipalConverter.writePrincipal(principal), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER);
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
        DeleteRequestBuilder builder = enhanceRequest(
                new DeleteRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId),
                etmConfiguration
        );
        dataRepository.delete(builder);
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
        UpdateRequestBuilder builder = enhanceRequest(
                new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId),
                etmConfiguration
        ).setDoc(this.etmPrincipalConverter.writePrincipal(newPrincipal), XContentType.JSON)
                .setDocAsUpsert(true)
                .setDetectNoop(true);
        dataRepository.update(builder);
        if (currentPrincipal == null && etmConfiguration.getMaxSearchTemplateCount() >= 3) {
            // Add some default templates to the user if he/she is able to search.
            builder = enhanceRequest(
                    new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId),
                    etmConfiguration
            ).setDoc(new DefaultSearchTemplates().toJson(), XContentType.JSON);
            dataRepository.update(builder);
        }
        if (userId.equals(getEtmPrincipal().getId())) {
            getEtmPrincipal().forceReload = true;
        }
        return "{ \"status\": \"success\" }";
    }

    @GET
    @Path("/user/api_key")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.USER_SETTINGS_READ_WRITE})
    public String createNewApiKey() {
        return "{" + this.escapeObjectToJsonNameValuePair("api_key", UUID.randomUUID().toString()) + "}";
    }

    @GET
    @Path("/groups")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.GROUP_SETTINGS_READ, SecurityRoles.GROUP_SETTINGS_READ_WRITE, SecurityRoles.USER_SETTINGS_READ, SecurityRoles.USER_SETTINGS_READ_WRITE})
    public String getGroups() {
        SearchRequestBuilder searchRequestBuilder = enhanceRequest(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME), etmConfiguration)
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP));
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder);
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
            result.append(toStringWithoutNamespace(this.etmPrincipalConverter.writeGroup(etmGroup), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP));
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
            GetResponse getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + group.getName()));
            if (getResponse.isExists()) {
                Map<String, Object> sourceAsMap = toMapWithoutNamespace(getResponse.getSourceAsMap(), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP);
                if (getBoolean(this.principalTags.getLdapBaseTag(), sourceAsMap, Boolean.FALSE)) {
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
        IndexRequestBuilder builder = enhanceRequest(
                new IndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName),
                etmConfiguration
        )
                .setSource(this.etmPrincipalConverter.writeGroup(group), XContentType.JSON);
        dataRepository.index(builder);
        return toStringWithoutNamespace(this.etmPrincipalConverter.writeGroup(group), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP);
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
        UpdateRequestBuilder builder = enhanceRequest(
                new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName),
                etmConfiguration
        )
                .setDoc(this.etmPrincipalConverter.writeGroup(newGroup), XContentType.JSON)
                .setDocAsUpsert(true)
                .setDetectNoop(true);
        dataRepository.update(builder);
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
        BulkRequestBuilder bulkRequestBuilder = enhanceRequest(new BulkRequestBuilder(), etmConfiguration);
        bulkRequestBuilder.add(
                enhanceRequest(
                        new DeleteRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName),
                        etmConfiguration
                ).build()
        );
        removeGroupFromPrincipal(bulkRequestBuilder, groupName);
        dataRepository.bulk(bulkRequestBuilder);
        // Force a reload of the principal if he/she is in the deleted group.
        EtmPrincipal principal = getEtmPrincipal();
        if (principal.isInGroup(groupName)) {
            principal.forceReload = true;
        }
        return "{\"status\":\"success\"}";
    }

    @GET
    @Path("/notifiers")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({
            SecurityRoles.NOTIFIERS_READ,
            SecurityRoles.NOTIFIERS_READ_WRITE})
    public String getNotifiers() {
        SearchRequestBuilder searchRequestBuilder = enhanceRequest(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME), etmConfiguration)
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER));
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder);
        StringBuilder result = new StringBuilder();
        result.append("{\"notifiers\": [");
        boolean first = true;
        for (SearchHit searchHit : scrollableSearch) {
            if (!first) {
                result.append(",");
            }
            result.append(toStringWithoutNamespace(searchHit.getSourceAsMap(), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER));
            first = false;
        }
        result.append("]}");
        return result.toString();
    }

    @GET
    @Path("/notifiers/basics")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({
            SecurityRoles.USER_SETTINGS_READ,
            SecurityRoles.USER_SETTINGS_READ_WRITE,
            SecurityRoles.GROUP_SETTINGS_READ,
            SecurityRoles.GROUP_SETTINGS_READ_WRITE})
    public String getNotifiersBasics() {
        SearchRequestBuilder searchRequestBuilder = enhanceRequest(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME), etmConfiguration)
                .setFetchSource(new String[]{
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER + "." + Notifier.NAME,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER + "." + Notifier.NOTIFIER_TYPE
                }, null)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER));
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder);
        StringBuilder result = new StringBuilder();
        result.append("{\"notifiers\": [");
        boolean first = true;
        for (SearchHit searchHit : scrollableSearch) {
            if (!first) {
                result.append(",");
            }
            result.append(toStringWithoutNamespace(searchHit.getSourceAsMap(), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER));
            first = false;
        }
        result.append("]}");
        return result.toString();
    }

    @PUT
    @Path("/notifier/{notifierName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.NOTIFIERS_READ_WRITE)
    public String addNotifier(@PathParam("notifierName") String notifierName, String json) {
        // Do a read and write of the notifier to make sure it's valid.
        Map<String, Object> objectMap = toMapWithNamespace(json, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER);
        Notifier notifier = this.notifierConverter.read(objectMap);

        IndexRequestBuilder builder = enhanceRequest(
                new IndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER_ID_PREFIX + notifier.getName()),
                etmConfiguration
        )
                .setSource(this.notifierConverter.write(notifier), XContentType.JSON);
        dataRepository.index(builder);
        return "{ \"status\": \"success\" }";
    }

    @DELETE
    @Path("/notifier/{notifierName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.NOTIFIERS_READ_WRITE)
    public String deleteNotifier(@PathParam("notifierName") String notifierName) {
        BulkRequestBuilder bulkRequestBuilder = enhanceRequest(new BulkRequestBuilder(), etmConfiguration);
        bulkRequestBuilder.add(
                enhanceRequest(
                        new DeleteRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER_ID_PREFIX + notifierName),
                        etmConfiguration
                ).build()
        );
        removeNotifierFromSignalsAndPrincipals(bulkRequestBuilder, notifierName);
        dataRepository.bulk(bulkRequestBuilder);
        return "{\"status\":\"success\"}";
    }

    private void removeNotifierFromSignalsAndPrincipals(BulkRequestBuilder bulkRequestBuilder, String notifierName) {
        SearchRequestBuilder searchRequestBuilder = enhanceRequest(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME), etmConfiguration)
                .setFetchSource(true)
                .setQuery(QueryBuilders.boolQuery()
                        .should(QueryBuilders.termQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.principalTags.getNotifiersTag(), notifierName))
                        .should(QueryBuilders.termQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.principalTags.getSignalsTag() + "." + this.principalTags.getNotifiersTag(), notifierName))
                        .should(QueryBuilders.termQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP + "." + this.principalTags.getSignalsTag() + "." + this.principalTags.getNotifiersTag(), notifierName))
                        .should(QueryBuilders.termQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP + "." + this.principalTags.getNotifiersTag(), notifierName))
                        .minimumShouldMatch(1)
                );
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder);
        for (SearchHit searchHit : scrollableSearch) {
            boolean updated = false;
            Map<String, Object> valueMap = searchHit.getSourceAsMap();
            Map<String, Object> groupOrUserValueMap;
            if (valueMap.containsKey(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER)) {
                groupOrUserValueMap = getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, valueMap);
            } else {
                groupOrUserValueMap = getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP, valueMap);
            }
            if (groupOrUserValueMap == null) {
                continue;
            }
            // Remote the notifier from the signals
            List<Map<String, Object>> signals = getArray(this.principalTags.getSignalsTag(), groupOrUserValueMap, Collections.emptyList());
            for (Map<String, Object> signal : signals) {
                List<String> notifiers = getArray(this.principalTags.getNotifiersTag(), signal);
                if (notifiers == null) {
                    continue;
                }
                Iterator<String> it = notifiers.iterator();
                while (it.hasNext()) {
                    if (notifierName.equals(it.next())) {
                        it.remove();
                        updated = true;
                    }
                }
            }
            // Remove the notifier from the user or group
            List<String> notifiers = getArray(this.principalTags.getNotifiersTag(), groupOrUserValueMap, Collections.emptyList());
            Iterator<String> it = notifiers.iterator();
            while (it.hasNext()) {
                if (notifierName.equals(it.next())) {
                    it.remove();
                    updated = true;
                }
            }
            if (updated) {
                bulkRequestBuilder.add(
                        enhanceRequest(new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, searchHit.getId()), etmConfiguration)
                                .setDoc(valueMap)
                                .setDocAsUpsert(true)
                                .setDetectNoop(true)
                                .build()
                );
            }
        }
    }


    private void removeGroupFromPrincipal(BulkRequestBuilder bulkRequestBuilder, String groupName) {
        SearchRequestBuilder searchRequestBuilder = enhanceRequest(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME), etmConfiguration)
                .setFetchSource(false)
                .setQuery(QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP))
                        .must(QueryBuilders.termQuery(this.principalTags.getGroupsTag(), groupName))
                );
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder);
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
                bulkRequestBuilder.add(createPrincipalUpdateRequest(principal).build());
            }
        }
    }

    private UpdateRequestBuilder createPrincipalUpdateRequest(EtmPrincipal principal) {
        return enhanceRequest(
                new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + principal.getId()),
                etmConfiguration
        )
                .setDoc(this.etmPrincipalConverter.writePrincipal(principal), XContentType.JSON)
                .setDocAsUpsert(true)
                .setDetectNoop(true);
    }

    /**
     * Load an <code>EtmPrincipal</code> based on the user id.
     *
     * @param userId The id of the user to load.
     * @return A fully loaded <code>EtmPrincipal</code>, or <code>null</code> if no user with the given userId exists.
     */
    private EtmPrincipal loadPrincipal(String userId) {
        GetResponse getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId));
        if (!getResponse.isExists()) {
            return null;
        }
        return loadPrincipal(getResponse.getSourceAsMap());
    }

    /**
     * Converts a map with json key/value pairs to an <code>EtmPrincipal</code>
     * and reads the corresponding groups from the database.
     *
     * @param principalObject The namespaced <code>Map</code> with json key/value pairs.
     * @return A fully loaded <code>EtmPrincipal</code>
     */
    private EtmPrincipal loadPrincipal(Map<String, Object> principalObject) {
        EtmPrincipal principal = this.etmPrincipalConverter.readPrincipal(principalObject);
        Map<String, Object> principalValues = toMapWithoutNamespace(principalObject, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER);
        Collection<String> groups = getArray(this.principalTags.getGroupsTag(), principalValues);
        if (groups != null && !groups.isEmpty()) {
            MultiGetRequestBuilder multiGetBuilder = new MultiGetRequestBuilder();
            for (String group : groups) {
                multiGetBuilder.add(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + group);
            }
            MultiGetResponse multiGetResponse = dataRepository.get(multiGetBuilder);
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
        GetResponse getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName));
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
        SearchRequestBuilder searchRequestBuilder = enhanceRequest(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME), etmConfiguration)
                .setFetchSource(true)
                .setQuery(QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP))
                        .must(QueryBuilders.termsQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP + "." + this.principalTags.getRolesTag(), roles))
                );
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder);
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
            query.must(QueryBuilders.termQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.principalTags.getRolesTag(), SecurityRoles.USER_SETTINGS_READ_WRITE));
        } else {
            query.should(QueryBuilders.termsQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.principalTags.getRolesTag(), SecurityRoles.USER_SETTINGS_READ_WRITE))
                    .should(QueryBuilders.termsQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.principalTags.getGroupsTag(), adminGroups))
                    .minimumShouldMatch(1);
        }
        SearchRequestBuilder builder = enhanceRequest(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME), etmConfiguration)
                .setFetchSource(false)
                .setSize(0)
                .trackTotalHits(true)
                .setQuery(query);
        SearchResponse response = dataRepository.search(builder);
        return response.getHits().getTotalHits().value;
    }
}