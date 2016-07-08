package com.jecstar.etm.gui.rest.services.settings;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsAction;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequestBuilder;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.jecstar.etm.gui.rest.AbstractJsonService;
import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.configuration.License;
import com.jecstar.etm.server.core.domain.EtmPrincipal;
import com.jecstar.etm.server.core.domain.converter.EtmConfigurationConverter;
import com.jecstar.etm.server.core.domain.converter.ExpressionParserConverter;
import com.jecstar.etm.server.core.domain.converter.json.EtmConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.json.ExpressionParserConverterJsonImpl;
import com.jecstar.etm.server.core.parsers.ExpressionParser;

@Path("/settings")
public class SettingsService extends AbstractJsonService {
	
	
	private final String configurationIndexName = "etm_configuration";
	private final String licenseIndexType = "license";
	private final String licenseId = "default_configuration";
	private final String parserIndexType = "parser";
	private final String endpointIndexType = "endpoint";
	private final String defaultEndpointId = "default_configuration";
	
	private final EtmConfigurationConverter<String> etmConfigurationConverter = new EtmConfigurationConverterJsonImpl();
	private final ExpressionParserConverter<String> expressionParserConverter = new ExpressionParserConverterJsonImpl();
	
	private static Client client;
	private static EtmConfiguration etmConfiguration;
	
	public static void initialize(Client client, EtmConfiguration etmConfiguration) {
		SettingsService.client = client;
		SettingsService.etmConfiguration = etmConfiguration;
	}
	
	@GET
	@Path("/license")
	@Produces(MediaType.APPLICATION_JSON)
	public String getLicense() {
		EtmPrincipal etmPrincipal = getEtmPrincipal();
		License license = etmConfiguration.getLicense();
		if (license == null) {
			return null;
		}
		boolean added = false;
		StringBuilder result = new StringBuilder();
		result.append("{");
		added = addStringElementToJsonBuffer("owner", license.getOwner(), result, !added) || added;
		added = addLongElementToJsonBuffer("expiration_date", license.getExpiryDate().toEpochMilli(), result, !added) || added;
		added = addStringElementToJsonBuffer("time_zone", etmPrincipal.getTimeZone().getID(), result, !added) || added;
		added = addStringElementToJsonBuffer("license_type", license.getLicenseType().name(), result, !added) || added;
		result.append("}");
		return result.toString();
	}
	
	@PUT
	@Path("/license")
	@Produces(MediaType.APPLICATION_JSON)
	public String setLicense(String json) {
		Map<String, Object> requestValues = toMap(json); 
		String licenseKey = getString("key", requestValues);
		etmConfiguration.setLicenseKey(licenseKey);
		Map<String, Object> values = new HashMap<>();
		values.put(this.etmConfigurationConverter.getTags().getLicenseTag(), licenseKey);
		client.prepareUpdate(this.configurationIndexName, this.licenseIndexType, this.licenseId)
			.setDoc(values)
			.setDocAsUpsert(true)
			.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
			.get();
		EtmPrincipal etmPrincipal = getEtmPrincipal();
		License license = etmConfiguration.getLicense();
		boolean added = false;
		StringBuilder result = new StringBuilder();
		result.append("{");
		added = addStringElementToJsonBuffer("status", "success", result, !added) || added;
		added = addStringElementToJsonBuffer("owner", license.getOwner(), result, !added) || added;
		added = addLongElementToJsonBuffer("expiration_date", license.getExpiryDate().toEpochMilli(), result, !added) || added;
		added = addStringElementToJsonBuffer("time_zone", etmPrincipal.getTimeZone().getID(), result, !added) || added;
		added = addStringElementToJsonBuffer("license_type", license.getLicenseType().name(), result, !added) || added;
		result.append("}");
		return result.toString();
	}
	
	@GET
	@Path("/parsers")
	@Produces(MediaType.APPLICATION_JSON)	
	public String getParsers() {
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(this.configurationIndexName)
			.setTypes(this.parserIndexType)
			.setFetchSource(true)
			.setQuery(QueryBuilders.matchAllQuery())
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
		if (!scrollableSearch.hasNext()) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		result.append("{\"parsers\": [");
		boolean first = true;
		for (SearchHit searchHit : scrollableSearch) {
			if (!first) {
				result.append(",");
			}
			result.append(searchHit.getSourceAsString());
			first = false;
		}
		result.append("]}");
		return result.toString();
	}
	
	
	@DELETE
	@Path("/parser/{parserName}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String deleteParser(@PathParam("parserName") String parserName) {
		client.prepareDelete(this.configurationIndexName, this.parserIndexType, parserName)
			.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.get();
		// TODO, loop door endpoints en verwijder referentie
		return "{\"status\":\"success\"}";
	}

	@PUT
	@Path("/parser/{parserName}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String addParser(@PathParam("parserName") String parserName, String json) {
		// Do a read and write of the parser to make sure it's valid.
		ExpressionParser expressionParser = this.expressionParserConverter.read(json);
		client.prepareUpdate(this.configurationIndexName, this.parserIndexType, parserName)
			.setDoc(this.expressionParserConverter.write(expressionParser))
			.setDocAsUpsert(true)
			.setDetectNoop(true)
			.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
			.get();
		return "{ \"status\": \"success\" }";
	}

	@GET
	@Path("/endpoints")
	@Produces(MediaType.APPLICATION_JSON)	
	public String getEndpoints() {
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(this.configurationIndexName)
			.setTypes(this.endpointIndexType)
			.setFetchSource(true)
			.setQuery(QueryBuilders.matchAllQuery())
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
		StringBuilder result = new StringBuilder();
		result.append("{\"endpoints\": [");
		boolean first = true;
		boolean defaultFound = false;
		for (SearchHit searchHit : scrollableSearch) {
			if (!first) {
				result.append(",");
			}
			result.append(searchHit.getSourceAsString());
			if (this.defaultEndpointId.equals(searchHit.getId())) {
				defaultFound = true;
			}
			first = false;
		}
		if (!defaultFound) {
			if (!first) {
				result.append(",");
			}
			result.append("{\"name\": " + escapeToJson(this.defaultEndpointId, true) + ", \"detect_payload_format\": true}");			
		}
	
		result.append("]}");
		return result.toString();
	}
	
	
	@DELETE
	@Path("/endpoint/{endpointName}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String deleteEndpoint(@PathParam("endpointName") String endpointName) {
		client.prepareDelete(this.configurationIndexName, this.endpointIndexType, endpointName)
			.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.get();
		return "{\"status\":\"success\"}";
	}

	@PUT
	@Path("/endpoint/{endpointName}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String addEndpoint(@PathParam("endpointName") String endpointName, String json) {
		// TODO als de naam default_configuration is moeten alle endpoints bij langs om bijgewerkt te worden.
		// Anders -> Default ophalen en mergen met dit endpoint.
		
		// Do a read and write of the parser to make sure it's valid.
//		ExpressionParser expressionParser = this.expressionParserConverter.read(json);
//		client.prepareUpdate(this.configurationIndexName, this.endpointIndexType, endpointName)
//			.setDoc(this.expressionParserConverter.write(expressionParser))
//			.setDocAsUpsert(true)
//			.setDetectNoop(true)
//			.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()))
//			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
//			.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
//			.get();
		return "{ \"status\": \"success\" }";
	}
	
	@GET
	@Path("/nodes/es")
	@Produces(MediaType.APPLICATION_JSON)
	public String getEsNodes() {
		NodesStatsResponse nodesStatsResponse = new NodesStatsRequestBuilder(client, NodesStatsAction.INSTANCE).all().setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout())).get();
		try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            nodesStatsResponse.toXContent(builder, ToXContent.EMPTY_PARAMS);
            builder.endObject();
            return builder.string();
        } catch (IOException e) {
        	throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
	}
}
