package com.jecstar.etm.gui.rest.services.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.jecstar.etm.domain.writer.TelemetryEventTags;
import com.jecstar.etm.domain.writer.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

class SearchRequestParameters {
	
	private static final Integer DEFAULT_START_IX = 0;
	private static final Integer DEFAULT_MAX_SIZE = 50;
	
	private final JsonConverter converter = new JsonConverter();

	private final String queryString;
	private final Integer startIndex;
	private final Integer maxResults;
	private final String sortField;
	private final String sortOrder;
	private Long notAfterTimestamp;
	private final List<String> types;
	private List<String> fields;
	private final List<Map<String, Object>> fieldsLayout;

	public SearchRequestParameters(String query) {
		this.queryString = query;
		this.startIndex = 0;
		this.maxResults = 50;
		TelemetryEventTags tags = new TelemetryEventTagsJsonImpl();
		this.sortField = tags.getEndpointsTag() + "." + tags.getWritingEndpointHandlerTag() + "." + tags.getEndpointHandlerHandlingTimeTag();
		this.sortOrder = "desc";
		this.types = new ArrayList<>(5);
		this.types.add(ElasticsearchLayout.ETM_EVENT_INDEX_TYPE_BUSINESS);
		this.types.add(ElasticsearchLayout.ETM_EVENT_INDEX_TYPE_HTTP);
		this.types.add(ElasticsearchLayout.ETM_EVENT_INDEX_TYPE_LOG);
		this.types.add(ElasticsearchLayout.ETM_EVENT_INDEX_TYPE_MESSAGING);
		this.types.add(ElasticsearchLayout.ETM_EVENT_INDEX_TYPE_SQL);
		this.fields = new ArrayList<>(2);
		this.fields.add(tags.getEndpointsTag() + "." + tags.getWritingEndpointHandlerTag() + "." + tags.getEndpointHandlerHandlingTimeTag());
		this.fields.add(tags.getNameTag());
		this.fieldsLayout = new ArrayList<>();
		Map<String, Object> layout = new HashMap<>();
		layout.put("array", "lowest");
		layout.put("field", tags.getEndpointsTag() + "." + tags.getWritingEndpointHandlerTag() + "." + tags.getEndpointHandlerHandlingTimeTag());
		layout.put("format", "isotimestamp");
		layout.put("link", true);
		layout.put("name", "Timestamp");
		this.fieldsLayout.add(layout);
		layout = new HashMap<>();
		layout.put("array", "first");
		layout.put("field", tags.getNameTag());
		layout.put("format", "plain");
		layout.put("link", false);
		layout.put("name", "Name");
		this.fieldsLayout.add(layout);
	}
	
	
	SearchRequestParameters(Map<String, Object> requestValues) {
		this.queryString = this.converter.getString("query", requestValues);
		this.startIndex = this.converter.getInteger("start_ix", requestValues, DEFAULT_START_IX);
		this.maxResults = this.converter.getInteger("max_results", requestValues, DEFAULT_MAX_SIZE);
		this.sortField = this.converter.getString("sort_field", requestValues);
		this.sortOrder = this.converter.getString("sort_order", requestValues);
		this.types = this.converter.getArray("types", requestValues);
		this.fields = this.converter.getArray("fields", requestValues);
		if (this.fields == null) {
			this.fields = new ArrayList<>(2);
		}
		this.notAfterTimestamp = this.converter.getLong("timestamp", requestValues);
		if (this.notAfterTimestamp == null) {
			this.notAfterTimestamp = System.currentTimeMillis();
		}
		this.fieldsLayout = this.converter.getArray("fieldsLayout", requestValues);
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
		return this.fields;
	}
	
	public List<Map<String, Object>> getFieldsLayout() {
		return this.fieldsLayout;
	}
	
	public Long getNotAfterTimestamp() {
		return this.notAfterTimestamp;
	}
	
	
}
