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

package com.jecstar.etm.gui.rest.services.search.query;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.converter.custom.EnumConverter;

public class AdditionalQueryParameter {

    public enum FieldType {
        FREE_FORMAT, RANGE_START, RANGE_END, SELECT, SELECT_MULTI;

        public static FieldType safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return FieldType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    @JsonField("name")
    private String name;
    @JsonField("field")
    private String field;
    @JsonField(value = "type", converterClass = EnumConverter.class)
    private FieldType fieldType;
    @JsonField("value")
    private Object value;
    @JsonField("default_value")
    private Object defaultValue;

    public String getName() {
        return this.name;
    }

    public AdditionalQueryParameter setName(String name) {
        this.name = name;
        return this;
    }

    public String getField() {
        return this.field;
    }

    public AdditionalQueryParameter setField(String field) {
        this.field = field;
        return this;
    }

    public FieldType getFieldType() {
        return this.fieldType;
    }

    public AdditionalQueryParameter setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
        return this;
    }

    public Object getValue() {
        return this.value;
    }

    public AdditionalQueryParameter setValue(Object value) {
        this.value = value;
        return this;
    }

    public Object getDefaultValue() {
        return this.defaultValue;
    }

    public AdditionalQueryParameter setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }
}
