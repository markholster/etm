package com.jecstar.etm.gui.rest.services.settings;

import java.util.Map;

import com.jecstar.etm.server.core.domain.converter.AuditLogTags;
import com.jecstar.etm.server.core.domain.converter.json.AuditLogTagsJsonImpl;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

public class AuditSearchRequestParameters {
	
	private static final Integer DEFAULT_START_IX = 0;
	private static final Integer DEFAULT_MAX_SIZE = 50;
	
	private final JsonConverter converter = new JsonConverter();
	private final AuditLogTags tags = new AuditLogTagsJsonImpl();
	
	private String queryString;
	private Integer startIndex;
	private Integer maxResults;
	private String sortField;
	private String sortOrder;
	private Long notAfterTimestamp;

	public AuditSearchRequestParameters(String query) {
		this.queryString = query;
		this.startIndex = 0;
		this.maxResults = 50;
		this.sortField = this.tags.getHandlingTimeTag();
		this.sortOrder = "desc";
	}
	
	
	AuditSearchRequestParameters(Map<String, Object> requestValues) {
		this.queryString = this.converter.getString("query", requestValues);
		this.startIndex = this.converter.getInteger("start_ix", requestValues, DEFAULT_START_IX);
		this.maxResults = this.converter.getInteger("max_results", requestValues, DEFAULT_MAX_SIZE);
		this.sortField = this.converter.getString("sort_field", requestValues);
		this.sortOrder = this.converter.getString("sort_order", requestValues);
		this.notAfterTimestamp = this.converter.getLong("timestamp", requestValues);
		if (this.notAfterTimestamp == null) {
			this.notAfterTimestamp = System.currentTimeMillis();
		}
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
	
	public Long getNotAfterTimestamp() {
		return this.notAfterTimestamp;
	}
	
	
}
