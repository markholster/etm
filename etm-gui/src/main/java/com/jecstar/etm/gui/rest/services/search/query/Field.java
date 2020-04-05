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

public class Field {

    public enum Format {
        PLAIN, ISOTIMESTAMP, ISOUTCTIMESTAMP;

        public static Format safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return Format.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public enum ArraySelector {
        LOWEST, HIGHEST, FIRST, LAST, ALL;

        public static ArraySelector safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return ArraySelector.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    @JsonField("name")
    private String name;
    @JsonField("field")
    private String field;
    @JsonField(value = "format", converterClass = EnumConverter.class)
    private Format format;
    @JsonField(value = "array", converterClass = EnumConverter.class)
    private ArraySelector arraySelector;
    @JsonField("link")
    private boolean link;

    public String getName() {
        return this.name;
    }

    public Field setName(String name) {
        this.name = name;
        return this;
    }

    public String getField() {
        return this.field;
    }

    public Field setField(String field) {
        this.field = field;
        return this;
    }

    public Format getFormat() {
        return this.format;
    }

    public Field setFormat(Format format) {
        this.format = format;
        return this;
    }

    public ArraySelector getArraySelector() {
        return this.arraySelector;
    }

    public Field setArraySelector(ArraySelector arraySelector) {
        this.arraySelector = arraySelector;
        return this;
    }

    public boolean isLink() {
        return this.link;
    }

    public Field setLink(boolean link) {
        this.link = link;
        return this;
    }
}
