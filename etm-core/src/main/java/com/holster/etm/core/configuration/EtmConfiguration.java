package com.holster.etm.core.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// TODO configuration is now done in Properties but should be stored in zookeeper.
public class EtmConfiguration extends Properties {

    private static final long serialVersionUID = -8967252221295201228L;

	public List<String> getCassandraContactPoints() {
		return Arrays.stream(super.getProperty("cassandra.contact_points", "127.0.0.1").split(",")).collect(Collectors.toList());
	}
	
	public String getCassandraUsername() {
		return super.getProperty("cassandra.username");
	}
	
	public String getCassandraPassword() {
		return super.getProperty("cassandra.password");
	}
	
	public String getCassandraKeyspace() {
		return super.getProperty("cassandra.keyspace", "etm");
	}
	
	public String getSolrCollectionName() {
		return super.getProperty("solr.collection", "etm");
	}
	
	public int getEnhancingHandlerCount() {
		return Integer.valueOf(super.getProperty("etm.enhancing_handler_count", "5"));
	}
	
	public int getIndexingHandlerCount() {
		return Integer.valueOf(super.getProperty("etm.indexing_handler_count", "5"));
	}
	
	public int getPersistingHandlerCount() {
		return Integer.valueOf(super.getProperty("etm.persisting_handler_count", "5"));
	}
	
	public int getRingbufferSize() {
		return Integer.valueOf(super.getProperty("etm.ringbuffer_size", "4096"));
	}
	
	public long getEndpointCacheExpiryTime() {
		return Long.valueOf(super.getProperty("etm.endpoint_cache_expiry_time", "60000"));
	}
	
	public TimeUnit getStatisticsTimeUnit() {
		return TimeUnit.valueOf(super.getProperty("etm.statistics_timeunit", "MINUTES"));
	}
}
