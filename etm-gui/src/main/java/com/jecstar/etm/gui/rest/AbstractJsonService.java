package com.jecstar.etm.gui.rest;

import java.text.DateFormatSymbols;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

	protected static final String KEYWORD_SUFFIX = ".keyword";
	
    @Context
    protected SecurityContext securityContext;

    
    protected EtmPrincipal getEtmPrincipal() {
    	return (EtmPrincipal)this.securityContext.getUserPrincipal();
    }
    
    private List<FilterQuery> getEtmPrincipalFilterQueries() {
    	List<FilterQuery> result = new ArrayList<>();
    	String filterQuery = getEtmPrincipal().getFilterQuery();
    	if (filterQuery != null && filterQuery.trim().length() > 0) {
    		result.add(new FilterQuery(getEtmPrincipal().getFilterQueryOccurrence(), 
    			new QueryStringQueryBuilder(filterQuery.trim())
    				.allowLeadingWildcard(true)
    				.analyzeWildcard(true)
    				.timeZone(getEtmPrincipal().getTimeZone().getID())));
    	}
    	Set<EtmGroup> groups = getEtmPrincipal().getGroups();
    	for (EtmGroup group : groups) {
    		if (group.getFilterQuery() != null && group.getFilterQuery().trim().length() > 0) {
        		result.add(new FilterQuery(group.getFilterQueryOccurrence(),
        			new QueryStringQueryBuilder(group.getFilterQuery().trim())
        				.allowLeadingWildcard(true)
        				.analyzeWildcard(true)
        				.timeZone(getEtmPrincipal().getTimeZone().getID())));
    		}
    	}
    	return result;
    }
    
    protected boolean hasFilterQueries() {
    	String filterQuery = getEtmPrincipal().getFilterQuery();
    	if (filterQuery != null && filterQuery.trim().length() > 0) {
    		return true;
    	}
    	Set<EtmGroup> groups = getEtmPrincipal().getGroups();
    	for (EtmGroup group : groups) {
    		if (group.getFilterQuery() != null && group.getFilterQuery().trim().length() > 0) {
    			return true;
    		}
    	}
    	return false;
    }
    
    protected QueryBuilder addEtmPrincipalFilterQuery(QueryBuilder queryBuilder) {
    	List<FilterQuery> filterQueries = getEtmPrincipalFilterQueries();
    	if (filterQueries == null || filterQueries.isEmpty()) {
    		return queryBuilder;
    	}
    	BoolQueryBuilder filteredQuery = new BoolQueryBuilder().must(queryBuilder);
    	for (FilterQuery filterQuery : filterQueries) {
    		if (QueryOccurrence.MUST.equals(filterQuery.getQueryOccurrence())) {
    			filteredQuery.filter(filterQuery.getQuery());
    		} else if (QueryOccurrence.MUST_NOT.equals(filterQuery.getQueryOccurrence())) {
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
    
    protected String getD3Formatter() {
    	EtmPrincipal etmPrincipal = getEtmPrincipal();
    	NumberFormat numberFormat = NumberFormat.getInstance(etmPrincipal.getLocale());
    	DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(etmPrincipal.getLocale());
    	DateFormatSymbols dateFormatSymbols = DateFormatSymbols.getInstance(etmPrincipal.getLocale());
    	StringBuilder result = new StringBuilder();
    	result.append("{");
    	addStringElementToJsonBuffer("decimal", "" + decimalFormatSymbols.getDecimalSeparator(), result, true);
    	addStringElementToJsonBuffer("thousands", "" + decimalFormatSymbols.getGroupingSeparator(), result, false);
    	result.append(",\"grouping\": [").append(numberFormat.getMaximumFractionDigits()).append("]");
    	result.append(",\"currency\": [\"").append(numberFormat.getCurrency().getSymbol()).append("\", \"\"]");
    	// TODO dateTime and time should be dynamic and based on the locale.
    	addStringElementToJsonBuffer("dateTime", "%a %b %e %X %Y", result, false);
    	addStringElementToJsonBuffer("date", "%m/%d/%Y", result, false);
    	addStringElementToJsonBuffer("time", "%H:%M:%S", result, false);
    	result.append(",\"periods\": [\"").append(Arrays.stream(dateFormatSymbols.getAmPmStrings()).collect(Collectors.joining("\",\""))).append("\"]");
    	result.append(",\"days\": [\"").append(Arrays.stream(dateFormatSymbols.getWeekdays()).collect(Collectors.joining("\",\""))).append("\"]");
    	result.append(",\"shortDays\": [\"").append(Arrays.stream(dateFormatSymbols.getShortWeekdays()).collect(Collectors.joining("\",\""))).append("\"]");
    	result.append(",\"months\": [\"").append(Arrays.stream(dateFormatSymbols.getMonths()).collect(Collectors.joining("\",\""))).append("\"]");
    	result.append(",\"shortMonths\": [\"").append(Arrays.stream(dateFormatSymbols.getShortMonths()).collect(Collectors.joining("\",\""))).append("\"]");
    	result.append("}");
    	return result.toString();
    }

}
