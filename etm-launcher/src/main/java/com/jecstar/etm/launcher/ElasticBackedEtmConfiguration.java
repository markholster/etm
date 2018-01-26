package com.jecstar.etm.launcher;

import com.jecstar.etm.server.core.domain.configuration.*;
import com.jecstar.etm.server.core.domain.configuration.converter.EtmConfigurationConverter;
import com.jecstar.etm.server.core.domain.configuration.converter.LdapConfigurationConverter;
import com.jecstar.etm.server.core.domain.configuration.converter.json.EtmConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.domain.configuration.converter.json.LdapConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.ldap.Directory;
import com.jecstar.etm.server.core.util.DateUtils;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ElasticBackedEtmConfiguration extends EtmConfiguration {
	
	private static final DateTimeFormatter dateTimeFormatterIndexPerDay = DateUtils.getIndexPerDayFormatter();
	private final Client elasticClient;
	private final String elasticsearchIndexName;
	private final EtmConfigurationConverter<String> etmConfigurationConverter = new EtmConfigurationConverterJsonImpl();
	private final LdapConfigurationConverter<String> ldapConfigurationConverter = new LdapConfigurationConverterJsonImpl();

    private long lastCheckedForUpdates;
	
	private long eventsPersistedToday = 0;
	private long sizePersistedToday = 0;

    /**
     * Creates a new <code>ElasticBackedEtmConfiguration</code> instance.
     *
     * @param nodeName The name of the node this instance is running on.
     * @param elasticClient The elasticsearch client.
     */
	public ElasticBackedEtmConfiguration(String nodeName, final Client elasticClient) {
	    this(nodeName, elasticClient, ElasticsearchLayout.CONFIGURATION_INDEX_NAME);
	}

    /**
     * Creates a new <code>ElasticBackedEtmConfiguration</code> instances. The data is loaded from the given elasticsearchIndexName.
     *
     * This constructor is mainly used when converting the database by an <code>EtmMigrator</code>.
     *
     * @param nodeName The name of the node this instance is running on.
     * @param elasticClient The elasticsearch client.
     * @param elasticsearchIndexName The name of the elasticsearch index to load the data from.
     */
	public ElasticBackedEtmConfiguration(String nodeName, final Client elasticClient, String elasticsearchIndexName) {
		super(nodeName);
		this.elasticsearchIndexName = elasticsearchIndexName;
		this.elasticClient = elasticClient;
		reloadConfigurationWhenNecessary();
	}
	
	@Override
	public License getLicense() {
		reloadConfigurationWhenNecessary();
		return super.getLicense();
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
	public long getSessionTimeout() {
		reloadConfigurationWhenNecessary();
		return super.getSessionTimeout();
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
	public Boolean isLicenseCountExceeded() {
        reloadConfigurationWhenNecessary();
        License license = getLicense();
        if (license == null) {
            return true;
        }
        return license.getMaxEventsPerDay() != -1 && this.eventsPersistedToday > license.getMaxEventsPerDay();
    }
	
	@Override
	public Boolean isLicenseSizeExceeded() {
		reloadConfigurationWhenNecessary();
		License license = getLicense();
		if (license == null) {
			return true;
		}
		return license.getMaxSizePerDay() != -1 && this.sizePersistedToday > license.getMaxSizePerDay();
	}
	
	@Override
	public Directory getDirectory() {
		reloadConfigurationWhenNecessary();
		return super.getDirectory();
	}
	
	private boolean reloadConfigurationWhenNecessary() {
        long updateCheckInterval = 60 * 1000;
        if (System.currentTimeMillis() - this.lastCheckedForUpdates <= updateCheckInterval) {
			return false;
		}
		
		String indexNameOfToday = ElasticsearchLayout.EVENT_INDEX_PREFIX + dateTimeFormatterIndexPerDay.format(ZonedDateTime.now());
		this.elasticClient.admin().indices().prepareStats(indexNameOfToday)
				.clear()
				.setStore(true)
				.execute(new ActionListener<IndicesStatsResponse>() {
					@Override
					public void onResponse(IndicesStatsResponse response) {
						sizePersistedToday = response.getTotal().store.getSizeInBytes();
					}

					@Override
					public void onFailure(Exception e) {
					}
				});
		this.elasticClient.prepareSearch(indexNameOfToday)
			.setQuery(new BoolQueryBuilder().mustNot(new QueryStringQueryBuilder("endpoints.writing_endpoint_handler.application.name: \"Enterprise Telemetry Monitor\"")))
			.execute(new ActionListener<SearchResponse>() {
				@Override
				public void onResponse(SearchResponse response) {
					eventsPersistedToday = response.getHits().getTotalHits();
				}

				@Override
				public void onFailure(Exception e) {
				}
			});
		ActionFuture<GetResponse> licenseExecute = this.elasticClient.prepareGet(this.elasticsearchIndexName, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LICENSE_DEFAULT).execute();
		ActionFuture<GetResponse> ldapExecute = this.elasticClient.prepareGet(this.elasticsearchIndexName, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LDAP_DEFAULT).execute();
		GetResponse defaultResponse = this.elasticClient.prepareGet(this.elasticsearchIndexName, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_NODE_DEFAULT).get();
		GetResponse nodeResponse = this.elasticClient.prepareGet(this.elasticsearchIndexName, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + getNodeName()).get();

		String defaultContent = defaultResponse.getSourceAsString();
		String nodeContent = null;

		if (nodeResponse.isExists()) {
			nodeContent = nodeResponse.getSourceAsString();
		}
		EtmConfiguration etmConfiguration = this.etmConfigurationConverter.read(nodeContent, defaultContent, "temp-for-reload-merge");
		GetResponse licenseResponse = licenseExecute.actionGet();
		if (licenseResponse.isExists() && !licenseResponse.isSourceEmpty()) {
			Object license = licenseResponse.getSourceAsMap().get(this.etmConfigurationConverter.getTags().getLicenseTag());
			if (license != null && isValidLicenseKey(license.toString())) {
				etmConfiguration.setLicenseKey(license.toString());
			}
		}
		GetResponse ldapResponse = ldapExecute.actionGet();
		if (ldapResponse.isExists() &&!ldapResponse.isSourceEmpty()) {
			LdapConfiguration ldapConfiguration = this.ldapConfigurationConverter.read(ldapResponse.getSourceAsString());
			if (super.getDirectory() != null) {
				super.getDirectory().merge(ldapConfiguration);
			} else {
				Directory directory = new Directory(ldapConfiguration);
				setDirectory(directory);
			}
		} else {
			setDirectory(null);
		}
		boolean changed = this.mergeAndNotify(etmConfiguration);
		this.lastCheckedForUpdates = System.currentTimeMillis();
		return changed;
	}
}
