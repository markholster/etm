package com.jecstar.etm.launcher;

import java.time.ZonedDateTime;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;

import com.jecstar.etm.processor.core.persisting.elastic.AbstractElasticTelemetryEventPersister;
import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.configuration.LdapConfiguration;
import com.jecstar.etm.server.core.configuration.License;
import com.jecstar.etm.server.core.configuration.WaitStrategy;
import com.jecstar.etm.server.core.configuration.converter.EtmConfigurationConverter;
import com.jecstar.etm.server.core.configuration.converter.LdapConfigurationConverter;
import com.jecstar.etm.server.core.configuration.converter.json.EtmConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.configuration.converter.json.LdapConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.ldap.Directory;

public class ElasticBackedEtmConfiguration extends EtmConfiguration {
	
	private final Client elasticClient;
	private final EtmConfigurationConverter<String> etmConfigurationConverter = new EtmConfigurationConverterJsonImpl();
	private final LdapConfigurationConverter<String> ldapConfigurationConverter = new LdapConfigurationConverterJsonImpl();
	
	private final long updateCheckInterval = 60 * 1000;
	private long lastCheckedForUpdates;
	
	private long eventsPersistedToday = 0;
	private long sizePersistedToday = 0;
	
	public ElasticBackedEtmConfiguration(String nodeName, final Client elasticClient) {
		super(nodeName);
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
		if (license.getMaxEventsPerDay() == -1) {
			return false;
		}
		return this.eventsPersistedToday > license.getMaxEventsPerDay();  
	}
	
	@Override
	public Boolean isLicenseSizeExceeded() {
		reloadConfigurationWhenNecessary();
		License license = getLicense();
		if (license == null) {
			return true;
		}
		if (license.getMaxSizePerDay() == -1) {
			return false;
		}
		return this.sizePersistedToday > license.getMaxSizePerDay();  
	}
	
	@Override
	public Directory getDirectory() {
		reloadConfigurationWhenNecessary();
		return super.getDirectory();
	}
	
	private boolean reloadConfigurationWhenNecessary() {
		boolean changed = false;
		if (System.currentTimeMillis() - this.lastCheckedForUpdates <= this.updateCheckInterval) {
			return changed;
		}
		
		String indexNameOfToday = ElasticSearchLayout.ETM_EVENT_INDEX_PREFIX + AbstractElasticTelemetryEventPersister.dateTimeFormatterIndexPerDay.format(ZonedDateTime.now());
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
		ListenableActionFuture<GetResponse> licenseExecute = this.elasticClient.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_LICENSE, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_LICENSE_ID).execute();
		ListenableActionFuture<GetResponse> ldapExecute = this.elasticClient.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_LDAP, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_LDAP_DEFAULT).execute();
		GetResponse defaultResponse = this.elasticClient.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_NODE, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_NODE_DEFAULT).get();
		GetResponse nodeResponse = this.elasticClient.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_NODE, getNodeName()).get();

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
		changed = this.mergeAndNotify(etmConfiguration);
		this.lastCheckedForUpdates = System.currentTimeMillis();
		return changed;
	}
}
