package com.jecstar.etm.core.domain.converter.json;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.converter.EtmConfigurationConverter;
import com.jecstar.etm.core.domain.converter.EtmConfigurationConverterTags;

/**
 * Converter class that converts a <code>TelemetryEvent</code> to a JSON string.
 * 
 * @author mark
 */
public class EtmConfigurationConverterJsonImpl extends AbstractJsonConverter implements EtmConfigurationConverter<String>{

	@Override
	public String convert(EtmConfiguration etmConfiguration, EtmConfiguration defaultConfiguration, EtmConfigurationConverterTags tags) {
		final StringBuilder sb = new StringBuilder();
		boolean added = false;
		sb.append("{");
		if (etmConfiguration == null) {
			// only add the defaults.
			added = addIntegerElementToJsonBuffer(tags.getEnhancingHandlerCountTag(), defaultConfiguration.getEnhancingHandlerCount(), sb, !added) || added;
			added = addIntegerElementToJsonBuffer(tags.getPersistingHandlerCountTag(), defaultConfiguration.getPersistingHandlerCount(), sb, !added) || added;
			added = addIntegerElementToJsonBuffer(tags.getEventBufferSizeTag(), defaultConfiguration.getEventBufferSize(), sb, !added) || added;
			
			added = addIntegerElementToJsonBuffer(tags.getPersistingBulkSizeTag(), defaultConfiguration.getPersistingBulkSize(), sb, !added) || added;
			added = addIntegerElementToJsonBuffer(tags.getShardsPerIndexTag(), defaultConfiguration.getShardsPerIndex(), sb, !added) || added;
			added = addIntegerElementToJsonBuffer(tags.getReplicasPerIndexTag(), defaultConfiguration.getReplicasPerIndex(), sb, !added) || added;
		} else {
			added = addIntegerWhenNotDefault(tags.getEnhancingHandlerCountTag(), defaultConfiguration.getEnhancingHandlerCount(), etmConfiguration.getEnhancingHandlerCount(), sb, !added) || added;
			added = addIntegerWhenNotDefault(tags.getPersistingHandlerCountTag(), defaultConfiguration.getPersistingHandlerCount(), etmConfiguration.getPersistingHandlerCount(), sb, !added) || added;
			added = addIntegerWhenNotDefault(tags.getEventBufferSizeTag(), defaultConfiguration.getEventBufferSize(), etmConfiguration.getEventBufferSize(), sb, !added) || added;
			
			added = addIntegerWhenNotDefault(tags.getPersistingBulkSizeTag(), defaultConfiguration.getPersistingBulkSize(), etmConfiguration.getPersistingBulkSize(), sb, !added) || added;
			added = addIntegerWhenNotDefault(tags.getShardsPerIndexTag(), defaultConfiguration.getShardsPerIndex(), etmConfiguration.getShardsPerIndex(), sb, !added) || added;
			added = addIntegerWhenNotDefault(tags.getReplicasPerIndexTag(), defaultConfiguration.getReplicasPerIndex(), etmConfiguration.getReplicasPerIndex(), sb, !added) || added;
		}
		sb.append("}");
		return sb.toString();
	}
	
	private boolean addIntegerWhenNotDefault(String tag, int defaultValue, int specificValue, StringBuilder buffer, boolean firstElement) {
		if (defaultValue == specificValue) {
			return false;
		}
		return addIntegerElementToJsonBuffer(tag, specificValue, buffer, firstElement);
	}
	

}
