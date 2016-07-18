package com.jecstar.etm.gui.rest.services.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

class SearchRequestParameters {
	
	private final JsonConverter converter = new JsonConverter();
	private String queryString;
	private Integer startIndex;
	private Integer maxResults;
	private String sortField;
	private String sortOrder;
	private List<String> types;
	private List<String> fields;
	private List<Map<String, Object>> fieldsLayout;

	SearchRequestParameters(Map<String, Object> requestValues) {
		this.queryString = this.converter.getString("query", requestValues);
		this.startIndex = this.converter.getInteger("start_ix", requestValues, new Integer(0));
		this.maxResults = this.converter.getInteger("max_results", requestValues, new Integer(50));
		if (this.maxResults > 500) {
			this.maxResults = 500;
		}
		this.sortField = this.converter.getString("sort_field", requestValues);
		this.sortOrder = this.converter.getString("sort_order", requestValues);
		this.types = this.converter.getArray("types", requestValues);
		this.fields = this.converter.getArray("fields", requestValues);
		if (this.fields == null) {
			this.fields = new ArrayList<String>(2);
		}
		this.fieldsLayout = this.converter.getArray("fieldsLayout", requestValues);
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
		return fieldsLayout;
	}
	
	
}
