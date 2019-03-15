package com.jecstar.etm.gui.rest.services.search;

import com.jecstar.etm.domain.writer.TelemetryEventTags;
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
import java.util.stream.Collectors;

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
    private final List<String> types;
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
        this.types = new ArrayList<>(5);
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
        this.types = this.converter.getArray("types", requestValues);
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
        StringBuilder result = new StringBuilder();
        result.append("{\"types\":[");
        if (this.types != null) {
            result.append(this.types.stream().map(e -> this.converter.escapeToJson(e, true)).collect(Collectors.joining(",")));
        }
        result.append("]");
        this.converter.addStringElementToJsonBuffer("name", name, result, false);
        this.converter.addStringElementToJsonBuffer("sort_field", this.sortField, result, false);
        this.converter.addStringElementToJsonBuffer("sort_order", this.sortOrder, result, false);
        this.converter.addIntegerElementToJsonBuffer("start_ix", this.getStartIndex(), result, false);
        this.converter.addStringElementToJsonBuffer("start_time", this.getStartTime(), result, false);
        this.converter.addStringElementToJsonBuffer("end_time", this.getEndTime(), result, false);
        this.converter.addStringElementToJsonBuffer("time_filter_field", this.getTimeFilterField(), result, false);
        this.converter.addIntegerElementToJsonBuffer("results_per_page", this.getMaxResults(), result, false);
        this.converter.addStringElementToJsonBuffer("query", this.queryString, result, false);
        result.append(",\"fields\": [");
        if (this.fieldsLayout != null) {
            boolean first = true;
            for (Map<String, Object> field : this.fieldsLayout) {
                if (!first) {
                    result.append(",");
                }
                result.append("{");
                this.converter.addStringElementToJsonBuffer("name", this.converter.getString("name", field), result, true);
                this.converter.addStringElementToJsonBuffer("field", this.converter.getString("field", field), result, false);
                this.converter.addStringElementToJsonBuffer("format", this.converter.getString("format", field), result, false);
                this.converter.addStringElementToJsonBuffer("array", this.converter.getString("array", field), result, false);
                this.converter.addBooleanElementToJsonBuffer("link", this.converter.getBoolean("link", field), result, false);
                result.append("}");
                first = false;
            }
        }
        result.append("]}");
        return result.toString();
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

    public List<String> getTypes() {
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
