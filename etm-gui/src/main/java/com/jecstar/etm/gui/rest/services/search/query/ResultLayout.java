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

import com.jecstar.etm.domain.writer.TelemetryEventTags;
import com.jecstar.etm.domain.writer.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.gui.rest.export.FieldLayout;
import com.jecstar.etm.gui.rest.export.FieldType;
import com.jecstar.etm.gui.rest.export.MultiSelect;
import com.jecstar.etm.gui.rest.services.search.query.converter.FieldListConverter;
import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.converter.custom.EnumConverter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ResultLayout {

    private final TelemetryEventTags tags = new TelemetryEventTagsJsonImpl();

    public enum SortOrder {
        ASC, DESC;

        public static SortOrder safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return SortOrder.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

    }

    @JsonField("current_ix")
    private Integer startIndex = 0;
    @JsonField("results_per_page")
    private int maxResults = 50;
    @JsonField("current_query")
    private String query;
    @JsonField(value = "fields", converterClass = FieldListConverter.class)
    private List<Field> fields;
    @JsonField("sort_field")
    private String sortField;
    @JsonField(value = "sort_order", converterClass = EnumConverter.class)
    private SortOrder sortOrder;
    @JsonField("timestamp")
    private Instant timestamp;

    public ResultLayout() {
        this.fields = new ArrayList<>();
    }

    public Integer getStartIndex() {
        return this.startIndex;
    }

    public ResultLayout setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
        return this;
    }

    public int getMaxResults() {
        return this.maxResults;
    }

    public ResultLayout setMaxResults(int maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    public String getQuery() {
        return this.query;
    }

    public ResultLayout setQuery(String query) {
        this.query = query;
        return this;
    }

    public List<String> getFieldNames() {
        return this.fields.stream().map(Field::getField).collect(Collectors.toList());
    }

    public List<Field> getFields() {
        return Collections.unmodifiableList(this.fields);
    }

    public void addField(Field field) {
        this.fields.add(field);
    }

    public String getSortField() {
        return this.sortField;
    }

    public ResultLayout setSortField(String sortField) {
        this.sortField = sortField;
        return this;
    }

    public SortOrder getSortOrder() {
        return this.sortOrder;
    }

    public ResultLayout setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    public Instant getTimestamp() {
        return this.timestamp;
    }

    public ResultLayout setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public List<FieldLayout> toFieldLayouts() {
        List<FieldLayout> fieldLayouts = new ArrayList<>();
        for (Field field : this.fields) {
            fieldLayouts.add(
                    new FieldLayout(
                            field.getName(),
                            field.getField(),
                            FieldType.fromJsonValue(field.getFormat().name()),
                            MultiSelect.valueOf(field.getArraySelector().name())
                    )
            );
        }
        return fieldLayouts;
    }
}
