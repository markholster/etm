package com.jecstar.etm.launcher;

import com.jecstar.etm.server.core.domain.configuration.*;
import com.jecstar.etm.server.core.domain.configuration.converter.EtmConfigurationConverter;
import com.jecstar.etm.server.core.domain.configuration.converter.LdapConfigurationConverter;
import com.jecstar.etm.server.core.domain.configuration.converter.json.EtmConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.SyncActionListener;
import com.jecstar.etm.server.core.elasticsearch.builder.GetRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.IndicesStatsRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.domain.IndicesStatsResponse;
import com.jecstar.etm.server.core.ldap.Directory;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.Map;
import java.util.Set;

public class ElasticBackedEtmConfiguration extends EtmConfiguration {

    private final DataRepository dataRepository;
    private final String elasticsearchIndexName;
    private final EtmConfigurationConverter<String> etmConfigurationConverter = new EtmConfigurationConverterJsonImpl();
    private final LdapConfigurationConverter ldapConfigurationConverter = new LdapConfigurationConverter();

    private long lastCheckedForUpdates;
    private long sizePersisted = 0;
    private int activeNodes;

    /**
     * Creates a new <code>ElasticBackedEtmConfiguration</code> instance.
     *
     * @param nodeName       The name of the node this instance is running on.
     * @param dataRepository The <code>DataRepository</code>.
     */
    ElasticBackedEtmConfiguration(String nodeName, final DataRepository dataRepository) {
        this(nodeName, dataRepository, ElasticsearchLayout.CONFIGURATION_INDEX_NAME);
    }

    /**
     * Creates a new <code>ElasticBackedEtmConfiguration</code> instances. The data is loaded from the given elasticsearchIndexName.
     * <p>
     * This constructor is mainly used when converting the database by an <code>EtmMigrator</code>.
     *
     * @param nodeName               The name of the node this instance is running on.
     * @param dataRepository         The <code>DataRepository</code>.
     * @param elasticsearchIndexName The name of the elasticsearch index to load the data from.
     */
    public ElasticBackedEtmConfiguration(String nodeName, final DataRepository dataRepository, String elasticsearchIndexName) {
        super(nodeName);
        this.elasticsearchIndexName = elasticsearchIndexName;
        this.dataRepository = dataRepository;
        reloadConfigurationWhenNecessary();
    }

    @Override
    public License getLicense() {
        reloadConfigurationWhenNecessary();
        return super.getLicense();
    }

    @Override
    public Set<RemoteCluster> getRemoteClusters() {
        reloadConfigurationWhenNecessary();
        return super.getRemoteClusters();
    }

    @Override
    public EtmConfiguration addRemoteCluster(RemoteCluster remoteCluster) {
        reloadConfigurationWhenNecessary();
        return super.addRemoteCluster(remoteCluster);
    }

    @Override
    public EtmConfiguration removeRemoteCluster(RemoteCluster remoteCluster) {
        reloadConfigurationWhenNecessary();
        return super.removeRemoteCluster(remoteCluster);
    }

    @Override
    public int getEnhancingHandlerCount() {
        reloadConfigurationWhenNecessary();
        return super.getEnhancingHandlerCount();
    }

    @Override
    public int getPersistingHandlerCount() {
        reloadConfigurationWhenNecessary();
        return super.getPersistingHandlerCount();
    }

    @Override
    public int getEventBufferSize() {
        reloadConfigurationWhenNecessary();
        return super.getEventBufferSize();
    }

    @Override
    public WaitStrategy getWaitStrategy() {
        reloadConfigurationWhenNecessary();
        return super.getWaitStrategy();
    }

    @Override
    public int getPersistingBulkSize() {
        reloadConfigurationWhenNecessary();
        return super.getPersistingBulkSize();
    }

    @Override
    public int getPersistingBulkCount() {
        reloadConfigurationWhenNecessary();
        return super.getPersistingBulkCount();
    }

    @Override
    public int getPersistingBulkTime() {
        reloadConfigurationWhenNecessary();
        return super.getPersistingBulkTime();
    }

    @Override
    public int getPersistingBulkThreads() {
        reloadConfigurationWhenNecessary();
        return super.getPersistingBulkThreads();
    }

    @Override
    public int getShardsPerIndex() {
        reloadConfigurationWhenNecessary();
        return super.getShardsPerIndex();
    }

    @Override
    public int getReplicasPerIndex() {
        reloadConfigurationWhenNecessary();
        return super.getReplicasPerIndex();
    }

    @Override
    public int getMaxEventIndexCount() {
        reloadConfigurationWhenNecessary();
        return super.getMaxEventIndexCount();
    }

    @Override
    public int getMaxMetricsIndexCount() {
        reloadConfigurationWhenNecessary();
        return super.getMaxMetricsIndexCount();
    }

    @Override
    public int getMaxAuditLogIndexCount() {
        reloadConfigurationWhenNecessary();
        return super.getMaxAuditLogIndexCount();
    }

    @Override
    public int getWaitForActiveShards() {
        reloadConfigurationWhenNecessary();
        return super.getWaitForActiveShards();
    }

    @Override
    public long getQueryTimeout() {
        reloadConfigurationWhenNecessary();
        return super.getQueryTimeout();
    }

    @Override
    public int getRetryOnConflictCount() {
        reloadConfigurationWhenNecessary();
        return super.getRetryOnConflictCount();
    }

    @Override
    public int getMaxSearchResultDownloadRows() {
        reloadConfigurationWhenNecessary();
        return super.getMaxSearchResultDownloadRows();
    }

    @Override
    public int getMaxSearchHistoryCount() {
        reloadConfigurationWhenNecessary();
        return super.getMaxSearchHistoryCount();
    }

    @Override
    public int getMaxSearchTemplateCount() {
        reloadConfigurationWhenNecessary();
        return super.getMaxSearchTemplateCount();
    }

    @Override
    public int getMaxGraphCount() {
        reloadConfigurationWhenNecessary();
        return super.getMaxGraphCount();
    }

    @Override
    public int getMaxDashboardCount() {
        reloadConfigurationWhenNecessary();
        return super.getMaxDashboardCount();
    }

    @Override
    public int getMaxSignalCount() {
        reloadConfigurationWhenNecessary();
        return super.getMaxSignalCount();
    }

    @Override
    public long getSessionTimeout() {
        reloadConfigurationWhenNecessary();
        return super.getSessionTimeout();
    }

    @Override
    public int getImportProfileCacheSize() {
        reloadConfigurationWhenNecessary();
        return super.getImportProfileCacheSize();
    }

    @Override
    public boolean isLicenseExpired() {
        reloadConfigurationWhenNecessary();
        return super.isLicenseExpired();
    }

    @Override
    public Boolean isLicenseAlmostExpired() {
        reloadConfigurationWhenNecessary();
        return super.isLicenseAlmostExpired();
    }

    @Override
    public boolean isLicenseValid() {
        reloadConfigurationWhenNecessary();
        return super.isLicenseValid();
    }

    @Override
    public Boolean isLicenseSizeExceeded() {
        reloadConfigurationWhenNecessary();
        var license = getLicense();
        if (license == null) {
            return true;
        }
        return !license.getMaxDatabaseSize().equals(License.UNLIMITED) && this.sizePersisted > license.getMaxDatabaseSize();
    }

    @Override
    public Directory getDirectory() {
        reloadConfigurationWhenNecessary();
        return super.getDirectory();
    }

    @Override
    public Integer getActiveNodeCount() {
        reloadConfigurationWhenNecessary();
        return this.activeNodes;
    }

    @SuppressWarnings("unchecked")
    private boolean reloadConfigurationWhenNecessary() {
        var updateCheckInterval = 60 * 1000L;
        if (System.currentTimeMillis() - this.lastCheckedForUpdates <= updateCheckInterval) {
            return false;
        }
        this.lastCheckedForUpdates = System.currentTimeMillis();
        this.dataRepository.indicesGetStatsAsync(new IndicesStatsRequestBuilder()
                        .clear()
                        .setStore(true),
                new ActionListener<>() {
                    @Override
                    public void onResponse(IndicesStatsResponse response) {
                        sizePersisted = response.getTotal().getStore().getSizeInBytes();
                    }

                    @Override
                    public void onFailure(Exception e) {
                    }
                });

        SyncActionListener<GetResponse> licenseExecute = DataRepository.syncActionListener(30_000L);
        SyncActionListener<GetResponse> ldapExecute = DataRepository.syncActionListener(30_000L);
        SyncActionListener<SearchResponse> nodesExecute = DataRepository.syncActionListener(30_000L);
        this.dataRepository.getAsync(new GetRequestBuilder(this.elasticsearchIndexName, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LICENSE_DEFAULT), licenseExecute);
        this.dataRepository.getAsync(new GetRequestBuilder(this.elasticsearchIndexName, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LDAP_DEFAULT), ldapExecute);
        this.dataRepository.searchAsync(
                new SearchRequestBuilder()
                        .setIndices(this.elasticsearchIndexName)
                        .setSize(500)
                        .setFetchSource(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE + "." + etmConfigurationConverter.getTags().getInstancesTag(), null)
                        .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE)),
                nodesExecute
        );
        var defaultResponse = this.dataRepository.get(new GetRequestBuilder(this.elasticsearchIndexName, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_NODE_DEFAULT));
        var nodeResponse = this.dataRepository.get(new GetRequestBuilder(this.elasticsearchIndexName, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE_ID_PREFIX + getNodeName()));

        var defaultContent = defaultResponse.getSourceAsString();
        String nodeContent = null;

        if (nodeResponse.isExists()) {
            nodeContent = nodeResponse.getSourceAsString();
        }
        var etmConfiguration = this.etmConfigurationConverter.read(nodeContent, defaultContent, "temp-for-reload-merge");
        var licenseResponse = licenseExecute.get();
        if (licenseResponse != null && licenseResponse.isExists() && !licenseResponse.isSourceEmpty() && licenseResponse.getSourceAsMap().containsKey(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LICENSE)) {
            var licenseObject = (Map<String, Object>) licenseResponse.getSourceAsMap().get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LICENSE);
            var license = licenseObject.get(this.etmConfigurationConverter.getTags().getLicenseTag());
            if (license != null && isValidLicenseKey(license.toString())) {
                etmConfiguration.setLicenseKey(license.toString());
            }
        }
        var ldapResponse = ldapExecute.get();
        if (ldapResponse != null && ldapResponse.isExists() && !ldapResponse.isSourceEmpty()) {
            var ldapConfiguration = this.ldapConfigurationConverter.read(ldapResponse.getSourceAsString());
            if (super.getDirectory() != null) {
                super.getDirectory().merge(ldapConfiguration);
            } else {
                var directory = new Directory(this.dataRepository, ldapConfiguration);
                setDirectory(directory);
            }
        } else {
            setDirectory(null);
        }
        var searchResponse = nodesExecute.get();
        int activeNodes = 0;
        for (var searchHit : searchResponse.getHits().getHits()) {
            activeNodes += this.etmConfigurationConverter.getActiveNodeCount(searchHit.getSourceAsString());
        }
        this.activeNodes = activeNodes > 0 ? activeNodes : 1;
        return this.mergeAndNotify(etmConfiguration);
    }
}
