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

package com.jecstar.etm.server.core.domain.audit.converter;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.converter.CustomFieldConverter;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CorrelatedEventsFieldsConverter implements CustomFieldConverter<Set<String>> {

    @Override
    public void addToJsonBuffer(String jsonKey, Set<String> value, JsonBuilder builder) {
        if (value != null && value.size() > 0) {
            builder.field(jsonKey, value);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setValueOnEntity(Field field, Object entity, Object jsonValue) {
        if (jsonValue == null) {
            return;
        }
        Set<String> result = new HashSet<>();
        List<String> valueList = (List<String>) jsonValue;
        if (valueList != null) {
            result.addAll(valueList);
        }
        if (result.size() > 0) {
            try {
                Set<String> currentValue = (Set<String>) field.get(entity);
                currentValue.addAll(result);
            } catch (IllegalAccessException e) {
                throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
            }
        }
    }
}
