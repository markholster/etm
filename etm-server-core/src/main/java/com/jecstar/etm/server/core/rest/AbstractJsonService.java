package com.jecstar.etm.server.core.rest;

import com.jecstar.etm.server.core.domain.QueryOccurrence;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.FilterQuery;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;

import java.text.DateFormatSymbols;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;

/**
 * Abstract superclass for all services that handle json requests.
 */
public abstract class AbstractJsonService extends JsonConverter {

    public static final String KEYWORD_SUFFIX = ".keyword";

    /**
     * Method to determine whether or not an <code>EtmPrincipal</code> has filter queries.
     *
     * @param etmPrincipal The <code>EtmPrincipal</code> to test for filter queries.
     * @return <code>true</code> when the <code>EtmPrincipal</code> holds filter queries, <code>false</code> otherwise.
     */
    protected boolean hasFilterQueries(EtmPrincipal etmPrincipal) {
        String filterQuery = etmPrincipal.getFilterQuery();
        if (filterQuery != null && filterQuery.trim().length() > 0) {
            return true;
        }
        Set<EtmGroup> groups = etmPrincipal.getGroups();
        for (EtmGroup group : groups) {
            if (group.getFilterQuery() != null && group.getFilterQuery().trim().length() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method that adds the filter query of an <code>EtmGroup</code> to a given query.
     *
     * @param etmGroup     The <code>EtmGroup</code> to retrieve the filter query from.
     * @param queryBuilder The <code>BoolQueryBuilder</code> to enhance with the filter query.
     * @param etmPrincipal The <code>EtmPrincipal</code> to use for date formatting. When the query is executed without
     *                     the context of a user this parameter must be <code>null</code>.
     * @return A <code>QueryBuilder</code> with the filter queries applied.
     */
    protected QueryBuilder addFilterQuery(EtmGroup etmGroup, BoolQueryBuilder queryBuilder, EtmPrincipal etmPrincipal) {
        if (etmGroup.getFilterQuery() == null || etmGroup.getFilterQuery().trim().length() > 0) {
            return queryBuilder;
        }
        QueryStringQueryBuilder queryStringQueryBuilder = new QueryStringQueryBuilder(etmGroup.getFilterQuery().trim())
                .allowLeadingWildcard(true)
                .analyzeWildcard(true);
        if (etmPrincipal != null) {
            queryStringQueryBuilder.timeZone(etmPrincipal.getTimeZone().getID());
        }
        FilterQuery filterQuery = new FilterQuery(etmGroup.getFilterQueryOccurrence(), queryStringQueryBuilder);
        BoolQueryBuilder boolFilter = QueryBuilders.boolQuery();
        if (QueryOccurrence.MUST.equals(filterQuery.getQueryOccurrence())) {
            queryBuilder.filter(filterQuery.getQuery());
        } else if (QueryOccurrence.MUST_NOT.equals(filterQuery.getQueryOccurrence())) {
            boolFilter.mustNot(filterQuery.getQuery());
        }
        if (boolFilter.mustNot().size() > 0) {
            queryBuilder.filter(boolFilter);
        }
        return queryBuilder;
    }

    /**
     * Method that adds the filter query of an <code>EtmPrincipal</code> to a given query.
     *
     * @param etmPrincipal The <code>EtmPrincipal</code> to retrieve the filter queries from.
     * @param queryBuilder The <code>BoolQueryBuilder</code> to enhance with the filter query.
     * @return A <code>QueryBuilder</code> with the filter queries applied.
     */
    protected QueryBuilder addFilterQuery(EtmPrincipal etmPrincipal, BoolQueryBuilder queryBuilder) {
        List<FilterQuery> filterQueries = getEtmPrincipalFilterQueries(etmPrincipal);
        if (filterQueries == null || filterQueries.isEmpty()) {
            return queryBuilder;
        }
        BoolQueryBuilder boolFilter = QueryBuilders.boolQuery();
        for (FilterQuery filterQuery : filterQueries) {
            if (QueryOccurrence.MUST.equals(filterQuery.getQueryOccurrence())) {
                queryBuilder.filter(filterQuery.getQuery());
            } else if (QueryOccurrence.MUST_NOT.equals(filterQuery.getQueryOccurrence())) {
                boolFilter.mustNot(filterQuery.getQuery());
            }
        }
        if (boolFilter.mustNot().size() > 0) {
            queryBuilder.filter(boolFilter);
        }
        return queryBuilder;
    }

    private List<FilterQuery> getEtmPrincipalFilterQueries(EtmPrincipal etmPrincipal) {
        List<FilterQuery> result = new ArrayList<>();
        String filterQuery = etmPrincipal.getFilterQuery();
        if (filterQuery != null && filterQuery.trim().length() > 0) {
            FilterQuery fq = new FilterQuery(etmPrincipal.getFilterQueryOccurrence(),
                    new QueryStringQueryBuilder(filterQuery.trim())
                            .allowLeadingWildcard(true)
                            .analyzeWildcard(true)
                            .timeZone(etmPrincipal.getTimeZone().getID()));
            if (!result.contains(fq)) {
                result.add(fq);
            }
        }
        Set<EtmGroup> groups = etmPrincipal.getGroups();
        for (EtmGroup group : groups) {
            if (group.getFilterQuery() != null && group.getFilterQuery().trim().length() > 0) {
                FilterQuery fq = new FilterQuery(group.getFilterQueryOccurrence(),
                        new QueryStringQueryBuilder(group.getFilterQuery().trim())
                                .allowLeadingWildcard(true)
                                .analyzeWildcard(true)
                                .timeZone(etmPrincipal.getTimeZone().getID()));
                if (!result.contains(fq)) {
                    result.add(fq);
                }
            }
        }
        return result;
    }

    private ActiveShardCount getActiveShardCount(EtmConfiguration etmConfiguration) {
        if (-1 == etmConfiguration.getWaitForActiveShards()) {
            return ActiveShardCount.ALL;
        } else if (0 == etmConfiguration.getWaitForActiveShards()) {
            return ActiveShardCount.NONE;
        }
        return ActiveShardCount.from(etmConfiguration.getWaitForActiveShards());
    }

    protected String getLocalFormatting(EtmPrincipal etmPrincipal) {
        NumberFormat numberFormat = NumberFormat.getInstance(etmPrincipal.getLocale());
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(etmPrincipal.getLocale());
        DateFormatSymbols dateFormatSymbols = DateFormatSymbols.getInstance(etmPrincipal.getLocale());
        StringBuilder result = new StringBuilder();
        result.append("{");
        addStringElementToJsonBuffer("decimal", "" + decimalFormatSymbols.getDecimalSeparator(), result, true);
        addStringElementToJsonBuffer("thousands", "" + decimalFormatSymbols.getGroupingSeparator(), result, false);
        addStringElementToJsonBuffer("timezone", "" + etmPrincipal.getTimeZone().toZoneId().toString(), result, false);
        result.append(",\"currency\": [\"").append(numberFormat.getCurrency().getSymbol(etmPrincipal.getLocale())).append("\", \"\"]");
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

    /**
     * Enhances a <code>DeleteRequestBuilder</code> with the defaults from the <code>EtmConfiguration</code>.
     *
     * @param builder          The <code>DeleteRequestBuilder</code> that needs to be enhanced.
     * @param etmConfiguration The <code>EtmConfiguration</code>.
     * @return The enhanced <code>EtmConfiguration</code>.
     */
    protected DeleteRequestBuilder enhanceRequest(DeleteRequestBuilder builder, EtmConfiguration etmConfiguration) {
        return builder.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
    }

    /**
     * Enhances an <code>UpdateRequestBuilder</code> with the defaults from the <code>EtmConfiguration</code>.
     *
     * @param builder          The <code>UpdateRequestBuilder</code> that needs to be enhanced.
     * @param etmConfiguration The <code>EtmConfiguration</code>.
     * @return The enhanced <code>EtmConfiguration</code>.
     */
    protected UpdateRequestBuilder enhanceRequest(UpdateRequestBuilder builder, EtmConfiguration etmConfiguration) {
        return builder.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .setRetryOnConflict(etmConfiguration.getRetryOnConflictCount());
    }

    /**
     * Enhances an <code>IndexRequestBuilder</code> with the defaults from the <code>EtmConfiguration</code>.
     *
     * @param builder          The <code>IndexRequestBuilder</code> that needs to be enhanced.
     * @param etmConfiguration The <code>EtmConfiguration</code>.
     * @return The enhanced <code>EtmConfiguration</code>.
     */
    protected IndexRequestBuilder enhanceRequest(IndexRequestBuilder builder, EtmConfiguration etmConfiguration) {
        return builder.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
    }

    /**
     * Enhances an <code>SearchRequestBuilder</code> with the defaults from the <code>EtmConfiguration</code>.
     *
     * @param builder          The <code>SearchRequestBuilder</code> that needs to be enhanced.
     * @param etmConfiguration The <code>EtmConfiguration</code>.
     * @return The enhanced <code>EtmConfiguration</code>.
     */
    protected SearchRequestBuilder enhanceRequest(SearchRequestBuilder builder, EtmConfiguration etmConfiguration) {
        return builder.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
    }

    /**
     * Enhances an <code>BulkRequestBuilder</code> with the defaults from the <code>EtmConfiguration</code>.
     *
     * @param builder          The <code>BulkRequestBuilder</code> that needs to be enhanced.
     * @param etmConfiguration The <code>EtmConfiguration</code>.
     * @return The enhanced <code>EtmConfiguration</code>.
     */
    protected BulkRequestBuilder enhanceRequest(BulkRequestBuilder builder, EtmConfiguration etmConfiguration) {
        return builder.setWaitForActiveShards(getActiveShardCount(etmConfiguration));
    }

}
