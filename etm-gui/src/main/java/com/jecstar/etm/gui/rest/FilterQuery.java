package com.jecstar.etm.gui.rest;

import com.jecstar.etm.server.core.domain.QueryOccurrence;
import org.elasticsearch.index.query.QueryBuilder;

class FilterQuery {

	private final QueryOccurrence queryOccurrence;
	
	private final QueryBuilder query;
	
	FilterQuery(QueryOccurrence queryOccurrence, QueryBuilder query) {
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
