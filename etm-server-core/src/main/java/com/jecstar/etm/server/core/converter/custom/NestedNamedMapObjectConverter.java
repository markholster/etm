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

package com.jecstar.etm.server.core.converter.custom;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.server.core.converter.CustomFieldConverter;
import com.jecstar.etm.server.core.converter.JsonEntityConverter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NestedNamedMapObjectConverter<V, T extends Map<String, V>> extends JsonEntityConverter<T> implements CustomFieldConverter<T> {

    private final JsonEntityConverter<V> entityConverter;

    @SuppressWarnings("unchecked")
    public NestedNamedMapObjectConverter(JsonEntityConverter<V> entityConverter) {
        super(f -> (T) new HashMap<String, V>());
        this.entityConverter = entityConverter;
    }

    @Override
    public void addToJsonBuffer(String jsonKey, T values, JsonBuilder builder) {
        if (values == null) {
            return;
        }
        builder.startObject(jsonKey);
        for (var entry : values.entrySet()) {
            builder.rawField(entry.getKey(), this.entityConverter.write(entry.getValue()));
        }
        builder.endObject();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setValueOnEntity(Field field, Object entity, Object jsonValue) {
        Map<String, V> results = new HashMap<>();

        Map<String, Object> rootObject = (Map<String, Object>) jsonValue;
        Set<String> names = rootObject.keySet();
        for (String name : names) {
            Map<String, Object> values = jsonConverter.getObject(name, rootObject);
            results.put(name, this.entityConverter.read(values));
        }
        try {
            field.set(entity, results);
        } catch (IllegalAccessException e) {
            log.logErrorMessage("Unable to set value '" + jsonValue + "'.", e);
        }
    }
}
