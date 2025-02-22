/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.server.core.persisting;

import com.jecstar.etm.domain.writer.TelemetryEventTags;
import com.jecstar.etm.domain.writer.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.server.core.rest.AbstractJsonService;
import org.elasticsearch.index.query.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Class capable of parsing a query string into a nested join query.
 */
public class EtmQueryBuilder {

    private static final String JOIN_KEYWORD = "WITH";
    private static final String JOIN_ELEMENT_REQUEST = "REQUEST";
    private static final String JOIN_ELEMENT_RESPONSE = "RESPONSE";
    private static final List<String> JOIN_ELEMENTS = Arrays.asList(JOIN_ELEMENT_REQUEST, JOIN_ELEMENT_RESPONSE);

    private final Pattern queryParserPattern = Pattern.compile("[^\\s\"]+|\"(?:[^\"\\\\]|\\\\.)*\"");
    private final TelemetryEventTags eventTags = new TelemetryEventTagsJsonImpl();

    private final String queryString;
    private final DataRepository dataRepository;
    private final RequestEnhancer requestEnhancer;
    private final List<QueryBuilder> joinFilters = new ArrayList<>();
    private final List<QueryBuilder> rootFilters = new ArrayList<>();
    private String timeZoneId;

    public EtmQueryBuilder(String queryString, DataRepository dataRepository, RequestEnhancer requestEnhancer) {
        this.queryString = queryString;
        this.dataRepository = dataRepository;
        this.requestEnhancer = requestEnhancer;
    }

    public BoolQueryBuilder buildRootQuery() {
        BoolQueryBuilder queryBuilder = parseQuery(this.queryString);
        for (var filterQuery : this.rootFilters) {
            queryBuilder.filter(filterQuery);
        }
        return queryBuilder;
    }

    private BoolQueryBuilder parseQuery(String queryString) {
        if (!queryString.contains(JOIN_KEYWORD)) {
            // No join keyword, just return the query
            return new BoolQueryBuilder().must(createQueryStringQuery(queryString));
        }
        var elements = new ArrayList<String>();
        var matcher = queryParserPattern.matcher(queryString);
        while (matcher.find()) {
            elements.add(matcher.group());
        }
        var joinIx = elements.indexOf(JOIN_KEYWORD);
        if (joinIx == -1 || elements.size() <= joinIx + 1 || !JOIN_ELEMENTS.contains(elements.get(joinIx + 1))) {
            // Join keyword was probably in a quotes string. Return the original query.
            return new BoolQueryBuilder().must(createQueryStringQuery(queryString));
        }
        var queryStringBuilder = createQueryStringQuery(elements.stream().limit(joinIx).collect(Collectors.joining(" ")));
        BoolQueryBuilder rootQuery = new BoolQueryBuilder().must(queryStringBuilder);
        var joinQueryString = elements.stream().skip(joinIx + 2).collect(Collectors.joining(" "));
        if (joinQueryString.trim().length() == 0) {
            // No sub query after the join keywords. Return the  original query without the join keywords.
            return rootQuery;
        }
        // Execute the right part of the join.
        var joinQueryBuilder = new BoolQueryBuilder();
        joinQueryBuilder.must(parseQuery(joinQueryString));
        var joinElement = JOIN_ELEMENTS.get(JOIN_ELEMENTS.indexOf(elements.get(joinIx + 1)));
        for (var filterQuery : this.joinFilters) {
            joinQueryBuilder.filter(filterQuery);
        }
        SearchRequestBuilder joinRequestBuilder = this.requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL)
                .setQuery(joinQueryBuilder)
                .setFetchSource(false));
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, joinRequestBuilder, null);
        var ids = new ArrayList<String>();
        for (var searchHit : scrollableSearch) {
            ids.add(searchHit.getId());
        }
        if (ids.isEmpty()) {
            // Not a single match in the join query. The main query must not return any hits
            rootQuery.filter(new TermQueryBuilder("_not_existing_tag_", 1));
        } else {
            if (JOIN_ELEMENT_REQUEST.equals(joinElement)) {
                rootQuery.filter(new TermsQueryBuilder(this.eventTags.getCorrelationIdTag() + AbstractJsonService.KEYWORD_SUFFIX, ids));
            } else if (JOIN_ELEMENT_RESPONSE.equals(joinElement)) {
                rootQuery.filter(new TermsQueryBuilder(this.eventTags.getCorrelationsTag() + AbstractJsonService.KEYWORD_SUFFIX, ids));
            }
        }
        return rootQuery;
    }

    private QueryStringQueryBuilder createQueryStringQuery(String queryString) {
        QueryStringQueryBuilder builder = new QueryStringQueryBuilder(queryString)
                .allowLeadingWildcard(true)
                .analyzeWildcard(true)
                .defaultField(ElasticsearchLayout.ETM_ALL_FIELDS_ATTRIBUTE_NAME);
        if (this.timeZoneId != null) {
            builder.timeZone(this.timeZoneId);
        }
        return builder;
    }

    /**
     * Add an filter to any of the sub queries that will be executed. This is particularly useful when the root query is time scoped. That scope should also be applied to the join queries.
     *
     * @param queryBuilder The <code>QueryBuilder</code> to apply as filter on the internal join queries.
     * @return This object for chaining.
     */
    public EtmQueryBuilder filterJoin(QueryBuilder queryBuilder) {
        this.joinFilters.add(queryBuilder);
        return this;
    }

    /**
     * Add an filter to the root query.
     *
     * @param queryBuilder The <code>QueryBuilder</code> to apply as filter on the root query.
     * @return This object for chaining.
     */
    public EtmQueryBuilder filterRoot(QueryBuilder queryBuilder) {
        this.rootFilters.add(queryBuilder);
        return this;
    }

    /**
     * Sets the time zone id that should be used in internal queries.
     *
     * @param timeZoneId The time zone id.
     * @return This instance for chaining.
     */
    public EtmQueryBuilder setTimeZone(String timeZoneId) {
        this.timeZoneId = timeZoneId;
        return this;
    }
}
