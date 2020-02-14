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

package com.jecstar.etm.gui.rest.services.search;

import com.jecstar.etm.domain.writer.TelemetryEventTags;
import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.domain.writer.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.gui.rest.export.FieldLayout;
import com.jecstar.etm.gui.rest.export.FieldType;
import com.jecstar.etm.gui.rest.export.MultiSelect;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.util.DateUtils;

import java.time.Instant;
import java.util.*;

class SearchRequestParameters {

    private static final Integer DEFAULT_START_IX = 0;
    private static final Integer DEFAULT_MAX_SIZE = 50;

    private final JsonConverter converter = new JsonConverter();
    private final TelemetryEventTags tags = new TelemetryEventTagsJsonImpl();

    private final String queryString;
    private final Integer startIndex;
    private final Integer maxResults;
    private final String sortField;
    private final String sortOrder;
    private Long notAfterTimestamp;
    private final Set<String> types;
    private List<String> fields;
    private final String startTime;
    private final String endTime;
    private final String timeFilterField;
    private final List<Map<String, Object>> fieldsLayout;
    private final EtmPrincipal etmPrincipal;

    SearchRequestParameters(String query, String startTime, String endTime, EtmPrincipal etmPrincipal) {
        this.etmPrincipal = etmPrincipal;
        this.queryString = query;
        this.startIndex = 0;
        this.maxResults = 50;
        TelemetryEventTags tags = new TelemetryEventTagsJsonImpl();
        this.sortField = tags.getTimestampTag();
        this.sortOrder = "desc";
        this.types = new HashSet<>(5);
        this.types.add(ElasticsearchLayout.EVENT_OBJECT_TYPE_BUSINESS);
        this.types.add(ElasticsearchLayout.EVENT_OBJECT_TYPE_HTTP);
        this.types.add(ElasticsearchLayout.EVENT_OBJECT_TYPE_LOG);
        this.types.add(ElasticsearchLayout.EVENT_OBJECT_TYPE_MESSAGING);
        this.types.add(ElasticsearchLayout.EVENT_OBJECT_TYPE_SQL);
        this.fields = new ArrayList<>(2);
        addField(tags.getTimestampTag());
        addField(tags.getNameTag());
        this.fieldsLayout = new ArrayList<>();
        Map<String, Object> layout = new HashMap<>();
        layout.put("array", "lowest");
        layout.put("field", tags.getTimestampTag());
        layout.put("format", "isotimestamp");
        layout.put("link", true);
        layout.put("name", "Timestamp");
        addFieldLayout(layout);
        layout = new HashMap<>();
        layout.put("array", "first");
        layout.put("field", tags.getNameTag());
        layout.put("format", "plain");
        layout.put("link", false);
        layout.put("name", "Name");
        addFieldLayout(layout);
        Instant instant = DateUtils.parseDateString(startTime, etmPrincipal.getTimeZone().toZoneId(), true);
        if (instant != null) {
            this.startTime = "" + instant.toEpochMilli();
        } else {
            this.startTime = startTime;
        }
        instant = DateUtils.parseDateString(endTime, etmPrincipal.getTimeZone().toZoneId(), false);
        if (instant != null) {
            this.endTime = "" + instant.toEpochMilli();
        } else {
            this.endTime = endTime;
        }
        this.timeFilterField = "timestamp";
    }


    SearchRequestParameters(Map<String, Object> requestValues, EtmPrincipal etmPrincipal) {
        this.etmPrincipal = etmPrincipal;
        this.queryString = this.converter.getString("query", requestValues);
        this.startIndex = this.converter.getInteger("start_ix", requestValues, DEFAULT_START_IX);
        this.maxResults = this.converter.getInteger("max_results", requestValues, DEFAULT_MAX_SIZE);
        this.sortField = this.converter.getString("sort_field", requestValues);
        this.sortOrder = this.converter.getString("sort_order", requestValues);
        this.types = new HashSet<>(this.converter.getArray("types", requestValues));
        this.fields = new ArrayList<>(2);
        this.converter.getArray("fields", requestValues, new ArrayList<String>()).forEach(c -> addField(c));
        this.notAfterTimestamp = this.converter.getLong("timestamp", requestValues);
        if (this.notAfterTimestamp == null) {
            this.notAfterTimestamp = System.currentTimeMillis();
        }
        this.fieldsLayout = new ArrayList<>();
        this.converter.getArray("fieldsLayout", requestValues, new ArrayList<Map<String, Object>>()).forEach(c -> addFieldLayout(c));
        Instant instant = DateUtils.parseDateString(this.converter.getString("start_time", requestValues), etmPrincipal.getTimeZone().toZoneId(), true);
        if (instant != null) {
            this.startTime = "" + instant.toEpochMilli();
        } else {
            this.startTime = this.converter.getString("start_time", requestValues);
        }
        instant = DateUtils.parseDateString(this.converter.getString("end_time", requestValues), etmPrincipal.getTimeZone().toZoneId(), false);
        if (instant != null) {
            this.endTime = "" + instant.toEpochMilli();
        } else {
            this.endTime = this.converter.getString("end_time", requestValues);
        }
        this.timeFilterField = this.converter.getString("time_filter_field", requestValues);
    }

    public String toJsonSearchTemplate(String name) {
        var builder = new JsonBuilder();
        builder.startObject();
        builder.field("types", this.types);
        builder.field("name", name);
        builder.field("sort_field", this.sortField);
        builder.field("sort_order", this.sortOrder);
        builder.field("start_ix", this.getStartIndex());
        builder.field("start_time", this.getStartTime());
        builder.field("end_time", this.getEndTime());
        builder.field("time_filter_field", this.getTimeFilterField());
        builder.field("results_per_page", this.getMaxResults());
        builder.field("query", this.queryString);
        builder.startArray("fields");
        if (this.fieldsLayout != null) {
            for (Map<String, Object> field : this.fieldsLayout) {
                builder.startObject();
                builder.field("name", this.converter.getString("name", field));
                builder.field("field", this.converter.getString("field", field));
                builder.field("format", this.converter.getString("format", field));
                builder.field("array", this.converter.getString("array", field));
                builder.field("link", this.converter.getBoolean("link", field));
                builder.endObject();
            }
        }
        builder.endArray().endObject();
        return builder.build();
    }

    public String getQueryString() {
        return this.queryString;
    }

    public Integer getStartIndex() {
        return this.startIndex;
    }

    public Integer getMaxResults() {
        return this.maxResults;
    }

    public String getSortField() {
        return this.sortField;
    }

    public String getSortOrder() {
        return this.sortOrder;
    }

    public Set<String> getTypes() {
        return this.types;
    }

    public List<String> getFields() {
        return Collections.unmodifiableList(this.fields);
    }

    public void addField(String field) {
        if (this.tags.getPayloadTag().equals(field) && !this.etmPrincipal.maySeeEventPayload()) {
            return;
        }
        this.fields.add(field);
    }

    public boolean hasFields() {
        return !this.fields.isEmpty();
    }

    public List<Map<String, Object>> getFieldsLayout() {
        return Collections.unmodifiableList(this.fieldsLayout);
    }

    public void addFieldLayout(Map<String, Object> values) {
        if (this.tags.getPayloadTag().equals(this.converter.getString("field", values)) && !this.etmPrincipal.maySeeEventPayload()) {
            return;
        }
        this.fieldsLayout.add(values);
    }

    public List<FieldLayout> toFieldLayouts() {
        List<FieldLayout> fieldLayouts = new ArrayList<>();
        for (int i = 0; i < this.fieldsLayout.size(); i++) {
            Map<String, Object> values = this.fieldsLayout.get(i);
            fieldLayouts.add(
                    new FieldLayout(
                            this.converter.getString("name", values),
                            this.converter.getString("field", values),
                            FieldType.fromJsonValue(this.converter.getString("format", values)),
                            MultiSelect.valueOf(this.converter.getString("array", values).toUpperCase())
                    )
            );
        }
        return fieldLayouts;
    }

    public String getStartTime() {
        return this.startTime;
    }

    public String getEndTime() {
        return this.endTime;
    }

    public String getTimeFilterField() {
        return this.timeFilterField != null && this.timeFilterField.length() > 0 ? this.timeFilterField : "timestamp";
    }

    public Long getNotAfterTimestamp() {
        return this.notAfterTimestamp;
    }

}
