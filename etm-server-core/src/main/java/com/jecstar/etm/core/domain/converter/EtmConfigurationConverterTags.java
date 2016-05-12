package com.jecstar.etm.core.domain.converter;

public interface EtmConfigurationConverterTags {

	String getLicenseTag();
	
	String getEnhancingHandlerCountTag();

	String getPersistingHandlerCountTag();

	String getEventBufferSizeTag();

	String getPersistingBulkCountTag();
	String getPersistingBulkSizeTag();
	String getPersistingBulkTimeTag();

	String getShardsPerIndexTag();

	String getReplicasPerIndexTag();

}
