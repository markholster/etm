package com.jecstar.etm.server.core.domain.converter;

public interface EtmConfigurationTags {

	String getLicenseTag();
	
	String getEnhancingHandlerCountTag();
	String getPersistingHandlerCountTag();
	String getEventBufferSizeTag();

	String getPersistingBulkCountTag();
	String getPersistingBulkSizeTag();
	String getPersistingBulkTimeTag();

	String getShardsPerIndexTag();
	String getReplicasPerIndexTag();

	String getMaxEventIndexCountTag();
	String getMaxMetricsIndexCountTag();
	
	String getWriteConsistencyTag();
	String getRetryOnConflictCountTag();
	String getQueryTimeoutTag();
	String getMaxSearchResultDownloadRowsTag();


}
