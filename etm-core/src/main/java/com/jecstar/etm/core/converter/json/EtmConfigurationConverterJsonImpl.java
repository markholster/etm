package com.jecstar.etm.core.converter.json;

import java.util.Map;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.configuration.WriteConsistency;
import com.jecstar.etm.core.converter.EtmConfigurationConverter;
import com.jecstar.etm.core.converter.EtmConfigurationConverterTags;
import com.jecstar.etm.core.util.ObjectUtils;

/**
 * Converter class that converts a <code>TelemetryEvent</code> to a JSON string.
 * 
 * @author mark
 */
public class EtmConfigurationConverterJsonImpl extends AbstractJsonConverter implements EtmConfigurationConverter<String>{
	
	private EtmConfigurationConverterTags tags = new EtmConfigurationConverterTagsJsonImpl();

	@Override
	public String convert(EtmConfiguration nodeConfiguration, EtmConfiguration defaultConfiguration) {
		final StringBuilder sb = new StringBuilder();
		boolean added = false;
		sb.append("{");
		if (nodeConfiguration == null) {
			// only add the defaults.
			added = addIntegerElementToJsonBuffer(this.tags.getEnhancingHandlerCountTag(), defaultConfiguration.getEnhancingHandlerCount(), sb, !added) || added;
			added = addIntegerElementToJsonBuffer(this.tags.getPersistingHandlerCountTag(), defaultConfiguration.getPersistingHandlerCount(), sb, !added) || added;
			added = addIntegerElementToJsonBuffer(this.tags.getEventBufferSizeTag(), defaultConfiguration.getEventBufferSize(), sb, !added) || added;
			
			added = addIntegerElementToJsonBuffer(this.tags.getPersistingBulkSizeTag(), defaultConfiguration.getPersistingBulkSize(), sb, !added) || added;
			added = addIntegerElementToJsonBuffer(this.tags.getShardsPerIndexTag(), defaultConfiguration.getShardsPerIndex(), sb, !added) || added;
			added = addIntegerElementToJsonBuffer(this.tags.getReplicasPerIndexTag(), defaultConfiguration.getReplicasPerIndex(), sb, !added) || added;
			added = addIntegerElementToJsonBuffer(this.tags.getMaxIndexCountTag(), defaultConfiguration.getMaxIndexCount(), sb, !added) || added;
			added = addWriteConsitencyElementToJsonBuffer(this.tags.getWriteConsistencyTag(), defaultConfiguration.getWriteConsistency(), sb, !added) || added;
		} else {
			added = addIntegerWhenNotDefault(this.tags.getEnhancingHandlerCountTag(), defaultConfiguration.getEnhancingHandlerCount(), nodeConfiguration.getEnhancingHandlerCount(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getPersistingHandlerCountTag(), defaultConfiguration.getPersistingHandlerCount(), nodeConfiguration.getPersistingHandlerCount(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getEventBufferSizeTag(), defaultConfiguration.getEventBufferSize(), nodeConfiguration.getEventBufferSize(), sb, !added) || added;
			
			added = addIntegerWhenNotDefault(this.tags.getPersistingBulkSizeTag(), defaultConfiguration.getPersistingBulkSize(), nodeConfiguration.getPersistingBulkSize(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getShardsPerIndexTag(), defaultConfiguration.getShardsPerIndex(), nodeConfiguration.getShardsPerIndex(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getReplicasPerIndexTag(), defaultConfiguration.getReplicasPerIndex(), nodeConfiguration.getReplicasPerIndex(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getMaxIndexCountTag(), defaultConfiguration.getMaxIndexCount(), nodeConfiguration.getMaxIndexCount(), sb, !added) || added;
			added = addWriteConsistencyWhenNotDefault(this.tags.getWriteConsistencyTag(), defaultConfiguration.getWriteConsistency(), nodeConfiguration.getWriteConsistency(), sb, !added) || added;
		}
		sb.append("}");
		return sb.toString();
	}
	
	@Override
	public EtmConfiguration convert(String nodeJsonContent, String defaultJsonContent, String nodeName, String component) {
		Map<String, Object> nodeMap = nodeJsonContent == null ? null : toMap(nodeJsonContent);
		Map<String, Object> defaultMap = toMap(defaultJsonContent);
		EtmConfiguration etmConfiguration = new EtmConfiguration(nodeName, component);
		if (defaultMap.containsKey(this.tags.getLicenseTag())) {
			etmConfiguration.setLicenseKey(getStringValue(this.tags.getLicenseTag(), defaultMap, nodeMap));
		}
		etmConfiguration.setEnhancingHandlerCount(getIntValue(this.tags.getEnhancingHandlerCountTag(), defaultMap, nodeMap));
		etmConfiguration.setPersistingHandlerCount(getIntValue(this.tags.getPersistingHandlerCountTag(), defaultMap, nodeMap));
		etmConfiguration.setEventBufferSize(getIntValue(this.tags.getEventBufferSizeTag(), defaultMap, nodeMap));
		etmConfiguration.setPersistingBulkSize(getIntValue(this.tags.getPersistingBulkSizeTag(), defaultMap, nodeMap));
		etmConfiguration.setShardsPerIndex(getIntValue(this.tags.getShardsPerIndexTag(), defaultMap, nodeMap));
		etmConfiguration.setReplicasPerIndex(getIntValue(this.tags.getReplicasPerIndexTag(), defaultMap, nodeMap));
		etmConfiguration.setMaxIndexCount(getIntValue(this.tags.getMaxIndexCountTag(), defaultMap, nodeMap));
		etmConfiguration.setWriteConsistency(getWriteConsitencyValue(this.tags.getWriteConsistencyTag(), defaultMap, nodeMap));
		return etmConfiguration;
	}
	
	private Integer getIntValue(String tag, Map<String, Object> defaultMap, Map<String, Object> nodeMap) {
		if (nodeMap != null && nodeMap.containsKey(tag)) {
			return ((Number) nodeMap.get(tag)).intValue();
		} else {
			return ((Number) defaultMap.get(tag)).intValue();
		}
	}

	private String getStringValue(String tag, Map<String, Object> defaultMap, Map<String, Object> nodeMap) {
		if (nodeMap != null && nodeMap.containsKey(tag)) {
			return nodeMap.get(tag).toString();
		} else {
			return defaultMap.get(tag).toString();
		}
	}
	
	private WriteConsistency getWriteConsitencyValue(String tag, Map<String, Object> defaultMap, Map<String, Object> nodeMap) {
		if (nodeMap != null && nodeMap.containsKey(tag)) {
			return WriteConsistency.valueOf(nodeMap.get(tag).toString());
		} else {
			return WriteConsistency.valueOf(defaultMap.get(tag).toString());
		}
	}
	
	private boolean addIntegerWhenNotDefault(String tag, int defaultValue, int specificValue, StringBuilder buffer, boolean firstElement) {
		if (defaultValue == specificValue) {
			return false;
		}
		return addIntegerElementToJsonBuffer(tag, specificValue, buffer, firstElement);
	}
	
	private boolean addWriteConsistencyWhenNotDefault(String tag, WriteConsistency defaultValue, WriteConsistency specificValue, StringBuilder buffer, boolean firstElement) {
		if (ObjectUtils.equalsNullProof(defaultValue, specificValue)) {
			return false;
		}
		return addWriteConsitencyElementToJsonBuffer(tag, specificValue, buffer, firstElement);
	}


	private boolean addWriteConsitencyElementToJsonBuffer(String tag, WriteConsistency writeConsistency, StringBuilder buffer, boolean firstElement) {
		if (writeConsistency == null) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("\"" + escapeToJson(tag) + "\": \"" + escapeToJson(writeConsistency.name()) + "\"");
		return true;

	}

	@Override
	public EtmConfigurationConverterTags getTags() {
		return this.tags;
	}

}
