package com.jecstar.etm.gui.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;

import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.EtmGroup;
import com.jecstar.etm.server.core.domain.EtmPrincipal;
import com.jecstar.etm.server.core.domain.QueryOccurrence;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

public class AbstractJsonService extends JsonConverter {

    @Context
    protected SecurityContext securityContext;

    
    protected EtmPrincipal getEtmPrincipal() {
    	return (EtmPrincipal)this.securityContext.getUserPrincipal();
    }
    
    protected List<FilterQuery> getEtmPrincipalFilterQueries() {
    	List<FilterQuery> result = new ArrayList<>();
    	String filterQuery = getEtmPrincipal().getFilterQuery();
    	if (filterQuery != null && filterQuery.trim().length() > 0) {
    		result.add(new FilterQuery(getEtmPrincipal().getFilterQueryOccurrence(), 
    			new QueryStringQueryBuilder(filterQuery.trim())
    				.allowLeadingWildcard(true)
    				.analyzeWildcard(true)
    				.locale(getEtmPrincipal().getLocale())
    				.lowercaseExpandedTerms(false)
    				.timeZone(getEtmPrincipal().getTimeZone().getID())));
    	}
    	Set<EtmGroup> groups = getEtmPrincipal().getGroups();
    	for (EtmGroup group : groups) {
    		if (group.getFilterQuery() != null && group.getFilterQuery().trim().length() > 0) {
        		result.add(new FilterQuery(group.getFilterQueryOccurrence(),
        			new QueryStringQueryBuilder(group.getFilterQuery().trim())
        				.allowLeadingWildcard(true)
        				.analyzeWildcard(true)
        				.locale(getEtmPrincipal().getLocale())
        				.lowercaseExpandedTerms(false)
        				.timeZone(getEtmPrincipal().getTimeZone().getID())));
    		}
    	}
    	return result;
    }
    
    protected QueryBuilder addEtmPrincipalFilterQuery(QueryBuilder queryBuilder) {
    	List<FilterQuery> filterQueries = getEtmPrincipalFilterQueries();
    	if (filterQueries == null || filterQueries.isEmpty()) {
    		return queryBuilder;
    	}
    	BoolQueryBuilder filteredQuery = new BoolQueryBuilder().must(queryBuilder);
    	for (FilterQuery filterQuery : filterQueries) {
    		if (QueryOccurrence.MUST.equals(filterQuery.getQueryOccurence())) {
    			filteredQuery.filter(filterQuery.getQuery());
    		} else if (QueryOccurrence.MUST_NOT.equals(filterQuery.getQueryOccurence())) {
    			filteredQuery.mustNot(filterQuery.getQuery());
    		}
    	}
    	return filteredQuery;
    }
    
    protected ActiveShardCount getActiveShardCount(EtmConfiguration etmConfiguration) {
    	if (-1 == etmConfiguration.getWaitForActiveShards()) {
    		return ActiveShardCount.ALL;
    	} else if (0 == etmConfiguration.getWaitForActiveShards()) {
    		return ActiveShardCount.NONE;
    	}
    	return ActiveShardCount.from(etmConfiguration.getWaitForActiveShards());
    }
}
