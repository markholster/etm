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

package com.jecstar.etm.server.core.persisting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jecstar.etm.server.core.EtmException;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper class around a <code>SearchHit</code> that is capable of redacting the content based on <code>JsonPath</code> expressions.
 */
public class RedactedSearchHit {

    private static final Configuration configuration = Configuration.builder()
            .jsonProvider(new JacksonJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .options(EnumSet.noneOf(Option.class)).build();

    private final String REDACTED_TEXT = "[REDACTED]";

    private final SearchHit searchHit;
    private final Set<JsonPath> redactedFields;
    private final boolean hasSource;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String source = null;
    private Map<String, Object> sourceAsMap = null;

    public RedactedSearchHit(SearchHit searchHit) {
        this(searchHit, null);
    }

    public RedactedSearchHit(SearchHit searchHit, Set<JsonPath> redactedFields) {
        this.searchHit = searchHit;
        this.hasSource = searchHit.hasSource();
        this.redactedFields = redactedFields;
    }

    public String getIndex() {
        return this.searchHit.getIndex();
    }

    public String getId() {
        return this.searchHit.getId();
    }

    public String getSourceAsString() {
        if (!this.hasSource) {
            return null;
        }
        if (this.source == null) {
            this.source = this.searchHit.getSourceAsString();
            if (this.redactedFields != null && redactedFields.size() > 0) {
                final var context = JsonPath.parse(this.source, configuration);
                this.redactedFields.forEach(c -> {
                    try {
                        context.set(c, REDACTED_TEXT);
                    } catch (InvalidPathException p) {
                    }
                });
                this.source = context.jsonString();
            }
        }
        return this.source;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSourceAsMap() {
        if (!this.hasSource) {
            return null;
        }
        if (this.sourceAsMap == null) {
            try {
                this.sourceAsMap = this.objectMapper.readValue(getSourceAsString(), HashMap.class);
            } catch (IOException e) {
                throw new EtmException(EtmException.INVALID_JSON_EXPRESSION, e);
            }
        }
        return this.sourceAsMap;
    }
}
