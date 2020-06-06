/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.gui.rest.services.settings;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.gui.rest.export.*;
import com.jecstar.etm.gui.rest.services.AbstractIndexMetadataService;
import com.jecstar.etm.gui.rest.services.Keyword;
import com.jecstar.etm.gui.rest.services.search.DefaultUserSettings;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.ImportProfile;
import com.jecstar.etm.server.core.domain.audit.ConfigurationChangedAuditLog;
import com.jecstar.etm.server.core.domain.audit.builder.ConfigurationChangedAuditLogBuilder;
import com.jecstar.etm.server.core.domain.audit.converter.ConfigurationChangedAuditLogConverter;
import com.jecstar.etm.server.core.domain.cluster.certificate.Usage;
import com.jecstar.etm.server.core.domain.cluster.certificate.converter.CertificateConverter;
import com.jecstar.etm.server.core.domain.cluster.notifier.ConnectionTestResult;
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
import com.jecstar.etm.server.core.enhancers.DefaultField;
import com.jecstar.etm.server.core.enhancers.DefaultTelemetryEventEnhancer;
import com.jecstar.etm.server.core.enhancers.DefaultTransformation;
import com.jecstar.etm.server.core.ldap.Directory;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.persisting.RequestEnhancer;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import com.jecstar.etm.server.core.tls.TrustAllTrustManager;
import com.jecstar.etm.server.core.util.BCrypt;
import org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.net.ssl.*;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Path("/settings")
@DeclareRoles(SecurityRoles.ALL_ROLES)
public class SettingsService extends AbstractIndexMetadataService {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private final LogWrapper log = LogFactory.getLogger(SettingsService.class);

    private final EtmConfigurationConverterJsonImpl etmConfigurationConverter = new EtmConfigurationConverterJsonImpl();
    private final LdapConfigurationConverter ldapConfigurationConverter = new LdapConfigurationConverter();
    private final NotifierConverter notifierConverter = new NotifierConverter();
    private final ExpressionParserConverter<String> expressionParserConverter = new ExpressionParserConverterJsonImpl();
    private final ImportProfileConverter<String> importProfileConverter = new ImportProfileConverterJsonImpl();
    private final EtmPrincipalConverterJsonImpl etmPrincipalConverter = new EtmPrincipalConverterJsonImpl();
    private final EtmPrincipalTags principalTags = this.etmPrincipalConverter.getTags();
    private final CertificateConverter certificateConverter = new CertificateConverter();
    private final ConfigurationChangedAuditLogConverter configurationChangedAuditLogConverter = new ConfigurationChangedAuditLogConverter();

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
    private static RequestEnhancer requestEnhancer;

    public static void initialize(DataRepository dataRepository, EtmConfiguration etmConfiguration) {
        SettingsService.dataRepository = dataRepository;
        SettingsService.etmConfiguration = etmConfiguration;
        SettingsService.requestEnhancer = new RequestEnhancer(etmConfiguration);
    }

    @GET
    @Path("/keywords/{indexName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.USER_SETTINGS_READ_WRITE, SecurityRoles.USER_SETTINGS_READ, SecurityRoles.GROUP_SETTINGS_READ_WRITE, SecurityRoles.GROUP_SETTINGS_READ})
    public String getKeywords(@PathParam("indexName") String indexName) {
        var builder = new JsonBuilder();
        List<Keyword> keywords = getIndexFields(SettingsService.dataRepository, indexName);
        builder.startObject();
        builder.startArray("keywords");
        builder.startObject();
        builder.field("index", indexName);
        builder.startArray("keywords");
        for (var keyword : keywords) {
            builder.startObject();
            builder.field("name", keyword.getName());
            builder.field("type", keyword.getType());
            builder.field("date", keyword.isDate());
            builder.field("number", keyword.isNumber());
            builder.endObject();
        }
        builder.endArray();
        builder.endObject();
        builder.endArray();
        builder.endObject();
        return builder.build();
    }

    @GET
    @Path("/license")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.LICENSE_READ, SecurityRoles.LICENSE_READ_WRITE})
    public String getLicense() {
        var etmPrincipal = getEtmPrincipal();
        var license = etmConfiguration.getLicense();
        if (license == null) {
            return null;
        }
        var builder = new JsonBuilder();
        builder.startObject();
        builder.field("max_database_size_in_bytes", license.getMaxDatabaseSize());
        builder.field("max_database_size_in_bytes_as_text", etmPrincipal.getNumberFormat().format(license.getMaxDatabaseSize()));
        addLicenseData(license, builder, etmPrincipal);
        builder.endObject();
        return builder.build();
    }

    @PUT
    @Path("/license")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.LICENSE_READ_WRITE)
    public String setLicense(String json) {
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        var requestValues = toMap(json);
        var licenseKey = getString("key", requestValues);
        var oldLicense = etmConfiguration.getLicense();
        etmConfiguration.setLicenseKey(licenseKey);
        var newLicense = etmConfiguration.getLicense();
        var licenseObject = new HashMap<String, Object>();
        var values = new HashMap<String, Object>();
        values.put(this.etmConfigurationConverter.getTags().getLicenseTag(), licenseKey);
        licenseObject.put(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LICENSE, values);
        UpdateRequestBuilder updateRequestBuilder = requestEnhancer.enhance(
                new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LICENSE_DEFAULT)
        )
                .setDoc(licenseObject)
                .setDocAsUpsert(true);
        dataRepository.update(updateRequestBuilder);
        // Create audit data.
        if (!Objects.equals(oldLicense, newLicense)) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(ConfigurationChangedAuditLog.Action.UPDATE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LICENSE)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LICENSE_DEFAULT)
                    .setOldValue(oldLicense)
                    .setNewValue(newLicense)
            );
        }

        // Because the access to the etmConfiguration in the above statement could cause a reload of the configuration
        // the old license may still be applied. To prevent this, we set the license again at this place.
        etmConfiguration.setLicenseKey(licenseKey);
        var license = etmConfiguration.getLicense();
        var builder = new JsonBuilder();
        builder.startObject();
        builder.field("status", "success");
        builder.field("max_database_size_in_bytes", license.getMaxDatabaseSize());
        builder.field("max_database_size_in_bytes_as_text", etmPrincipal.getNumberFormat().format(license.getMaxDatabaseSize()));
        addLicenseData(license, builder, etmPrincipal);
        builder.endObject();
        return builder.build();
    }

    /**
     * Add the common license data as json
     *
     * @param license      The <code>License</code> to add the data from.
     * @param builder      The <code>JsonBuilder</code> to add the data to.
     * @param etmPrincipal The <code>EtmPrincipal</code>?
     */
    private void addLicenseData(License license, JsonBuilder builder, EtmPrincipal etmPrincipal) {
        builder.field(License.OWNER, license.getOwner());
        builder.field(License.START_DATE, license.getStartDate());
        builder.field(License.EXPIRY_DATE, license.getExpiryDate());
        builder.field("time_zone", etmPrincipal.getTimeZone().getID());
        builder.field(License.MAX_REQUEST_UNITS_PER_SECOND, license.getMaxRequestUnitsPerSecond());
        builder.field(License.MAX_REQUEST_UNITS_PER_SECOND + "_as_text", etmPrincipal.getNumberFormat().format(license.getMaxRequestUnitsPerSecond()));
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
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();

        GetResponse getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_NODE_DEFAULT)
                .setFetchSource(true)
        );
        Map<String, Object> currentNodeObject = getResponse.getSourceAsMap();
        var oldClusterConfiguration = this.etmConfigurationConverter.write(null, this.etmConfigurationConverter.read(null, currentNodeObject, null));
        Map<String, Object> currentValues = (Map<String, Object>) currentNodeObject.get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE);
        // Overwrite the values with the new values.
        currentValues.putAll(toMap(json));
        EtmConfiguration defaultConfig = this.etmConfigurationConverter.read(null, currentNodeObject, null);

        // Update the new cluster settings.
        ClusterGetSettingsResponse clusterGetSettingsResponse = dataRepository.clusterGetSettings(new ClusterGetSettingsRequestBuilder());
        final var prefix = "cluster.remote.";
        Map<String, Settings> currentRemoteClusters = clusterGetSettingsResponse.getPersistentSettings().getGroups(prefix);
        Settings.Builder settingsBuilder = Settings.builder();
        // First remove all current remote clusters.
        for (var entry : currentRemoteClusters.entrySet()) {
            var keys = entry.getValue().keySet();
            for (var key : keys) {
                settingsBuilder.putNull(prefix + entry.getKey() + "." + key);
            }
        }
        // and add the clusters we've received from the gui
        for (var remoteCluster : defaultConfig.getRemoteClusters()) {
            if (remoteCluster.isClusterWide()) {
                var clusterName = remoteCluster.getName().replace(" ", "_");
                settingsBuilder.put(prefix + clusterName + ".skip_unavailable", true);
                settingsBuilder.putList(prefix + clusterName + ".seeds", remoteCluster.getSeeds().stream().map(rc -> rc.getHost() + ":" + rc.getPort()).collect(Collectors.toList()));
            }
        }
        if (!settingsBuilder.keys().isEmpty()) {
            dataRepository.clusterUpdateSettings(new ClusterUpdateSettingsRequestBuilder().setPersistentSettings(settingsBuilder));
        }
        var newClusterConfiguration = this.etmConfigurationConverter.write(null, defaultConfig);
        UpdateRequestBuilder builder = requestEnhancer.enhance(
                new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_NODE_DEFAULT)
        )
                .setDoc(newClusterConfiguration, XContentType.JSON)
                .setDocAsUpsert(true)
                .setDetectNoop(true);
        dataRepository.update(builder);
        if (!Objects.equals(oldClusterConfiguration, newClusterConfiguration)) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(ConfigurationChangedAuditLog.Action.UPDATE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_ID_NODE_DEFAULT)
                    .setOldValue(oldClusterConfiguration)
                    .setNewValue(newClusterConfiguration)
            );
        }
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
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        var oldLdapConfig = getCurrentLdapConfiguration();

        LdapConfiguration config = this.ldapConfigurationConverter.read(toStringWithNamespace(json, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LDAP));
        testLdapConnection(config);
        var newLdapConfig = this.ldapConfigurationConverter.write(config);
        IndexRequestBuilder indexRequestBuilder = requestEnhancer.enhance(
                new IndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LDAP_DEFAULT)
        ).setSource(newLdapConfig, XContentType.JSON);
        dataRepository.index(indexRequestBuilder);
        if (etmConfiguration.getDirectory() != null) {
            etmConfiguration.getDirectory().merge(config);
        } else {
            Directory directory = new Directory(dataRepository, config);
            etmConfiguration.setDirectory(directory);
        }
        if (!Objects.equals(oldLdapConfig, newLdapConfig)) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(oldLdapConfig == null ? ConfigurationChangedAuditLog.Action.CREATE : ConfigurationChangedAuditLog.Action.UPDATE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LDAP)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LDAP_DEFAULT)
                    .setOldValue(oldLdapConfig)
                    .setNewValue(newLdapConfig)
            );
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
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        var oldLdapConfig = getCurrentLdapConfiguration();

        DeleteRequestBuilder builder = requestEnhancer.enhance(
                new DeleteRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LDAP_DEFAULT)
        );
        dataRepository.delete(builder);
        etmConfiguration.setDirectory(null);

        if (oldLdapConfig != null) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(ConfigurationChangedAuditLog.Action.DELETE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LDAP)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LDAP_DEFAULT)
                    .setOldValue(oldLdapConfig)
            );
        }
        return "{\"status\":\"success\"}";
    }

    /**
     * Returns the current LDAP configuration.
     *
     * @return The current LDAP configuration.
     */
    private String getCurrentLdapConfiguration() {
        var getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LDAP_DEFAULT)
                .setFetchSource(true)
        );
        if (!getResponse.isExists()) {
            return null;
        }
        var config = this.ldapConfigurationConverter.read(getResponse.getSourceAsString());
        return this.ldapConfigurationConverter.write(config);
    }

    /**
     * Test a configuration by setting up a connection.
     *
     * @param config The configuration to test.
     */
    private void testLdapConnection(LdapConfiguration config) {
        try (Directory directory = new Directory(dataRepository, config)) {
            directory.test();
        }
    }

    @GET
    @Path("/nodes")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.NODE_SETTINGS_READ, SecurityRoles.NODE_SETTINGS_READ_WRITE})
    public String getNodes() {
        SearchRequestBuilder searchRequestBuilder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME))
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE));
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
        if (!scrollableSearch.hasNext()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        result.append("{\"nodes\": [");
        boolean first = true;
        for (var searchHit : scrollableSearch) {
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
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        // Do a read and write of the node to make sure it's valid.
        GetResponse defaultSettingsResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_NODE_DEFAULT)
                .setFetchSource(true)
        );
        EtmConfiguration defaultConfig = this.etmConfigurationConverter.read(null, defaultSettingsResponse.getSourceAsString(), null);
        var oldNodeConfiguration = getCurrentNodeConfiguration(nodeName, defaultConfig);
        EtmConfiguration nodeConfig = this.etmConfigurationConverter.read(toStringWithNamespace(json, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE), defaultSettingsResponse.getSourceAsString(), nodeName);
        var newNodeConfiguration = this.etmConfigurationConverter.write(nodeConfig, defaultConfig);
        IndexRequestBuilder builder = requestEnhancer.enhance(
                new IndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE_ID_PREFIX + nodeName)
        ).setSource(this.etmConfigurationConverter.write(nodeConfig, defaultConfig), XContentType.JSON);
        dataRepository.index(builder);
        if (!Objects.equals(oldNodeConfiguration, newNodeConfiguration)) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(oldNodeConfiguration == null ? ConfigurationChangedAuditLog.Action.CREATE : ConfigurationChangedAuditLog.Action.UPDATE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE_ID_PREFIX + nodeName)
                    .setOldValue(oldNodeConfiguration)
                    .setNewValue(newNodeConfiguration)
            );
        }
        return "{ \"status\": \"success\" }";
    }

    @DELETE
    @Path("/node/{nodeName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.NODE_SETTINGS_READ_WRITE)
    public String deleteNode(@PathParam("nodeName") String nodeName) {
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        GetResponse defaultSettingsResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_NODE_DEFAULT)
                .setFetchSource(true)
        );
        EtmConfiguration defaultConfig = this.etmConfigurationConverter.read(null, defaultSettingsResponse.getSourceAsString(), null);
        var oldNodeConfiguration = getCurrentNodeConfiguration(nodeName, defaultConfig);

        if (ElasticsearchLayout.ETM_OBJECT_NAME_DEFAULT.equalsIgnoreCase(nodeName)) {
            return "{\"status\":\"failed\"}";
        }
        DeleteRequestBuilder builder = requestEnhancer.enhance(
                new DeleteRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE_ID_PREFIX + nodeName)
        );
        dataRepository.delete(builder);
        if (oldNodeConfiguration != null) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(ConfigurationChangedAuditLog.Action.DELETE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE_ID_PREFIX + nodeName)
                    .setOldValue(oldNodeConfiguration)
            );
        }
        return "{\"status\":\"success\"}";
    }

    /**
     * Returns the current node configuration.
     *
     * @return The current node configuration.
     */
    private String getCurrentNodeConfiguration(String nodeName, EtmConfiguration defaultConfiguration) {
        var getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE_ID_PREFIX + nodeName)
                .setFetchSource(true)
        );
        if (!getResponse.isExists()) {
            return null;
        }
        var nodeConfig = this.etmConfigurationConverter.read(getResponse.getSourceAsString(), this.etmConfigurationConverter.write(null, defaultConfiguration), nodeName);
        return this.etmConfigurationConverter.write(nodeConfig, defaultConfiguration);
    }

    @GET
    @Path("/indicesstats")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.INDEX_STATISTICS_READ)
    public String getIndexStatistics() {
        var allIndicesStatsResponse = dataRepository.indicesGetStats(new IndicesStatsRequestBuilder()
                .clear()
                .setStore(true)
                .setDocs(true)
                .setIndexing(true)
                .setSearch(true)
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout())));
        var eventIndicesStatsResponse = dataRepository.indicesGetStats(new IndicesStatsRequestBuilder().setIndices("etm_event_*")
                .clear()
                .setStore(true)
                .setDocs(true)
                .setIndexing(true)
                .setSearch(true)
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout())));
        var numberFormat = getEtmPrincipal().getNumberFormat();
        var builder = new JsonBuilder();
        builder.startObject();
        builder.rawField("locale", getLocalFormatting(getEtmPrincipal()));
        builder.startObject("totals");
        final long totalCount = eventIndicesStatsResponse.getPrimaries().getDocs().getCount() - eventIndicesStatsResponse.getPrimaries().getDocs().getDeleted();
        builder.field("document_count", totalCount);
        builder.field("document_count_as_string", numberFormat.format(totalCount));
        builder.field("size_in_bytes", allIndicesStatsResponse.getTotal().getStore().getSizeInBytes());
        builder.field("size_in_bytes_as_string", numberFormat.format(allIndicesStatsResponse.getTotal().getStore().getSizeInBytes()));
        builder.endObject();
        builder.startArray("indices");
        Map<String, IndexStats> indices = eventIndicesStatsResponse.getIndices();
        for (Entry<String, IndexStats> entry : indices.entrySet()) {
            builder.startObject();
            final long count = entry.getValue().getPrimaries().getDocs().getCount() - entry.getValue().getPrimaries().getDocs().getDeleted();
            builder.field("name", entry.getKey());
            builder.field("document_count", count);
            builder.field("document_count_as_string", numberFormat.format(count));
            builder.field("size_in_bytes", entry.getValue().getTotal().getStore().getSizeInBytes());
            builder.field("size_in_bytes_as_string", numberFormat.format(entry.getValue().getTotal().getStore().getSizeInBytes()));

            long indexCount = entry.getValue().getTotal().getIndexing().getIndexCount();
            if (indexCount != 0) {
                long averageIndexTime = entry.getValue().getTotal().getIndexing().getIndexTime().millis() / indexCount;
                builder.field("average_index_time", averageIndexTime);
                builder.field("average_index_time_as_string", numberFormat.format(averageIndexTime));
            }

            indexCount = entry.getValue().getTotal().getSearch().getQueryCount();
            if (indexCount != 0) {
                long averageSearchTime = entry.getValue().getTotal().getSearch().getQueryTimeInMillis() / indexCount;
                builder.field("average_search_time", averageSearchTime);
                builder.field("average_search_time_as_string", numberFormat.format(averageSearchTime));
            }
            builder.endObject();
        }
        builder.endArray().endObject();
        return builder.build();
    }

    @GET
    @Path("/parsers")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.PARSER_SETTINGS_READ, SecurityRoles.PARSER_SETTINGS_READ_WRITE, SecurityRoles.IMPORT_PROFILES_READ, SecurityRoles.IMPORT_PROFILES_READ_WRITE})
    public String getParsers() {
        var searchRequestBuilder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME))
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER));
        var scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
        if (!scrollableSearch.hasNext()) {
            return null;
        }
        var result = new StringBuilder();
        result.append("{\"parsers\": [");
        boolean first = true;
        for (var searchHit : scrollableSearch) {
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
        var builder = new JsonBuilder();
        builder.startObject();
        builder.startArray("parserfields");
        for (ExpressionParserField field : ExpressionParserField.values()) {
            builder.startObject();
            builder.field("name", field.getJsonTag());
            builder.endObject();
        }
        builder.endArray().endObject();
        return builder.build();
    }


    @DELETE
    @Path("/parser/{parserName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.PARSER_SETTINGS_READ_WRITE)
    public String deleteParser(@PathParam("parserName") String parserName) {
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        var oldParserConfiguration = getCurrentParserConfiguration(parserName);
        BulkRequestBuilder bulkRequestBuilder = requestEnhancer.enhance(new BulkRequestBuilder());
        bulkRequestBuilder.add(
                requestEnhancer.enhance(
                        new DeleteRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER_ID_PREFIX + parserName)
                ).build()
        );
        removeParserFromImportProfiles(bulkRequestBuilder, parserName);
        dataRepository.bulk(bulkRequestBuilder);
        if (oldParserConfiguration != null) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(ConfigurationChangedAuditLog.Action.DELETE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER_ID_PREFIX + parserName)
                    .setOldValue(oldParserConfiguration)
            );
        }
        return "{\"status\":\"success\"}";
    }

    private void removeParserFromImportProfiles(BulkRequestBuilder bulkRequestBuilder, String parserName) {
        SearchRequestBuilder searchRequestBuilder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME))
                .setFetchSource(true)
                .setQuery(QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE))
                        .should(QueryBuilders.termQuery(parserInImportProfileFieldsTag, parserName))
                        .should(QueryBuilders.termQuery(parserInImportProfileTransformationTag, parserName))
                        .minimumShouldMatch(1)
                );
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
        for (var searchHit : scrollableSearch) {
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
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        var oldParserConfiguration = getCurrentParserConfiguration(parserName);

        BulkRequestBuilder bulkRequestBuilder = requestEnhancer.enhance(new BulkRequestBuilder());
        // Do a read and write of the parser to make sure it's valid.
        ExpressionParser expressionParser = this.expressionParserConverter.read(toStringWithNamespace(json, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER));
        var newParserConfiguration = this.expressionParserConverter.write(expressionParser, true);

        bulkRequestBuilder.add(
                requestEnhancer.enhance(
                        new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER_ID_PREFIX + parserName)
                )
                        .setDoc(newParserConfiguration, XContentType.JSON)
                        .setDocAsUpsert(true)
                        .setDetectNoop(true)
                        .build()
        );
        updateParserInImportProfiles(bulkRequestBuilder, expressionParser);
        dataRepository.bulk(bulkRequestBuilder);
        if (!Objects.equals(oldParserConfiguration, newParserConfiguration)) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(oldParserConfiguration == null ? ConfigurationChangedAuditLog.Action.CREATE : ConfigurationChangedAuditLog.Action.UPDATE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER_ID_PREFIX + parserName)
                    .setOldValue(oldParserConfiguration)
                    .setNewValue(newParserConfiguration)
            );
        }
        return "{ \"status\": \"success\" }";
    }

    private void updateParserInImportProfiles(BulkRequestBuilder bulkRequestBuilder, ExpressionParser expressionParser) {
        SearchRequestBuilder searchRequestBuilder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME))
                .setFetchSource(true)
                .setQuery(QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE))
                        .must(QueryBuilders.termQuery(parserInImportProfileFieldsTag, expressionParser.getName()))
                        .should(QueryBuilders.termQuery(parserInImportProfileTransformationTag, expressionParser.getName()))
                        .minimumShouldMatch(1)
                );
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
        for (var searchHit : scrollableSearch) {
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

    /**
     * Returns the current parser configuration.
     *
     * @return The current parser configuration.
     */
    private String getCurrentParserConfiguration(String parserName) {
        var getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER_ID_PREFIX + parserName)
                .setFetchSource(true)
        );
        if (!getResponse.isExists()) {
            return null;
        }
        var expressionParser = this.expressionParserConverter.read(getResponse.getSourceAsString());
        return this.expressionParserConverter.write(expressionParser, true);
    }

    @GET
    @Path("/import_profiles")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.IMPORT_PROFILES_READ, SecurityRoles.IMPORT_PROFILES_READ_WRITE})
    public String getImportProfiles() {
        SearchRequestBuilder searchRequestBuilder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME))
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE));
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
        StringBuilder result = new StringBuilder();
        result.append("{\"import_profiles\": [");
        boolean first = true;
        boolean defaultFound = false;
        for (var searchHit : scrollableSearch) {
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
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        var oldImportProfileConfiguration = getCurrentImportProfileConfiguration(importProfileName);
        DeleteRequestBuilder builder = requestEnhancer.enhance(
                new DeleteRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE_ID_PREFIX + importProfileName)
        );
        dataRepository.delete(builder);
        if (oldImportProfileConfiguration != null) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(ConfigurationChangedAuditLog.Action.DELETE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE_ID_PREFIX + importProfileName)
                    .setOldValue(oldImportProfileConfiguration)
            );
        }
        return "{\"status\":\"success\"}";
    }

    @PUT
    @Path("/import_profile/{importProfileName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.IMPORT_PROFILES_READ_WRITE)
    public String addImportProfile(@PathParam("importProfileName") String importProfileName, String json) {
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        var oldImportProfileConfiguration = getCurrentImportProfileConfiguration(importProfileName);
        // Do a read and write of the import profile to make sure it's valid.
        ImportProfile importProfile = this.importProfileConverter.read(toStringWithNamespace(json, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE));
        var newImportProfileConfiguration = this.importProfileConverter.write(importProfile);
        dataRepository.update(createImportProfileUpdateRequest(importProfile));

        if (!Objects.equals(oldImportProfileConfiguration, newImportProfileConfiguration)) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(oldImportProfileConfiguration == null ? ConfigurationChangedAuditLog.Action.CREATE : ConfigurationChangedAuditLog.Action.UPDATE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE_ID_PREFIX + importProfileName)
                    .setOldValue(oldImportProfileConfiguration)
                    .setNewValue(newImportProfileConfiguration)
            );
        }
        return "{ \"status\": \"success\" }";
    }

    private UpdateRequestBuilder createImportProfileUpdateRequest(ImportProfile importProfile) {
        return requestEnhancer.enhance(new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE_ID_PREFIX + importProfile.name)
        )
                .setDoc(this.importProfileConverter.write(importProfile), XContentType.JSON)
                .setDocAsUpsert(true)
                .setDetectNoop(true);
    }

    /**
     * Returns the current import profile configuration.
     *
     * @return The current import profile configuration.
     */
    private String getCurrentImportProfileConfiguration(String importProfileName) {
        var getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE_ID_PREFIX + importProfileName)
                .setFetchSource(true)
        );
        if (!getResponse.isExists()) {
            return null;
        }
        var importProfile = this.importProfileConverter.read(getResponse.getSourceAsString());
        return this.importProfileConverter.write(importProfile);
    }

    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.USER_SETTINGS_READ, SecurityRoles.USER_SETTINGS_READ_WRITE})
    public String getUsers() {
        SearchRequestBuilder searchRequestBuilder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME))
                .setFetchSource(new String[]{"*"}, new String[]{this.principalTags.getPasswordHashTag(), this.principalTags.getSearchTemplatesTag(), this.principalTags.getSearchHistoryTag()})
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER));
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
        if (!scrollableSearch.hasNext()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        result.append("{\"users\": [");
        boolean first = true;
        for (var searchHit : scrollableSearch) {
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
        SearchRequestBuilder searchRequestBuilder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME))
                .setFetchSource(this.exportPrincipalFields.stream().map(FieldLayout::getField).toArray(String[]::new), null)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER));
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
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
        var query = getString("query", toMap(json));
        if (query == null || etmConfiguration.getDirectory() == null) {
            return null;
        }
        var principals = etmConfiguration.getDirectory().searchPrincipal(query.endsWith("*") ? query : query + "*");
        var builder = new JsonBuilder();
        builder.startObject();
        builder.startArray("users");
        for (EtmPrincipal principal : principals) {
            builder.startObject();
            builder.field("id", principal.getId());
            builder.field("label", principal.getName() != null ? principal.getId() + " - " + principal.getName() : principal.getId());
            builder.field("value", principal.getId());
            builder.endObject();
        }
        builder.endArray().endObject();
        return builder.build();
    }

    @PUT
    @Path("/users/ldap/import/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_SETTINGS_READ_WRITE)
    public String importLdapUser(@PathParam("userId") String userId) {
        if (etmConfiguration.getDirectory() == null) {
            return null;
        }
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        var oldUserConfiguration = getCurrentUserConfiguration(userId);

        var principalToImport = etmConfiguration.getDirectory().getPrincipal(userId, false);
        if (principalToImport == null) {
            throw new EtmException(EtmException.INVALID_LDAP_USER);
        }
        EtmPrincipal currentPrincipalToImport = loadPrincipal(principalToImport.getId());
        if (currentPrincipalToImport != null) {
            if (currentPrincipalToImport.isLdapBase()) {
                // LDAP user already present. No need to import the user again.
                return null;
            }
            // Merge the current and the LDAP principal.
            currentPrincipalToImport.setPasswordHash(null);
            currentPrincipalToImport.setName(principalToImport.getName());
            currentPrincipalToImport.setEmailAddress(principalToImport.getEmailAddress());
            currentPrincipalToImport.setChangePasswordOnLogon(false);
            principalToImport = currentPrincipalToImport;
        }
        principalToImport.setLdapBase(true);
        var newUserConfiguration = this.etmPrincipalConverter.writePrincipal(principalToImport);
        IndexRequestBuilder builder = requestEnhancer.enhance(
                new IndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + principalToImport.getId())
        )
                .setSource(newUserConfiguration, XContentType.JSON);
        dataRepository.index(builder);
        if (!Objects.equals(oldUserConfiguration, newUserConfiguration)) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(oldUserConfiguration == null ? ConfigurationChangedAuditLog.Action.CREATE : ConfigurationChangedAuditLog.Action.UPDATE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId)
                    .setOldValue(oldUserConfiguration)
                    .setNewValue(newUserConfiguration)
            );
        }
        return toStringWithoutNamespace(this.etmPrincipalConverter.writePrincipal(principalToImport), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER);
    }

    @DELETE
    @Path("/user/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_SETTINGS_READ_WRITE)
    public String deleteUser(@PathParam("userId") String userId) {
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        var oldUserConfiguration = getCurrentUserConfiguration(userId);

        var principalToDelete = loadPrincipal(userId);
        if (principalToDelete == null) {
            return null;
        }
        if (principalToDelete.isInRole(SecurityRoles.USER_SETTINGS_READ_WRITE)) {
            // The user was and user admin. Check if he/she is the latest admin. If so, block this operation because we don't want a user without the user admin role.
            // This check should be skipped/changed when LDAP is supported.
            if (getNumberOfUsersWithUserAdminRole() <= 1) {
                throw new EtmException(EtmException.NO_MORE_USER_ADMINS_LEFT);
            }
        }
        DeleteRequestBuilder builder = requestEnhancer.enhance(
                new DeleteRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId)
        );
        dataRepository.delete(builder);
        if (oldUserConfiguration != null) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(ConfigurationChangedAuditLog.Action.DELETE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId)
                    .setOldValue(oldUserConfiguration)
            );
        }
        return "{\"status\":\"success\"}";
    }

    @PUT
    @Path("/user/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_SETTINGS_READ_WRITE)
    public String addUser(@PathParam("userId") String userId, String json) {
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        var oldUserConfiguration = getCurrentUserConfiguration(userId);

        // Do a read and write of the user to make sure it's valid.
        var valueMap = toMap(json);
        var newPrincipal = loadPrincipal(toMapWithNamespace(json, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER));
        var newPassword = getString("new_password", valueMap);
        if (newPassword != null) {
            newPrincipal.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        }
        var currentPrincipal = loadPrincipal(userId);
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
        if (currentPrincipal == null && (newPrincipal.getApiKey() != null || newPrincipal.getSecondaryApiKey() != null)) {
            // New user, check if the api key is unique.
            if (newPrincipal.getApiKey() != null && !isApiKeyUnique(newPrincipal.getApiKey())) {
                throw new EtmException(EtmException.API_KEY_NOT_UNIQUE);
            }
            if (newPrincipal.getSecondaryApiKey() != null && !isApiKeyUnique(newPrincipal.getSecondaryApiKey())) {
                throw new EtmException(EtmException.API_KEY_NOT_UNIQUE);
            }
        }
        var newUserConfiguration = this.etmPrincipalConverter.writePrincipal(newPrincipal);
        var builder = requestEnhancer.enhance(
                new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId)
        ).setDoc(newUserConfiguration, XContentType.JSON)
                .setDocAsUpsert(true)
                .setDetectNoop(true);
        dataRepository.update(builder);
        if (!Objects.equals(oldUserConfiguration, newUserConfiguration)) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(oldUserConfiguration == null ? ConfigurationChangedAuditLog.Action.CREATE : ConfigurationChangedAuditLog.Action.UPDATE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId)
                    .setOldValue(oldUserConfiguration)
                    .setNewValue(newUserConfiguration)
            );
        }
        if (currentPrincipal == null) {
            // Add some default templates to the user so he/she is able to search.
            builder = requestEnhancer.enhance(
                    new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId)
            ).setDoc(new DefaultUserSettings().toJson(newPrincipal, etmConfiguration.getMaxSearchTemplateCount()), XContentType.JSON);
            dataRepository.update(builder);
        }
        if (userId.equals(getEtmPrincipal().getId())) {
            getEtmPrincipal().forceReload = true;
        }
        return "{ \"status\": \"success\" }";
    }

    private boolean isApiKeyUnique(String apiKey) {
        var searchResponse = dataRepository.search(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setFetchSource(false)
                .setQuery(
                        new BoolQueryBuilder()
                                .should(QueryBuilders.termQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.principalTags.getApiKeyTag(), apiKey))
                                .should(QueryBuilders.termQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.principalTags.getSecondaryApiKeyTag(), apiKey))
                                .minimumShouldMatch(1)
                )
                .setSize(1)
        );
        return searchResponse.getHits().getHits().length == 0;
    }

    @GET
    @Path("/user/api_key")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.USER_SETTINGS_READ_WRITE})
    public String createNewApiKey() {
        return new JsonBuilder().startObject().field("api_key", UUID.randomUUID().toString()).endObject().build();
    }

    /**
     * Returns the current user configuration.
     *
     * @return The current user configuration.
     */
    private String getCurrentUserConfiguration(String userId) {
        var getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId)
                .setFetchSource(true)
        );
        if (!getResponse.isExists()) {
            return null;
        }
        var user = this.etmPrincipalConverter.readPrincipal(getResponse.getSourceAsString());
        return this.etmPrincipalConverter.writePrincipal(user);
    }

    @GET
    @Path("/certificates")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.CLUSTER_SETTINGS_READ_WRITE)
    public String getCertificates() {
        SearchRequestBuilder searchRequestBuilder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME))
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_CERTIFICATE));
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
        if (!scrollableSearch.hasNext()) {
            return "{\"time_zone\": \"" + getEtmPrincipal().getTimeZone().getID() + "\"}";
        }
        StringBuilder result = new StringBuilder();
        result.append("{\"certificates\": [");
        boolean first = true;
        for (var searchHit : scrollableSearch) {
            if (!first) {
                result.append(",");
            }
            result.append(toStringWithoutNamespace(searchHit.getSourceAsMap(), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_CERTIFICATE));
            first = false;
        }
        result.append("],\"time_zone\": \"").append(getEtmPrincipal().getTimeZone().getID()).append("\"}");
        return result.toString();
    }

    @GET
    @Path("/certificate/download/{host}/{port}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.CLUSTER_SETTINGS_READ_WRITE)
    public String getCertificateChain(@PathParam("host") String host, @PathParam("port") Integer port) {
        var certChain = downloadCertificateChain(host, port);
        return certChainToJson(certChain);
    }

    @POST
    @Path("/certificate/load/")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.CLUSTER_SETTINGS_READ_WRITE)
    public String getCertificateChain(String json) {
        var objectMap = toMap(json);
        var certificateString = getString("certificate", objectMap);
        var certChain = loadCertificate(certificateString);
        return certChainToJson(certChain);
    }

    private String certChainToJson(Certificate... certChain) {
        var builder = new JsonBuilder();
        builder.startObject();
        builder.startArray("certificates");
        if (certChain != null) {
            for (var cert : certChain) {
                if (cert instanceof X509Certificate) {
                    var x509Cert = (X509Certificate) cert;
                    builder.startObject();
                    builder.field("dn", x509Cert.getSubjectDN().getName());
                    builder.field("serial", x509Cert.getSerialNumber().toString(16));
                    builder.endObject();
                }
            }
        }
        builder.endArray().endObject();
        return builder.build();
    }


    @POST
    @Path("/certificate/import")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.CLUSTER_SETTINGS_READ_WRITE)
    public String importCertificate(String json) {
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        var objectMap = toMap(json);
        var host = getString("host", objectMap);
        var port = getInteger("port", objectMap);
        List<String> serials = getArray("serials", objectMap);
        Certificate[] certChain;
        if (host != null && port != null) {
            certChain = downloadCertificateChain(host, port);
        } else {
            certChain = loadCertificateChain(getArray("certificates", objectMap));
        }

        var result = new StringBuilder();
        result.append("{ \"certificates\": [");
        Arrays.stream(certChain).filter(c -> {
            if (!(c instanceof X509Certificate)) {
                return false;
            }
            return serials.contains((((X509Certificate) c).getSerialNumber().toString(16)));
        }).forEach(c -> {
            var certificate = new com.jecstar.etm.server.core.domain.cluster.certificate.Certificate((X509Certificate) c);
            var oldCertificateConfiguration = getCurrentCertificateConfiguration(certificate.getSerial());
            var newCertificateConfiguration = this.certificateConverter.write(certificate);
            IndexRequestBuilder indexRequestBuilder = requestEnhancer.enhance(new IndexRequestBuilder()
                    .setIndex(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                    .setId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_CERTIFICATE_ID_PREFIX + certificate.getSerial())
                    .setSource(newCertificateConfiguration, XContentType.JSON)
            );
            dataRepository.index(indexRequestBuilder);
            if (!Objects.equals(oldCertificateConfiguration, newCertificateConfiguration)) {
                storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                        .setPrincipalId(etmPrincipal.getId())
                        .setHandlingTime(now)
                        .setAction(oldCertificateConfiguration == null ? ConfigurationChangedAuditLog.Action.CREATE : ConfigurationChangedAuditLog.Action.UPDATE)
                        .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_CERTIFICATE)
                        .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_CERTIFICATE_ID_PREFIX + certificate.getSerial())
                        .setOldValue(oldCertificateConfiguration)
                        .setNewValue(newCertificateConfiguration)
                );
            }
            if (result.toString().endsWith("}")) {
                result.append(",");
            }
            result.append(toStringWithoutNamespace(newCertificateConfiguration, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_CERTIFICATE));
        });
        result.append("]}");
        return result.toString();
    }

    @PUT
    @Path("/certificate/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.CLUSTER_SETTINGS_READ_WRITE)
    public Response updateCertificate(@PathParam("id") String id, String json) {
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        var oldCertificateConfiguration = getCurrentCertificateConfiguration(id);
        if (oldCertificateConfiguration == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Map<String, Object> inputMap = toMap(json);
        com.jecstar.etm.server.core.domain.cluster.certificate.Certificate certificate = this.certificateConverter.read(oldCertificateConfiguration);
        certificate.setTrustAnchor(getBoolean(com.jecstar.etm.server.core.domain.cluster.certificate.Certificate.TRUST_ANCHOR, inputMap));
        Collection<String> usages = getArray(com.jecstar.etm.server.core.domain.cluster.certificate.Certificate.USAGE, inputMap);
        for (String usage : usages) {
            certificate.addUsage(Usage.safeValueOf(usage));
        }
        var newCertificateConfiguration = this.certificateConverter.write(certificate);
        UpdateRequestBuilder builder = requestEnhancer.enhance(
                new UpdateRequestBuilder()
                        .setIndex(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                        .setId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_CERTIFICATE_ID_PREFIX + id)
                        .setDoc(newCertificateConfiguration, XContentType.JSON)
        );
        dataRepository.update(builder);
        if (!Objects.equals(oldCertificateConfiguration, newCertificateConfiguration)) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(ConfigurationChangedAuditLog.Action.UPDATE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_CERTIFICATE)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_CERTIFICATE_ID_PREFIX + certificate.getSerial())
                    .setOldValue(oldCertificateConfiguration)
                    .setNewValue(newCertificateConfiguration)
            );
        }
        return Response.ok("{\"status\":\"success\"}", MediaType.APPLICATION_JSON).build();
    }

    @DELETE
    @Path("/certificate/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.CLUSTER_SETTINGS_READ_WRITE)
    public String deleteCertificate(@PathParam("id") String id) {
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        var oldCertificateConfiguration = getCurrentCertificateConfiguration(id);
        DeleteRequestBuilder builder = requestEnhancer.enhance(
                new DeleteRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_CERTIFICATE_ID_PREFIX + id)
        );
        dataRepository.delete(builder);
        if (oldCertificateConfiguration != null) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(ConfigurationChangedAuditLog.Action.DELETE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_CERTIFICATE)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_CERTIFICATE_ID_PREFIX + id)
                    .setOldValue(oldCertificateConfiguration)
            );
        }
        return "{\"status\":\"success\"}";
    }

    /**
     * Download the certificate chain from a given host.
     *
     * @param host The hostname of the server to download the certificate chain from.
     * @param port The port on which the server process is listening.
     * @return An array with server certificates.
     */
    private Certificate[] downloadCertificateChain(String host, int port) {
        Socket socket = null;
        try {
            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new TrustAllTrustManager()}, null);
            var socketFactory = sslContext.getSocketFactory();
            socket = socketFactory.createSocket(host, port);
            socket.setSoTimeout(5000);
            var session = ((SSLSocket) socket).getSession();
            return session.getPeerCertificates();
        } catch (SSLPeerUnverifiedException e) {
            return downloadWithStartTls(host, port);
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new EtmException((EtmException.WRAPPED_EXCEPTION), e);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    if (log.isDebugLevelEnabled()) {
                        log.logDebugMessage(e.getMessage(), e);
                    }
                }
            }
        }
    }

    /**
     * Try to download the peer certificates with support for SMTP STARTTLS support.
     *
     * @param host The hostname of the server to download the certificate chain from.
     * @param port The port on which the server process is listening.
     * @return An array with server certificates.
     */
    private Certificate[] downloadWithStartTls(String host, int port) {
        Socket socket = null;
        try {
            socket = new Socket(host, port);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream outputStream = socket.getOutputStream();
            waitForReader(reader, 1000);
            while (waitForReader(reader, 100)) {
                if (!reader.readLine().startsWith("220")) {
                    return null;
                }
            }
            outputStream.write("EHLO etm\r\n".getBytes());
            outputStream.flush();
            waitForReader(reader, 1000);
            while (waitForReader(reader, 100)) {
                if (!reader.readLine().startsWith("250")) {
                    return null;
                }
            }
            outputStream.write("STARTTLS\r\n".getBytes());
            outputStream.flush();
            waitForReader(reader, 1000);
            while (waitForReader(reader, 100)) {
                var line = reader.readLine();
                if (line.startsWith("220")) {
                    SSLContext sc = SSLContext.getInstance("TLS");
                    sc.init(null, new TrustManager[]{new TrustAllTrustManager()}, null);
                    SSLSocketFactory sslSocketFactory = sc.getSocketFactory();
                    SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(
                            socket,
                            socket.getInetAddress().getHostAddress(),
                            socket.getPort(),
                            true);
                    sslSocket.startHandshake();
                    return sslSocket.getSession().getPeerCertificates();
                }
            }
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new EtmException((EtmException.WRAPPED_EXCEPTION), e);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    if (log.isDebugLevelEnabled()) {
                        log.logDebugMessage(e.getMessage(), e);
                    }
                }

            }
        }
        return null;
    }

    private boolean waitForReader(BufferedReader reader, int timeout) throws IOException {
        var startTime = System.currentTimeMillis();
        while ((!reader.ready()) && (System.currentTimeMillis() - startTime < timeout)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return reader.ready();
    }


    private Certificate[] loadCertificateChain(List<String> certificateStrings) {
        var result = new ArrayList<Certificate>();
        for (var certificateString : certificateStrings) {
            result.addAll(Arrays.asList(loadCertificate(certificateString)));
        }
        return result.toArray(Certificate[]::new);
    }

    private Certificate[] loadCertificate(String certificateString) {
        try {
            return CertificateFactory.getInstance("X.509").generateCertificates(new ByteArrayInputStream(certificateString.getBytes())).toArray(Certificate[]::new);
        } catch (CertificateException e) {
            throw new EtmException((EtmException.WRAPPED_EXCEPTION), e);
        }
    }

    /**
     * Returns the current certificate configuration.
     *
     * @return The current certificate configuration.
     */
    private String getCurrentCertificateConfiguration(String certificateSerial) {
        var getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_CERTIFICATE_ID_PREFIX + certificateSerial)
                .setFetchSource(true)
        );
        if (!getResponse.isExists()) {
            return null;
        }
        var certificate = this.certificateConverter.read(getResponse.getSourceAsString());
        return this.certificateConverter.write(certificate);
    }

    @GET
    @Path("/groups")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.GROUP_SETTINGS_READ, SecurityRoles.GROUP_SETTINGS_READ_WRITE, SecurityRoles.USER_SETTINGS_READ, SecurityRoles.USER_SETTINGS_READ_WRITE})
    public String getGroups() {
        SearchRequestBuilder searchRequestBuilder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME))
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP));
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
        StringBuilder result = new StringBuilder();
        result.append("{\"groups\": [");
        boolean first = true;
        for (var searchHit : scrollableSearch) {
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
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        EtmGroup group = etmConfiguration.getDirectory().getGroup(groupName);
        if (group == null) {
            throw new EtmException(EtmException.INVALID_LDAP_GROUP);
        }
        var oldGroupConfiguration = getCurrentGroupConfiguration(groupName);
        EtmGroup currentGroup = loadGroup(groupName);
        if (currentGroup != null) {
            // Merge the existing group with the new one.
            // Currently no properties need to be merged
            group = currentGroup;
        }
        group.setLdapBase(true);
        var newGroupConfiguration = this.etmPrincipalConverter.writeGroup(group);
        IndexRequestBuilder builder = requestEnhancer.enhance(
                new IndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName)
        )
                .setSource(newGroupConfiguration, XContentType.JSON);
        dataRepository.index(builder);
        if (!Objects.equals(oldGroupConfiguration, newGroupConfiguration)) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(oldGroupConfiguration == null ? ConfigurationChangedAuditLog.Action.CREATE : ConfigurationChangedAuditLog.Action.UPDATE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName)
                    .setOldValue(oldGroupConfiguration)
                    .setNewValue(newGroupConfiguration)
            );
        }
        return toStringWithoutNamespace(this.etmPrincipalConverter.writeGroup(group), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP);
    }

    @PUT
    @Path("/group/{groupName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.GROUP_SETTINGS_READ_WRITE)
    public String addGroup(@PathParam("groupName") String groupName, String json) {
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        var oldGroupConfiguration = getCurrentGroupConfiguration(groupName);
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
        var newGroupConfiguration = this.etmPrincipalConverter.writeGroup(newGroup);
        UpdateRequestBuilder builder = requestEnhancer.enhance(
                new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName)
        )
                .setDoc(newGroupConfiguration, XContentType.JSON)
                .setDocAsUpsert(true)
                .setDetectNoop(true);
        dataRepository.update(builder);
        if (!Objects.equals(oldGroupConfiguration, newGroupConfiguration)) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(oldGroupConfiguration == null ? ConfigurationChangedAuditLog.Action.CREATE : ConfigurationChangedAuditLog.Action.UPDATE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName)
                    .setOldValue(oldGroupConfiguration)
                    .setNewValue(newGroupConfiguration)
            );
        }
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
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        var oldGroupConfiguration = getCurrentGroupConfiguration(groupName);

        List<String> adminGroups = getGroupsWithRole(SecurityRoles.USER_SETTINGS_READ_WRITE);
        if (adminGroups.contains(groupName)) {
            // Check if there are admins left if this group is removed.
            // This check should be skipped/changed when LDAP is supported.
            adminGroups.remove(groupName);
            if (getNumberOfUsersWithUserAdminRole(adminGroups) < 1) {
                throw new EtmException(EtmException.NO_MORE_USER_ADMINS_LEFT);
            }
        }
        BulkRequestBuilder bulkRequestBuilder = requestEnhancer.enhance(new BulkRequestBuilder());
        bulkRequestBuilder.add(
                requestEnhancer.enhance(
                        new DeleteRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName)
                ).build()
        );
        removeGroupFromPrincipal(bulkRequestBuilder, groupName);
        dataRepository.bulk(bulkRequestBuilder);
        if (oldGroupConfiguration != null) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(ConfigurationChangedAuditLog.Action.DELETE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName)
                    .setOldValue(oldGroupConfiguration)
            );
        }
        // Force a reload of the principal if he/she is in the deleted group.
        EtmPrincipal principal = getEtmPrincipal();
        if (principal.isInGroup(groupName)) {
            principal.forceReload = true;
        }
        return "{\"status\":\"success\"}";
    }

    private void removeGroupFromPrincipal(BulkRequestBuilder bulkRequestBuilder, String groupName) {
        SearchRequestBuilder searchRequestBuilder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME))
                .setFetchSource(false)
                .setQuery(QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP))
                        .must(QueryBuilders.termQuery(this.principalTags.getGroupsTag(), groupName))
                );
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
        for (var searchHit : scrollableSearch) {
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

    /**
     * Returns the current group configuration.
     *
     * @return The current group configuration.
     */
    private String getCurrentGroupConfiguration(String groupName) {
        var getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName)
                .setFetchSource(true)
        );
        if (!getResponse.isExists()) {
            return null;
        }
        var group = this.etmPrincipalConverter.readGroup(getResponse.getSourceAsString());
        return this.etmPrincipalConverter.writeGroup(group);
    }

    @GET
    @Path("/notifiers")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({
            SecurityRoles.NOTIFIERS_READ,
            SecurityRoles.NOTIFIERS_READ_WRITE})
    public String getNotifiers() {
        SearchRequestBuilder searchRequestBuilder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME))
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER));
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
        return notifiersToJson(scrollableSearch);
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
        SearchRequestBuilder searchRequestBuilder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME))
                .setFetchSource(new String[]{
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER + "." + Notifier.NAME,
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER + "." + Notifier.NOTIFIER_TYPE
                }, null)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER));
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
        return notifiersToJson(scrollableSearch);
    }

    private String notifiersToJson(ScrollableSearch scrollableSearch) {
        StringBuilder result = new StringBuilder();
        result.append("{\"notifiers\": [");
        boolean first = true;
        for (var searchHit : scrollableSearch) {
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
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        var oldNotifierConfiguration = getCurrentNotifierConfiguration(notifierName);

        // Do a read and write of the notifier to make sure it's valid.
        Map<String, Object> objectMap = toMapWithNamespace(json, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER);
        var testConnection = getBoolean("test_connection", getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER, objectMap), false);
        Notifier notifier = this.notifierConverter.read(objectMap);

        if (testConnection) {
            ConnectionTestResult testResult = notifier.testConnection(dataRepository);
            if (testResult.isFailed()) {
                return new JsonBuilder().startObject().field("status", "failed").field("reason", testResult.getErrorMessage()).endObject().build();
            }
        }
        var newNotifierConfiguration = this.notifierConverter.write(notifier);
        IndexRequestBuilder builder = requestEnhancer.enhance(
                new IndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER_ID_PREFIX + notifier.getName())
        )
                .setSource(newNotifierConfiguration, XContentType.JSON);
        dataRepository.index(builder);
        if (!Objects.equals(oldNotifierConfiguration, newNotifierConfiguration)) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(oldNotifierConfiguration == null ? ConfigurationChangedAuditLog.Action.CREATE : ConfigurationChangedAuditLog.Action.UPDATE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER_ID_PREFIX + notifierName)
                    .setOldValue(oldNotifierConfiguration)
                    .setNewValue(newNotifierConfiguration)
            );
        }
        return "{ \"status\": \"success\" }";
    }

    @DELETE
    @Path("/notifier/{notifierName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.NOTIFIERS_READ_WRITE)
    public String deleteNotifier(@PathParam("notifierName") String notifierName) {
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        var oldNotifierConfiguration = getCurrentNotifierConfiguration(notifierName);

        BulkRequestBuilder bulkRequestBuilder = requestEnhancer.enhance(new BulkRequestBuilder());
        bulkRequestBuilder.add(
                requestEnhancer.enhance(
                        new DeleteRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER_ID_PREFIX + notifierName)
                ).build()
        );
        removeNotifierFromSignalsAndPrincipals(bulkRequestBuilder, notifierName);
        dataRepository.bulk(bulkRequestBuilder);
        if (oldNotifierConfiguration != null) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(ConfigurationChangedAuditLog.Action.DELETE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER_ID_PREFIX + notifierName)
                    .setOldValue(oldNotifierConfiguration)
            );
        }
        return "{\"status\":\"success\"}";
    }

    private void removeNotifierFromSignalsAndPrincipals(BulkRequestBuilder bulkRequestBuilder, String notifierName) {
        SearchRequestBuilder searchRequestBuilder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME))
                .setFetchSource(true)
                .setQuery(QueryBuilders.boolQuery()
                        .should(QueryBuilders.termQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.principalTags.getNotifiersTag(), notifierName))
                        .should(QueryBuilders.termQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.principalTags.getSignalsTag() + "." + this.principalTags.getNotifiersTag(), notifierName))
                        .should(QueryBuilders.termQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP + "." + this.principalTags.getSignalsTag() + "." + this.principalTags.getNotifiersTag(), notifierName))
                        .should(QueryBuilders.termQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP + "." + this.principalTags.getNotifiersTag(), notifierName))
                        .minimumShouldMatch(1)
                );
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
        for (var searchHit : scrollableSearch) {
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
                        requestEnhancer.enhance(new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, searchHit.getId()))
                                .setDoc(valueMap)
                                .setDocAsUpsert(true)
                                .setDetectNoop(true)
                                .build()
                );
            }
        }
    }

    /**
     * Returns the current notifier configuration.
     *
     * @return The current notifier configuration.
     */
    private String getCurrentNotifierConfiguration(String notifierName) {
        var getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER_ID_PREFIX + notifierName)
                .setFetchSource(true)
        );
        if (!getResponse.isExists()) {
            return null;
        }
        var notifier = this.notifierConverter.read(getResponse.getSourceAsString());
        return this.notifierConverter.write(notifier);
    }

    private UpdateRequestBuilder createPrincipalUpdateRequest(EtmPrincipal principal) {
        return requestEnhancer.enhance(
                new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + principal.getId())
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
        SearchRequestBuilder searchRequestBuilder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME))
                .setFetchSource(true)
                .setQuery(QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP))
                        .must(QueryBuilders.termsQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP + "." + this.principalTags.getRolesTag(), roles))
                );
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
        List<String> groups = new ArrayList<>();
        if (scrollableSearch.hasNext()) {
            for (var searchHit : scrollableSearch) {
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
        SearchRequestBuilder builder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME))
                .setFetchSource(false)
                .setSize(0)
                .trackTotalHits(true)
                .setQuery(query);
        SearchResponse response = dataRepository.search(builder);
        return response.getHits().getTotalHits().value;
    }

    /**
     * Store a <code>ConfigurationChangedAuditLog</code> instance.
     *
     * @param auditLogBuilder The builder that contains the data.
     */
    private void storeConfigurationChangedAuditLog(ConfigurationChangedAuditLogBuilder auditLogBuilder) {
        var now = Instant.now();
        IndexRequestBuilder indexRequestBuilder = requestEnhancer.enhance(
                new IndexRequestBuilder(ElasticsearchLayout.AUDIT_LOG_INDEX_PREFIX + dateTimeFormatterIndexPerWeek.format(now))
                        .setId(auditLogBuilder.getId())
        )
                .setSource(this.configurationChangedAuditLogConverter.write(
                        auditLogBuilder
                                .setId(idGenerator.createId())
                                .setTimestamp(now)
                                .build()
                ), XContentType.JSON);
        dataRepository.indexAsync(indexRequestBuilder, DataRepository.noopActionListener());
    }
}