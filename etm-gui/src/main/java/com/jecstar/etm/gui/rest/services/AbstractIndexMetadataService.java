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
	protected Map<String, List<String>> getIndexFields(Client client, String indexName) {
		Map<String, List<String>> names = new HashMap<String, List<String>>();
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
				List<String> values = new ArrayList<String>();
				values.add("_exists_");
				values.add("_missing_");
				try {
					Map<String, Object> valueMap = mappingMetaData.getSourceAsMap();
					addFieldProperties(values, "", valueMap);
					if (names.containsKey(mappingMetadataCursor.key)) {
						List<String> currentValues = names.get(mappingMetadataCursor.key);
						for (String value : values) {
							if (!currentValues.contains(value)) {
								currentValues.add(value);
							}
						}
					} else {
						names.put(mappingMetadataCursor.key, values);
					}
				} catch (IOException e) {
					// Error will never been thrown. See mappingMetaData.getSourceAsMap() sources for detailes.
				}
			}
			
		}
		return names;
	}

	@SuppressWarnings("unchecked")
	private void addFieldProperties(List<String> names, String prefix, Map<String, Object> valueMap) {
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
				addFieldProperties(names, name, entryValues);
			} else {
				if (!names.contains(name)) {
					names.add(name);
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
