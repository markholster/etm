/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.gui.rest.services;

import com.jecstar.etm.gui.rest.AbstractGuiService;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetFieldMappingsRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.GetMappingsRequestBuilder;

import java.util.*;

public abstract class AbstractIndexMetadataService extends AbstractGuiService {

    // Alphabetic ascending list of keywords that need to be excludes for the user.
    private static final String[] keywordsToExclude = new String[]{
            "endpoints.reading_endpoint_handlers.forced",
            "endpoints.writing_endpoint_handler.forced",
            "event_hashes",
            "temp_for_correlations"
    };

    /**
     * Gives a list of fields of an index.
     *
     * @param dataRepository    The <code>DataRepository</code>.
     * @param indexName         The name of the index to find the keywords for.
     * @return A <code>List</code> with <code>Keywords</code>
     */
    protected List<Keyword> getIndexFields(DataRepository dataRepository, String indexName) {
        var keywords = new ArrayList<Keyword>();
        keywords.add(Keyword.EXISTS);
        keywords.add(Keyword.ID);
        var mappingsResponse = dataRepository.indicesGetMappings(new GetMappingsRequestBuilder().setIndices(indexName));
        var mappings = mappingsResponse.mappings();
        for (var mappingMetaData : mappings.values()) {
            var valueMap = mappingMetaData.getSourceAsMap();
            addFieldProperties(keywords, "", valueMap);
        }
        return keywords;
    }

    protected String getSortProperty(DataRepository dataRepository, String indexName, String propertyName) {
        var response = dataRepository.indicesGetFieldMappings(new GetFieldMappingsRequestBuilder().setIndices(indexName).setFields(propertyName));

        if (response.mappings().isEmpty()) {
            return null;
        }
        // Iterate over all mappings trying to find a field definition.
        for (var indexMap : response.mappings().values()) {
            for (var fieldMappingMetaData : indexMap.values()) {
                if (!propertyName.equals(fieldMappingMetaData.fullName())) {
                    continue;
                }
                var metadataMapping = fieldMappingMetaData.sourceAsMap();
                var iterator = metadataMapping.keySet().iterator();
                if (!iterator.hasNext()) {
                    continue;
                }
                var type = getString("type", getObject(iterator.next(), fieldMappingMetaData.sourceAsMap(), Collections.emptyMap()));
                if (type != null) {
                    return "text".equals(type) ? propertyName + KEYWORD_SUFFIX : propertyName;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void addFieldProperties(List<Keyword> keywords, String prefix, Map<String, Object> valueMap) {
        valueMap = getObject("properties", valueMap);
        if (valueMap == null) {
            return;
        }
        for (var entry : valueMap.entrySet()) {
            var entryValues = (Map<String, Object>) entry.getValue();
            var name = determinePropertyForFieldName(prefix, entry.getKey());
            if (Arrays.binarySearch(keywordsToExclude, name) >= 0) {
                continue;
            }

            if (entryValues.containsKey("properties")) {
                addFieldProperties(keywords, name, entryValues);
            } else {
                var keyword = new Keyword(name, (String) entryValues.get("type"));
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
