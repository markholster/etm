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
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Function;

public abstract class NestedObjectConverter<T> extends JsonEntityConverter<T> implements CustomFieldConverter<T> {

    private final LogWrapper log = LogFactory.getLogger(getClass());

    public NestedObjectConverter(Function<Map<String, Object>, T> objectFactory) {
        super(objectFactory);
    }

    @Override
    public void addToJsonBuffer(String jsonKey, T value, JsonBuilder builder) {
        if (value == null) {
            return;
        }
        builder.rawField(jsonKey, write(value));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setValueOnEntity(Field field, Object entity, Object jsonValue) {
        T value = read((Map<String, Object>) jsonValue);
        try {
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            log.logErrorMessage("Unable to set value '" + jsonValue + "'.", e);
        }
    }
}
