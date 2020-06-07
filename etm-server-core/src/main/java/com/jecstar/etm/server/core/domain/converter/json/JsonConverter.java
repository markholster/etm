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

package com.jecstar.etm.server.core.domain.converter.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class JsonConverter {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(JsonConverter.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public Map<String, Object> toMap(String json) {
        try {
            return this.objectMapper.readValue(json, HashMap.class);
        } catch (IOException e) {
            throw new EtmException(EtmException.INVALID_JSON_EXPRESSION, e);
        }
    }

    public String toString(Object object) {
        try {
            return this.objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new EtmException(EtmException.INVALID_JSON_EXPRESSION, e);
        }
    }

    public Map<String, Object> getObject(String tag, Map<String, Object> valueMap) {
        return getObject(tag, valueMap, null);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getObject(String tag, Map<String, Object> valueMap, Map<String, Object> defaultValue) {
        if (valueMap != null && valueMap.containsKey(tag)) {
            return (Map<String, Object>) valueMap.get(tag);
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public <T extends Collection<?>> T getArray(String tag, Map<String, Object> valueMap) {
        return getArray(tag, valueMap, null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Collection<?>> T getArray(String tag, Map<String, Object> valueMap, T defaultValue) {
        if (valueMap != null && valueMap.containsKey(tag)) {
            return (T) valueMap.get(tag);
        }
        return defaultValue;
    }

    public Boolean getBoolean(String tag, Map<String, Object> valueMap) {
        return getBoolean(tag, valueMap, null);
    }

    public Boolean getBoolean(String tag, Map<String, Object> valueMap, Boolean defaultValue) {
        if (valueMap != null && valueMap.get(tag) != null) {
            return (Boolean) valueMap.get(tag);
        }
        return defaultValue;
    }

    public String getString(String tag, Map<String, Object> valueMap) {
        return getString(tag, valueMap, null);
    }

    public String getString(String tag, Map<String, Object> valueMap, String defaultValue) {
        if (valueMap != null && valueMap.get(tag) != null) {
            return valueMap.get(tag).toString();
        }
        return defaultValue;
    }

    public Integer getInteger(String tag, Map<String, Object> valueMap) {
        return getInteger(tag, valueMap, null);
    }

    public Integer getInteger(String tag, Map<String, Object> valueMap, Integer defaultValue) {
        if (valueMap != null && valueMap.get(tag) != null) {
            return ((Number) valueMap.get(tag)).intValue();
        }
        return defaultValue;
    }

    public Long getLong(String tag, Map<String, Object> valueMap) {
        return getLong(tag, valueMap, null);
    }

    private Long getLong(String tag, Map<String, Object> valueMap, Long defaultValue) {
        if (valueMap != null && valueMap.get(tag) != null) {
            return ((Number) valueMap.get(tag)).longValue();
        }
        return defaultValue;
    }

    public Double getDouble(String tag, Map<String, Object> valueMap) {
        return getDouble(tag, valueMap, null);
    }

    public Double getDouble(String tag, Map<String, Object> valueMap, Double defaultValue) {
        if (valueMap != null && valueMap.get(tag) != null) {
            return ((Number) valueMap.get(tag)).doubleValue();
        }
        return defaultValue;
    }

    public Float getFloat(String tag, Map<String, Object> valueMap) {
        return getFloat(tag, valueMap, null);
    }

    public Float getFloat(String tag, Map<String, Object> valueMap, Float defaultValue) {
        if (valueMap != null && valueMap.get(tag) != null) {
            return ((Number) valueMap.get(tag)).floatValue();
        }
        return defaultValue;
    }

    public Instant getInstant(String tag, Map<String, Object> valueMap) {
        Long epochTime = getLong(tag, valueMap);
        if (epochTime == null) {
            return null;
        }
        try {
            return Instant.ofEpochMilli(epochTime);
        } catch (NumberFormatException nfe) {
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("'" + epochTime + "' could not be converted to a long. ZonedDateTime could not be determined.");
            }
            return null;
        }
    }

}
