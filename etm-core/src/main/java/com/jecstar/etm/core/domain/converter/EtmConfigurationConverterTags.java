package com.jecstar.etm.core.domain.converter;

public interface EtmConfigurationConverterTags {

	String getEnhancingHandlerCountTag();

	String getPersistingHandlerCountTag();

	String getEventBufferSizeTag();

	String getPersistingBulkSizeTag();

	String getShardsPerIndexTag();

	String getReplicasPerIndexTag();

}
