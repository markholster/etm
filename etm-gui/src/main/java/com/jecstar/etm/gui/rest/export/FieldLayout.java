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

package com.jecstar.etm.gui.rest.export;

public class FieldLayout {

    private String name;

    private String field;

    private FieldType type;

    private MultiSelect multiSelect;

    public FieldLayout(String name, String field, FieldType type, MultiSelect multiSelect) {
        this.name = name;
        this.field = field;
        this.type = type;
        this.multiSelect = multiSelect;
    }

    public String getName() {
        return this.name;
    }

    public String getField() {
        return this.field;
    }

    public FieldType getType() {
        return this.type;
    }

    public MultiSelect getMultiSelect() {
        return this.multiSelect;
    }
}
