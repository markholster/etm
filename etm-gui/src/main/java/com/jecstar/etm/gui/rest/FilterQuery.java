package com.jecstar.etm.gui.rest;

import org.elasticsearch.index.query.QueryBuilder;

import com.jecstar.etm.server.core.domain.QueryOccurrence;

public class FilterQuery {

	private QueryOccurrence queryOccurrence;
	
	private QueryBuilder query;
	
	public FilterQuery(QueryOccurrence queryOccurrence, QueryBuilder query) {
		this.queryOccurrence = queryOccurrence;
		this.query = query;
	}
	
	public QueryOccurrence getQueryOccurence() {
		return this.queryOccurrence;
	}
	
	public QueryBuilder getQuery() {
		return this.query;
	}
	
}
