package com.jecstar.etm.gui.rest.services;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.jecstar.etm.gui.rest.AbstractJsonService;
import org.elasticsearch.action.admin.indices.mapping.get.*;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;

import java.util.*;
import java.util.Map.Entry;

public abstract class AbstractIndexMetadataService extends AbstractJsonService {

    // Alphabetic ascending list of keywords that need to be excludes for the user.
    private static final String[] keywordsToExclude = new String[] {
            "endpoints.reading_endpoint_handlers.forced",
            "endpoints.writing_endpoint_handler.forced",
            "event_hashes",
            "temp_for_correlations"
    };

	/**
	 * Gives a list of fields of an index per index type.
	 * 
	 * @param client The elasticsearch client.
	 * @param indexName The name of the index to find the keywords for.
	 * @return
	 */
	protected Map<String, List<Keyword>> getIndexFields(Client client, String indexName) {
		Map<String, List<Keyword>> names = new HashMap<>();
		GetMappingsResponse mappingsResponse = new GetMappingsRequestBuilder(client, GetMappingsAction.INSTANCE, indexName).get();
		ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = mappingsResponse.getMappings();
		for (ObjectObjectCursor<String, ImmutableOpenMap<String, MappingMetaData>> mappingsCursor : mappings) {
			for (ObjectObjectCursor<String, MappingMetaData> mappingMetadataCursor : mappingsCursor.value) {
				if ("_default_".equals(mappingMetadataCursor.key)) {
					continue;
				}
				MappingMetaData mappingMetaData = mappingMetadataCursor.value;
				List<Keyword> foundKeywords = new ArrayList<>();
				foundKeywords.add(Keyword.EXISTS);
				foundKeywords.add(Keyword.ID);
				foundKeywords.add(Keyword.TYPE);
				Map<String, Object> valueMap = mappingMetaData.getSourceAsMap();
				addFieldProperties(foundKeywords, "", valueMap);
				if (names.containsKey(mappingMetadataCursor.key)) {
					List<Keyword> currentKeywords = names.get(mappingMetadataCursor.key);
					for (Keyword value : foundKeywords) {
						if (!currentKeywords.contains(value)) {
							currentKeywords.add(value);
						}
					}
				} else {
					names.put(mappingMetadataCursor.key, foundKeywords);
				}
			}
		}
		return names;
	}
	
	protected String getSortProperty(Client client, String indexName, String indexType, String propertyName) {
		GetFieldMappingsRequestBuilder fieldRequest = client.admin().indices().prepareGetFieldMappings(indexName)
			.setFields(propertyName);
		if (indexType != null) {
			fieldRequest.addTypes(indexType);
		}
		GetFieldMappingsResponse response = fieldRequest.get();
		if (response.mappings().isEmpty()) {
			return propertyName;
		}
		Map<String, Object> sourceMap = response.mappings().values().iterator().next().values().iterator().next().values().iterator().next().sourceAsMap();
		String type = getString("type", getObject(propertyName, sourceMap));
		return ("text".equals(type) || "keyword".equals(type)) ? propertyName + KEYWORD_SUFFIX : propertyName;
	}

	@SuppressWarnings("unchecked")
	private void addFieldProperties(List<Keyword> keywords, String prefix, Map<String, Object> valueMap) {
		valueMap = getObject("properties", valueMap);
		if (valueMap == null) {
			return;
		}
		for (Entry<String, Object> entry : valueMap.entrySet()) {
			Map<String, Object> entryValues = (Map<String, Object>) entry.getValue();
			String name = determinePropertyForFieldName(prefix, entry.getKey());
			if (Arrays.binarySearch(keywordsToExclude, name) >=0) {
			    continue;
            }

			if (entryValues.containsKey("properties")) {
				addFieldProperties(keywords, name, entryValues);
			} else {
				Keyword keyword = new Keyword(name, (String) entryValues.get("type"));
				if (!keywords.contains(keyword)) {
					keywords.add(keyword);
				}
			}
		}
	}
	
	private String determinePropertyForFieldName(String prefix, String name) {
		if (prefix.length() == 0) {
			return name;
		}
		return prefix + "." + name;
	}

}
