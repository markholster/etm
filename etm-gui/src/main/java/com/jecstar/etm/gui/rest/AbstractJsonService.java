package com.jecstar.etm.gui.rest;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;

import com.jecstar.etm.core.domain.EtmPrincipal;
import com.jecstar.etm.core.domain.converter.json.AbstractJsonConverter;

public class AbstractJsonService extends AbstractJsonConverter {

    @Context
    protected SecurityContext securityContext;

    
    protected EtmPrincipal getEtmPrincipal() {
    	return (EtmPrincipal)this.securityContext.getUserPrincipal();
    }
    
    protected QueryBuilder getEtmPrincipalFilterQuery() {
    	String filterQuery = getEtmPrincipal().getFilterQuery();
    	if (filterQuery == null) {
    		return null;
    	}
    	return new QueryStringQueryBuilder(filterQuery)
				.allowLeadingWildcard(true)
				.analyzeWildcard(true)
				.locale(getEtmPrincipal().getLocale())
				.lowercaseExpandedTerms(false)
				.timeZone(getEtmPrincipal().getTimeZone().getID());
    }
    
    protected QueryBuilder addEtmPrincipalFilterQuery(QueryBuilder queryBuilder) {
    	QueryBuilder etmPrincipalFilterQuery = getEtmPrincipalFilterQuery();
    	if (etmPrincipalFilterQuery == null) {
    		return queryBuilder;
    	}
    	return new BoolQueryBuilder().must(queryBuilder).filter(etmPrincipalFilterQuery);
    }
}
