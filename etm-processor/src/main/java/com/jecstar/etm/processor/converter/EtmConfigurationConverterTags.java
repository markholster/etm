package com.jecstar.etm.processor.converter;

public interface EtmConfigurationConverterTags {

	String getLicenseTag();
	
	String getEnhancingHandlerCountTag();

	String getPersistingHandlerCountTag();

	String getEventBufferSizeTag();

	String getPersistingBulkSizeTag();

	String getShardsPerIndexTag();

	String getReplicasPerIndexTag();
	
	String getDataretentionTag();

}
