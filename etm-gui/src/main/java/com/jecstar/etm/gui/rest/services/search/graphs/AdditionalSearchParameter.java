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

package com.jecstar.etm.gui.rest.services.search.graphs;

import com.jecstar.etm.domain.writer.json.JsonBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AdditionalSearchParameter implements Vertex {

    private final int id;
    private String field;
    private String type;
    private Object value;
    private List<String> distinctValues = new ArrayList<>();

    public AdditionalSearchParameter(int index) {
        this.id = index;
    }

    @Override
    public String getVertexId() {
        return "" + this.id;
    }

    @Override
    public Vertex getParent() {
        return null;
    }

    @Override
    public void toJson(JsonBuilder builder) {
        return;
    }

    public int getId() {
        return this.id;
    }

    public AdditionalSearchParameter setField(String field) {
        this.field = field;
        return this;
    }

    public String getField() {
        return this.field;
    }

    public AdditionalSearchParameter setType(String type) {
        this.type = type;
        return this;
    }

    public String getType() {
        return this.type;
    }

    public AdditionalSearchParameter setValue(Object value) {
        this.value = value;
        return this;
    }

    public Object getValue() {
        return this.value;
    }

    public boolean hasValue() {
        return getValue() != null;
    }

    public void addDistinctValues(List<String> distinctValues) {
        this.distinctValues.addAll(distinctValues);
        this.distinctValues.sort(String.CASE_INSENSITIVE_ORDER);
    }

    public List<String> getDistinctValues() {
        return this.distinctValues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdditionalSearchParameter that = (AdditionalSearchParameter) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
