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

package com.jecstar.etm.gui.rest.services.settings;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.gui.rest.services.AbstractIndexMetadataService;
import com.jecstar.etm.gui.rest.services.Keyword;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.text.NumberFormat;
import java.util.List;

@Path("/audit")
@DeclareRoles(SecurityRoles.ALL_ROLES)
public class AuditService extends AbstractIndexMetadataService {


    private static DataRepository dataRepository;
    private static EtmConfiguration etmConfiguration;

    public static void initialize(DataRepository dataRepository, EtmConfiguration etmConfiguration) {
        AuditService.dataRepository = dataRepository;
        AuditService.etmConfiguration = etmConfiguration;
    }

    @GET
    @Path("/keywords/{indexName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.AUDIT_LOG_READ)
    public String getKeywords(@PathParam("indexName") String indexName) {
        var builder = new JsonBuilder();
        List<Keyword> keywords = getIndexFields(AuditService.dataRepository, indexName);
        builder.startObject();
        builder.startArray("keywords");
        builder.startObject();
        builder.field("index", indexName);
        builder.startArray("keywords");
        for (var keyword : keywords) {
            builder.startObject();
            builder.field("name", keyword.getName());
            builder.field("type", keyword.getType());
            builder.field("date", keyword.isDate());
            builder.field("number", keyword.isNumber());
            builder.endObject();
        }
        builder.endArray();
        builder.endObject();
        builder.endArray();
        builder.endObject();
        return builder.build();
    }

    @GET
    @Path("/{index}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.AUDIT_LOG_READ)
    public String getAuditLog(@PathParam("index") String index, @PathParam("id") String id) {
        if (!index.startsWith(ElasticsearchLayout.AUDIT_LOG_INDEX_PREFIX)) {
            return null;
        }
        GetResponse getResponse = dataRepository.get(new GetRequestBuilder(index, id)
                .setFetchSource(true));
        return getResponse.getSourceAsString();
    }


    @POST
    @Path("/query")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.AUDIT_LOG_READ)
    public String executeQuery(String json) {
        var startTime = System.currentTimeMillis();
        var etmPrincipal = getEtmPrincipal();

        var parameters = new AuditSearchRequestParameters(toMap(json));
        var requestBuilder = createRequestFromInput(parameters, etmPrincipal);
        var numberFormat = NumberFormat.getInstance(etmPrincipal.getLocale());
        var response = dataRepository.search(requestBuilder);
        var builder = new JsonBuilder();
        builder.startObject();
        builder.field("status", "success");
        builder.field("hits", response.getHits().getTotalHits().value);
        builder.field("hits_relation", response.getHits().getTotalHits().relation.name());
        builder.field("hits_as_string", numberFormat.format(response.getHits().getTotalHits().value) + (TotalHits.Relation.GREATER_THAN_OR_EQUAL_TO.equals(response.getHits().getTotalHits().relation) ? "+" : ""));
        builder.field("time_zone", etmPrincipal.getTimeZone().getID());
        builder.field("start_ix", parameters.getStartIndex());
        builder.field("end_ix", parameters.getStartIndex() + response.getHits().getHits().length - 1);
        // TODO has_more_results is inaccurate in etm 4 because totalhits is a GTE value.
        builder.field("has_more_results", parameters.getStartIndex() + response.getHits().getHits().length < response.getHits().getTotalHits().value - 1);
        builder.field("time_zone", etmPrincipal.getTimeZone().getID());
        builder.startArray("results");
        addSearchHits(builder, response.getHits());
        builder.endArray();
        long queryTime = System.currentTimeMillis() - startTime;
        builder.field("query_time", queryTime);
        builder.field("query_time_as_string", numberFormat.format(queryTime));
        builder.endObject();
        return builder.build();
    }

    private SearchRequestBuilder createRequestFromInput(AuditSearchRequestParameters parameters, EtmPrincipal etmPrincipal) {
        QueryStringQueryBuilder queryStringBuilder = new QueryStringQueryBuilder(parameters.getQueryString()).allowLeadingWildcard(true)
                .analyzeWildcard(true)
                .defaultField(ElasticsearchLayout.ETM_ALL_FIELDS_ATTRIBUTE_NAME)
                .timeZone(etmPrincipal.getTimeZone().getID());
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(queryStringBuilder);
        boolQueryBuilder.filter(new RangeQueryBuilder("timestamp").lte(parameters.getNotAfterTimestamp()));
        SearchRequestBuilder requestBuilder = new SearchRequestBuilder().setIndices(ElasticsearchLayout.AUDIT_LOG_INDEX_ALIAS_ALL).setQuery(boolQueryBuilder)
                .setFetchSource(true)
                .setFrom(parameters.getStartIndex())
                .setSize(parameters.getMaxResults() > 500 ? 500 : parameters.getMaxResults())
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
        if (parameters.getSortField() != null && parameters.getSortField().trim().length() > 0) {
            String sortProperty = getSortProperty(dataRepository, ElasticsearchLayout.AUDIT_LOG_INDEX_ALIAS_ALL, parameters.getSortField());
            if (sortProperty != null) {
                requestBuilder.setSort(sortProperty, "desc".equals(parameters.getSortOrder()) ? SortOrder.DESC : SortOrder.ASC);
            }
        }
        return requestBuilder;
    }

    private void addSearchHits(JsonBuilder builder, SearchHits hits) {
        for (SearchHit searchHit : hits.getHits()) {
            builder.startObject();
            builder.field("index", searchHit.getIndex());
            builder.field("id", searchHit.getId());
            builder.rawField("source", searchHit.getSourceAsString());
            builder.endObject();
        }
    }

}
