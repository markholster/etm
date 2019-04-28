package com.jecstar.etm.gui.rest.services.settings;

import com.jecstar.etm.gui.rest.services.AbstractIndexMetadataService;
import com.jecstar.etm.gui.rest.services.Keyword;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
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
import java.util.stream.Collectors;

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
        StringBuilder result = new StringBuilder();
        List<Keyword> keywords = getIndexFields(AuditService.dataRepository, indexName);
        result.append("{ \"keywords\":[");
        result.append("{");
        result.append("\"index\": ").append(escapeToJson(indexName, true)).append(",");
        result.append("\"keywords\": [").append(keywords.stream().map(n -> {
            StringBuilder kw = new StringBuilder();
            kw.append("{");
            addStringElementToJsonBuffer("name", n.getName(), kw, true);
            addStringElementToJsonBuffer("type", n.getType(), kw, false);
            addBooleanElementToJsonBuffer("date", n.isDate(), kw, false);
            addBooleanElementToJsonBuffer("number", n.isNumber(), kw, false);
            kw.append("}");
            return kw.toString();
        }).collect(Collectors.joining(", "))).append("]");
        result.append("}");
        result.append("]}");
        return result.toString();
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
        long startTime = System.currentTimeMillis();
        EtmPrincipal etmPrincipal = getEtmPrincipal();

        AuditSearchRequestParameters parameters = new AuditSearchRequestParameters(toMap(json));
        SearchRequestBuilder requestBuilder = createRequestFromInput(parameters, etmPrincipal);
        NumberFormat numberFormat = NumberFormat.getInstance(etmPrincipal.getLocale());
        SearchResponse response = dataRepository.search(requestBuilder);
        StringBuilder result = new StringBuilder();
        result.append("{");
        result.append("\"status\": \"success\"");
        result.append(",\"hits\": ").append(response.getHits().getTotalHits().value);
        result.append(",\"hits_as_string\": \"").append(numberFormat.format(response.getHits().getTotalHits().value)).append("\"");
        result.append(",\"time_zone\": \"").append(etmPrincipal.getTimeZone().getID()).append("\"");
        result.append(",\"start_ix\": ").append(parameters.getStartIndex());
        result.append(",\"end_ix\": ").append(parameters.getStartIndex() + response.getHits().getHits().length - 1);
        result.append(",\"has_more_results\": ").append(parameters.getStartIndex() + response.getHits().getHits().length < response.getHits().getTotalHits().value - 1);
        result.append(",\"time_zone\": \"").append(etmPrincipal.getTimeZone().getID()).append("\"");
        result.append(",\"results\": [");
        addSearchHits(result, response.getHits());
        result.append("]");
        long queryTime = System.currentTimeMillis() - startTime;
        result.append(",\"query_time\": ").append(queryTime);
        result.append(",\"query_time_as_string\": \"").append(numberFormat.format(queryTime)).append("\"");
        result.append("}");
        return result.toString();
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

    private void addSearchHits(StringBuilder result, SearchHits hits) {
        boolean first = true;
        for (SearchHit searchHit : hits.getHits()) {
            if (first) {
                result.append("{");
                first = false;
            } else {
                result.append(", {");
            }
            addStringElementToJsonBuffer("index", searchHit.getIndex(), result, true);
            addStringElementToJsonBuffer("id", searchHit.getId(), result, false);
            result.append(", \"source\": ").append(searchHit.getSourceAsString());
            result.append("}");
        }
    }

}
