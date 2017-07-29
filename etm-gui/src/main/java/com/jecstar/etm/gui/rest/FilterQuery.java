package com.jecstar.etm.gui.rest;

import org.elasticsearch.index.query.QueryBuilder;

import com.jecstar.etm.server.core.domain.QueryOccurrence;

class FilterQuery {

	private final QueryOccurrence queryOccurrence;
	
	private final QueryBuilder query;
	
	public FilterQuery(QueryOccurrence queryOccurrence, QueryBuilder query) {
		this.queryOccurrence = queryOccurrence;
		this.query = query;
	}
	
	public QueryOccurrence getQueryOccurrence() {
		return this.queryOccurrence;
	}
	
	public QueryBuilder getQuery() {
		return this.query;
	}
	
}
