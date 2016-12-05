package com.jecstar.etm.gui.rest.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsAction;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.jecstar.etm.gui.rest.AbstractJsonService;

public abstract class AbstractIndexMetadataService extends AbstractJsonService {

	
	/**
	 * Gives a list of fields of an index per index type.
	 * 
	 * @param client The elasticsearch client.
	 * @param indexName The name of the index to find the keywords for.
	 * @return
	 */
	protected Map<String, List<Keyword>> getIndexFields(Client client, String indexName) {
		Map<String, List<Keyword>> names = new HashMap<String, List<Keyword>>();
		GetMappingsResponse mappingsResponse = new GetMappingsRequestBuilder(client, GetMappingsAction.INSTANCE, indexName).get();
		ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = mappingsResponse.getMappings();
		Iterator<ObjectObjectCursor<String, ImmutableOpenMap<String, MappingMetaData>>> mappingsIterator = mappings.iterator();
		while (mappingsIterator.hasNext()) {
			ObjectObjectCursor<String, ImmutableOpenMap<String, MappingMetaData>> mappingsCursor = mappingsIterator.next();
			Iterator<ObjectObjectCursor<String, MappingMetaData>> mappingMetaDataIterator = mappingsCursor.value.iterator();
			while (mappingMetaDataIterator.hasNext()) {
				ObjectObjectCursor<String, MappingMetaData> mappingMetadataCursor = mappingMetaDataIterator.next();
				if ("_default_".equals(mappingMetadataCursor.key)) {
					continue;
				}
				MappingMetaData mappingMetaData = mappingMetadataCursor.value;
				List<Keyword> foundKeywords = new ArrayList<Keyword>();
				foundKeywords.add(Keyword.EXISTS);
				foundKeywords.add(Keyword.MISSING);
				foundKeywords.add(Keyword.TYPE);
				try {
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
				} catch (IOException e) {
					// Error will never been thrown. See mappingMetaData.getSourceAsMap() sources for detailes.
				}
			}
			
		}
		return names;
	}

	@SuppressWarnings("unchecked")
	private void addFieldProperties(List<Keyword> keywords, String prefix, Map<String, Object> valueMap) {
		valueMap = getObject("properties", valueMap);
		if (valueMap == null) {
			return;
		}
		// Remove temp fields that are used for correlating events.
		valueMap.remove("temp_for_correlations");
		// Remove event hashes field. It is used for internal handling of events.
		valueMap.remove("event_hashes");
		for (Entry<String, Object> entry : valueMap.entrySet()) {
			Map<String, Object> entryValues = (Map<String, Object>) entry.getValue();
			String name = determinePropertyForFieldName(prefix, entry.getKey());
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
