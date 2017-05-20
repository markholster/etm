package com.jecstar.etm.server.core.configuration.converter;

public interface EtmConfigurationTags {

	String getNodeNameTag();
	String getLicenseTag();
	
	String getSessionTimeoutTag();
	
	String getEnhancingHandlerCountTag();
	String getPersistingHandlerCountTag();
	String getEventBufferSizeTag();
	String getWaitStrategyTag();

	String getPersistingBulkCountTag();
	String getPersistingBulkSizeTag();
	String getPersistingBulkTimeTag();

	String getShardsPerIndexTag();
	String getReplicasPerIndexTag();

	String getMaxEventIndexCountTag();
	String getMaxMetricsIndexCountTag();
	String getMaxAuditLogIndexCountTag();
	
	String getWaitForActiveShardsTag();
	String getRetryOnConflictCountTag();
	String getQueryTimeoutTag();
	String getMaxSearchResultDownloadRowsTag();
	String getMaxSearchHistoryCountTag();
	String getMaxSearchTemplateCountTag();
	String getMaxGraphCountTag();
	String getMaxDashboardCountTag();


}
