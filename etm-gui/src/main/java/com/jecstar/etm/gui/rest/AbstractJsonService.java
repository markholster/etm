package com.jecstar.etm.gui.rest;

import com.jecstar.etm.server.core.domain.QueryOccurrence;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.text.DateFormatSymbols;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

public class AbstractJsonService extends JsonConverter {

    public static final String KEYWORD_SUFFIX = ".keyword";

    @Context
    protected SecurityContext securityContext;


    protected EtmPrincipal getEtmPrincipal() {
        return (EtmPrincipal) this.securityContext.getUserPrincipal();
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

    protected QueryBuilder addEtmPrincipalFilterQuery(BoolQueryBuilder queryBuilder) {
        List<FilterQuery> filterQueries = getEtmPrincipalFilterQueries();
        if (filterQueries == null || filterQueries.isEmpty()) {
            return queryBuilder;
        }
        for (FilterQuery filterQuery : filterQueries) {
            if (QueryOccurrence.MUST.equals(filterQuery.getQueryOccurrence())) {
                queryBuilder.filter(filterQuery.getQuery());
            } else if (QueryOccurrence.MUST_NOT.equals(filterQuery.getQueryOccurrence())) {
                queryBuilder.filter(QueryBuilders.boolQuery().mustNot(filterQuery.getQuery()));
            }
        }
        return queryBuilder;
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

    /**
     * Converts a json string to a <code>Map</code> containing the values of the json string
     * in the given namespace. The namespace will be the single key in the returned <code>Map</code>. This method is
     * the reverse of {@link #toStringWithoutNamespace(Map, String)}
     * This method will mainly be used for converting json received from the front end.
     *
     * @param json      The json string.
     * @param namespace The namespace to use.
     * @return A <code>Map</code> containing the json values in the given namespace.
     */
    protected Map<String, Object> toMapWithNamespace(String json, String namespace) {
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put(namespace, toMap(json));
        return objectMap;
    }

    /**
     * Converts a json string to a namspaced json string containing the values of the original json string
     * in the given namespace.
     * This method will mainly be used for converting json received from the front end.
     *
     * @param json      The json string.
     * @param namespace The namespace to use.
     * @return A string containing the json values in the given namespace.
     */
    protected String toStringWithNamespace(String json, String namespace) {
        Map<String, Object> objectMap = toMapWithNamespace(json, namespace);
        return toString(objectMap);
    }

    /**
     * Converts a namespaced object <code>Map</code> to a json string without the namespace. This method is the reverse
     * of {@link #toMapWithNamespace(String, String)}
     * This method will mainly be used for converting json received from the backend that needs to be returned to the front end.
     *
     * @param namespacedObjectMap The <code>Map</code> that holds the values in a namespace.
     * @param namespace           The namespace to use.
     * @return A json string containing the values without a namespace.
     */
    protected String toStringWithoutNamespace(Map<String, Object> namespacedObjectMap, String namespace) {
        Map<String, Object> objectMap = getObject(namespace, namespacedObjectMap);
        return toString(objectMap);
    }

    /**
     * Converts a namespaced object <code>Map</code> to a <code>Map</code> string without the namespace.
     *
     * @param namespacedObjectMap The <code>Map</code> that holds the values in a namespace.
     * @param namespace           The namespace to use.
     * @return A <code>Map</code> containing the values without a namespace.
     */
    protected Map<String, Object> toMapWithoutNamespace(Map<String, Object> namespacedObjectMap, String namespace) {
        return getObject(namespace, namespacedObjectMap);
    }

    /**
     * Converts a namespaced json string to a json string without the namespace.
     * This method will mainly be used for converting json received from the backend that needs to be returned to the front end.
     *
     * @param namespacedJsonString The json string that holds the values in a namespace.
     * @param namespace            The namespace to use.
     * @return A json string containing the values without a namespace.
     */
    protected String toStringWithoutNamespace(String namespacedJsonString, String namespace) {
        return toStringWithoutNamespace(toMap(namespacedJsonString), namespace);
    }

}
