package com.jecstar.etm.server.core.configuration.converter.json;

import java.util.Map;

import com.jecstar.etm.server.core.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.configuration.WaitStrategy;
import com.jecstar.etm.server.core.configuration.converter.EtmConfigurationConverter;
import com.jecstar.etm.server.core.configuration.converter.EtmConfigurationTags;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

/**
 * Converter class that converts a <code>TelemetryEvent</code> to a JSON string.
 * 
 * @author mark
 */
public class EtmConfigurationConverterJsonImpl implements EtmConfigurationConverter<String> {
	
	private final EtmConfigurationTags tags = new EtmConfigurationTagsJsonImpl();
	private final JsonConverter converter = new JsonConverter();
	
	@Override
	public String write(EtmConfiguration nodeConfiguration, EtmConfiguration defaultConfiguration) {
		final StringBuilder sb = new StringBuilder();
		boolean added = false;
		sb.append("{");
		if (nodeConfiguration == null) {
			// only add the defaults.
			added = this.converter.addStringElementToJsonBuffer(this.tags.getNodeNameTag(), ElasticsearchLayout.CONFIGURATION_INDEX_TYPE_NODE_DEFAULT, sb, !added) || added;
			added = this.converter.addIntegerElementToJsonBuffer(this.tags.getEnhancingHandlerCountTag(), defaultConfiguration.getEnhancingHandlerCount(), sb, !added) || added;
			added = this.converter.addIntegerElementToJsonBuffer(this.tags.getPersistingHandlerCountTag(), defaultConfiguration.getPersistingHandlerCount(), sb, !added) || added;
			added = this.converter.addIntegerElementToJsonBuffer(this.tags.getEventBufferSizeTag(), defaultConfiguration.getEventBufferSize(), sb, !added) || added;
			added = this.converter.addStringElementToJsonBuffer(this.tags.getWaitStrategyTag(), defaultConfiguration.getWaitStrategy().name(), sb, !added);
			added = this.converter.addIntegerElementToJsonBuffer(this.tags.getPersistingBulkCountTag(), defaultConfiguration.getPersistingBulkCount(), sb, !added) || added;
			added = this.converter.addIntegerElementToJsonBuffer(this.tags.getPersistingBulkSizeTag(), defaultConfiguration.getPersistingBulkSize(), sb, !added) || added;
			added = this.converter.addIntegerElementToJsonBuffer(this.tags.getPersistingBulkTimeTag(), defaultConfiguration.getPersistingBulkTime(), sb, !added) || added;
			added = this.converter.addIntegerElementToJsonBuffer(this.tags.getShardsPerIndexTag(), defaultConfiguration.getShardsPerIndex(), sb, !added) || added;
			added = this.converter.addIntegerElementToJsonBuffer(this.tags.getReplicasPerIndexTag(), defaultConfiguration.getReplicasPerIndex(), sb, !added) || added;
			added = this.converter.addIntegerElementToJsonBuffer(this.tags.getMaxEventIndexCountTag(), defaultConfiguration.getMaxEventIndexCount(), sb, !added) || added;
			added = this.converter.addIntegerElementToJsonBuffer(this.tags.getMaxMetricsIndexCountTag(), defaultConfiguration.getMaxMetricsIndexCount(), sb, !added) || added;
			added = this.converter.addIntegerElementToJsonBuffer(this.tags.getMaxAuditLogIndexCountTag(), defaultConfiguration.getMaxAuditLogIndexCount(), sb, !added) || added;
			added = this.converter.addIntegerElementToJsonBuffer(this.tags.getMaxSearchResultDownloadRowsTag(), defaultConfiguration.getMaxSearchResultDownloadRows(), sb, !added) || added;
			added = this.converter.addIntegerElementToJsonBuffer(this.tags.getMaxSearchHistoryCountTag(), defaultConfiguration.getMaxSearchHistoryCount(), sb, !added) || added;
			added = this.converter.addIntegerElementToJsonBuffer(this.tags.getMaxSearchTemplateCountTag(), defaultConfiguration.getMaxSearchTemplateCount(), sb, !added) || added;
			added = this.converter.addIntegerElementToJsonBuffer(this.tags.getMaxGraphCountTag(), defaultConfiguration.getMaxGraphCount(), sb, !added) || added;
			added = this.converter.addIntegerElementToJsonBuffer(this.tags.getMaxDashboardCountTag(), defaultConfiguration.getMaxDashboardCount(), sb, !added) || added;
			added = this.converter.addIntegerElementToJsonBuffer(this.tags.getWaitForActiveShardsTag(), defaultConfiguration.getWaitForActiveShards(), sb, !added) || added;
			added = this.converter.addLongElementToJsonBuffer(this.tags.getQueryTimeoutTag(), defaultConfiguration.getQueryTimeout(), sb, !added) || added;
			added = this.converter.addIntegerElementToJsonBuffer(this.tags.getRetryOnConflictCountTag(), defaultConfiguration.getRetryOnConflictCount(), sb, !added) || added;
		} else {
			added = this.converter.addStringElementToJsonBuffer(this.tags.getNodeNameTag(), nodeConfiguration.getNodeName(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getEnhancingHandlerCountTag(), defaultConfiguration.getEnhancingHandlerCount(), nodeConfiguration.getEnhancingHandlerCount(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getPersistingHandlerCountTag(), defaultConfiguration.getPersistingHandlerCount(), nodeConfiguration.getPersistingHandlerCount(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getEventBufferSizeTag(), defaultConfiguration.getEventBufferSize(), nodeConfiguration.getEventBufferSize(), sb, !added) || added;
			added = addStringWhenNotDefault(this.tags.getWaitStrategyTag(), defaultConfiguration.getWaitStrategy().name(), nodeConfiguration.getWaitStrategy().name(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getPersistingBulkCountTag(), defaultConfiguration.getPersistingBulkCount(), nodeConfiguration.getPersistingBulkCount(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getPersistingBulkSizeTag(), defaultConfiguration.getPersistingBulkSize(), nodeConfiguration.getPersistingBulkSize(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getPersistingBulkTimeTag(), defaultConfiguration.getPersistingBulkTime(), nodeConfiguration.getPersistingBulkTime(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getShardsPerIndexTag(), defaultConfiguration.getShardsPerIndex(), nodeConfiguration.getShardsPerIndex(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getReplicasPerIndexTag(), defaultConfiguration.getReplicasPerIndex(), nodeConfiguration.getReplicasPerIndex(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getMaxEventIndexCountTag(), defaultConfiguration.getMaxEventIndexCount(), nodeConfiguration.getMaxEventIndexCount(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getMaxMetricsIndexCountTag(), defaultConfiguration.getMaxMetricsIndexCount(), nodeConfiguration.getMaxMetricsIndexCount(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getMaxAuditLogIndexCountTag(), defaultConfiguration.getMaxAuditLogIndexCount(), nodeConfiguration.getMaxAuditLogIndexCount(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getMaxSearchResultDownloadRowsTag(), defaultConfiguration.getMaxSearchResultDownloadRows(), nodeConfiguration.getMaxSearchResultDownloadRows(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getMaxSearchHistoryCountTag(), defaultConfiguration.getMaxSearchHistoryCount(), nodeConfiguration.getMaxSearchHistoryCount(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getMaxSearchTemplateCountTag(), defaultConfiguration.getMaxSearchTemplateCount(), nodeConfiguration.getMaxSearchTemplateCount(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getMaxGraphCountTag(), defaultConfiguration.getMaxGraphCount(), nodeConfiguration.getMaxGraphCount(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getMaxDashboardCountTag(), defaultConfiguration.getMaxDashboardCount(), nodeConfiguration.getMaxDashboardCount(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getWaitForActiveShardsTag(), defaultConfiguration.getWaitForActiveShards(), nodeConfiguration.getWaitForActiveShards(), sb, !added) || added;
			added = addLongWhenNotDefault(this.tags.getQueryTimeoutTag(), defaultConfiguration.getQueryTimeout(), nodeConfiguration.getQueryTimeout(), sb, !added) || added;
			added = addIntegerWhenNotDefault(this.tags.getRetryOnConflictCountTag(), defaultConfiguration.getRetryOnConflictCount(), nodeConfiguration.getRetryOnConflictCount(), sb, !added) || added;
		}
		sb.append("}");
		return sb.toString();
	}
	
	@Override
	public EtmConfiguration read(String nodeJsonContent, String defaultJsonContent, String nodeName) {
		Map<String, Object> nodeMap = nodeJsonContent == null ? null : this.converter.toMap(nodeJsonContent);
		Map<String, Object> defaultMap = this.converter.toMap(defaultJsonContent);
		return read(nodeMap, defaultMap, nodeName);
	}
	
	public EtmConfiguration read(Map<String, Object> nodeMap, Map<String, Object> defaultMap, String nodeName) {
		EtmConfiguration etmConfiguration = new EtmConfiguration(nodeName);
		etmConfiguration.setEnhancingHandlerCount(getIntValue(this.tags.getEnhancingHandlerCountTag(), defaultMap, nodeMap));
		etmConfiguration.setPersistingHandlerCount(getIntValue(this.tags.getPersistingHandlerCountTag(), defaultMap, nodeMap));
		etmConfiguration.setEventBufferSize(getIntValue(this.tags.getEventBufferSizeTag(), defaultMap, nodeMap));
		etmConfiguration.setWaitStrategy(WaitStrategy.safeValueOf(getStringValue(this.tags.getWaitStrategyTag(), defaultMap, nodeMap)));
		etmConfiguration.setPersistingBulkCount(getIntValue(this.tags.getPersistingBulkCountTag(), defaultMap, nodeMap));
		etmConfiguration.setPersistingBulkSize(getIntValue(this.tags.getPersistingBulkSizeTag(), defaultMap, nodeMap));
		etmConfiguration.setPersistingBulkTime(getIntValue(this.tags.getPersistingBulkTimeTag(), defaultMap, nodeMap));
		etmConfiguration.setShardsPerIndex(getIntValue(this.tags.getShardsPerIndexTag(), defaultMap, nodeMap));
		etmConfiguration.setReplicasPerIndex(getIntValue(this.tags.getReplicasPerIndexTag(), defaultMap, nodeMap));
		etmConfiguration.setMaxEventIndexCount(getIntValue(this.tags.getMaxEventIndexCountTag(), defaultMap, nodeMap));
		etmConfiguration.setMaxMetricsIndexCount(getIntValue(this.tags.getMaxMetricsIndexCountTag(), defaultMap, nodeMap));
		etmConfiguration.setMaxAuditLogIndexCount(getIntValue(this.tags.getMaxAuditLogIndexCountTag(), defaultMap, nodeMap));
		etmConfiguration.setMaxSearchResultDownloadRows(getIntValue(this.tags.getMaxSearchResultDownloadRowsTag(), defaultMap, nodeMap));
		etmConfiguration.setMaxSearchHistoryCount(getIntValue(this.tags.getMaxSearchHistoryCountTag(), defaultMap, nodeMap));
		etmConfiguration.setMaxSearchTemplateCount(getIntValue(this.tags.getMaxSearchTemplateCountTag(), defaultMap, nodeMap));
		etmConfiguration.setMaxGraphCount(getIntValue(this.tags.getMaxGraphCountTag(), defaultMap, nodeMap));
		etmConfiguration.setMaxDashboardCount(getIntValue(this.tags.getMaxDashboardCountTag(), defaultMap, nodeMap));
		etmConfiguration.setWaitForActiveShards(getIntValue(this.tags.getWaitForActiveShardsTag(), defaultMap, nodeMap));
		etmConfiguration.setQueryTimeout(getLongValue(this.tags.getQueryTimeoutTag(), defaultMap, nodeMap));
		etmConfiguration.setRetryOnConflictCount(getIntValue(this.tags.getRetryOnConflictCountTag(), defaultMap, nodeMap));
		return etmConfiguration;
	}
	
	private Integer getIntValue(String tag, Map<String, Object> defaultMap, Map<String, Object> nodeMap) {
		if (nodeMap != null && nodeMap.containsKey(tag)) {
			return ((Number) nodeMap.get(tag)).intValue();
		} else if (defaultMap != null && defaultMap.containsKey(tag)) {
			return ((Number) defaultMap.get(tag)).intValue();
		}
		return null;
	}
	
	private Long getLongValue(String tag, Map<String, Object> defaultMap, Map<String, Object> nodeMap) {
		if (nodeMap != null && nodeMap.containsKey(tag)) {
			return ((Number) nodeMap.get(tag)).longValue();
		} else if (defaultMap != null && defaultMap.containsKey(tag)) {
			return ((Number) defaultMap.get(tag)).longValue();
		}
		return null;
	}
	
	private String getStringValue(String tag, Map<String, Object> defaultMap, Map<String, Object> nodeMap) {
		if (nodeMap != null && nodeMap.containsKey(tag)) {
			return (nodeMap.get(tag)).toString();
		} else if (defaultMap != null && defaultMap.containsKey(tag)) {
			return (defaultMap.get(tag)).toString();
		}
		return null;
	}
	
	private boolean addIntegerWhenNotDefault(String tag, int defaultValue, int specificValue, StringBuilder buffer, boolean firstElement) {
		if (defaultValue == specificValue) {
			return false;
		}
		return this.converter.addIntegerElementToJsonBuffer(tag, specificValue, buffer, firstElement);
	}
	
	private boolean addLongWhenNotDefault(String tag, long defaultValue, long specificValue, StringBuilder buffer, boolean firstElement) {
		if (defaultValue == specificValue) {
			return false;
		}
		return this.converter.addLongElementToJsonBuffer(tag, specificValue, buffer, firstElement);
	}
	
	private boolean addStringWhenNotDefault(String tag, String defaultValue, String specificValue, StringBuilder buffer, boolean firstElement) {
		if (defaultValue.equals(specificValue)) {
			return false;
		}
		return this.converter.addStringElementToJsonBuffer(tag, specificValue, buffer, firstElement);
	}

	@Override
	public EtmConfigurationTags getTags() {
		return this.tags;
	}

}
