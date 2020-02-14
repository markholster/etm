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

package com.jecstar.etm.server.core.converter;

import com.jecstar.etm.domain.writer.json.JsonBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonField {

    String value();

    boolean writeWhenNull() default false;

    Class<? extends CustomFieldConverter> converterClass() default DefaultCustomConverter.class;

    final class DefaultCustomConverter implements CustomFieldConverter {
        @Override
        public void addToJsonBuffer(String jsonKey, Object entity, JsonBuilder builder) {
        }

        @Override
        public void setValueOnEntity(Field field, Object entity, Object value) {
        }
    }
}
