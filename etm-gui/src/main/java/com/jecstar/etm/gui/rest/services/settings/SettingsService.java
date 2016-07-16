package com.jecstar.etm.gui.rest.services.settings;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
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
import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.configuration.License;
import com.jecstar.etm.server.core.domain.EndpointConfiguration;
import com.jecstar.etm.server.core.domain.EtmPrincipal;
import com.jecstar.etm.server.core.domain.EtmPrincipal.PrincipalRole;
import com.jecstar.etm.server.core.domain.converter.EndpointConfigurationConverter;
import com.jecstar.etm.server.core.domain.converter.EtmConfigurationConverter;
import com.jecstar.etm.server.core.domain.converter.EtmPrincipalConverter;
import com.jecstar.etm.server.core.domain.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.converter.ExpressionParserConverter;
import com.jecstar.etm.server.core.domain.converter.json.EndpointConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.json.EtmConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.json.ExpressionParserConverterJsonImpl;
import com.jecstar.etm.server.core.enhancers.DefaultTelemetryEventEnhancer;
import com.jecstar.etm.server.core.parsers.ExpressionParser;
import com.jecstar.etm.server.core.parsers.ExpressionParserField;

@Path("/settings")
public class SettingsService extends AbstractJsonService {
	
	private final EtmConfigurationConverter<String> etmConfigurationConverter = new EtmConfigurationConverterJsonImpl();
	private final ExpressionParserConverter<String> expressionParserConverter = new ExpressionParserConverterJsonImpl();
	private final EndpointConfigurationConverter<String> endpointConfigurationConverter = new EndpointConfigurationConverterJsonImpl();
	private final EtmPrincipalConverter<String> etmPrincipalConverter = new EtmPrincipalConverterJsonImpl();
	
	private final String parserInEnpointTag = this.endpointConfigurationConverter.getTags().getEnhancerTag() +
			"." + this.endpointConfigurationConverter.getTags().getFieldsTag() +
			"." + this.endpointConfigurationConverter.getTags().getParsersTag() +
			"." + this.endpointConfigurationConverter.getTags().getNameTag();
	
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
		client.prepareUpdate(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_LICENSE, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_LICENSE_ID)
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
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticSearchLayout.CONFIGURATION_INDEX_NAME)
			.setTypes(ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_PARSER)
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
	
	@GET
	@Path("/parserfields")
	@Produces(MediaType.APPLICATION_JSON)	
	public String getParserFields() {
		StringBuilder result = new StringBuilder();
		boolean first = true;
		result.append("{\"parserfields\": [");
		for (ExpressionParserField field : ExpressionParserField.values()) {
			if (!first) {
				result.append(",");
			}
			result.append("{");
			addStringElementToJsonBuffer("name", field.getJsonTag(), result, true);
			result.append("}");
			first = false;
		}
		result.append("]}");
		return result.toString();
	}
	
	
	@DELETE
	@Path("/parser/{parserName}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String deleteParser(@PathParam("parserName") String parserName) {
		BulkRequestBuilder bulkRequestBuilder = client.prepareBulk()
				.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()));
		bulkRequestBuilder.add(client.prepareDelete(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_PARSER, parserName)
			.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
		);
		removeParserFromEndpoints(bulkRequestBuilder, parserName);
		bulkRequestBuilder.get();
		return "{\"status\":\"success\"}";
	}

	private void removeParserFromEndpoints(BulkRequestBuilder bulkRequestBuilder, String parserName) {
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticSearchLayout.CONFIGURATION_INDEX_NAME)
				.setTypes(ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_ENDPOINT)
				.setFetchSource(true)
				.setQuery(QueryBuilders.termQuery(parserInEnpointTag, parserName))
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
		for (SearchHit searchHit : scrollableSearch) {
			boolean updated = false;
			EndpointConfiguration endpointConfig = this.endpointConfigurationConverter.read(searchHit.getSourceAsString());
			if (endpointConfig.eventEnhancer instanceof DefaultTelemetryEventEnhancer) {
				DefaultTelemetryEventEnhancer enhancer = (DefaultTelemetryEventEnhancer) endpointConfig.eventEnhancer;
				for (List<ExpressionParser> parsers : enhancer.getFields().values()) {
					if (parsers != null) {
						Iterator<ExpressionParser> it = parsers.iterator();
						while (it.hasNext()) {
							ExpressionParser parser = it.next();
							if (parser.getName().equals(parserName)) {
								it.remove();
								updated = true;
							}
						}
					}
				}
			}
			if (updated) {
				bulkRequestBuilder.add(createEndpointUpdateRequest(endpointConfig));
			}
		}
	}

	@PUT
	@Path("/parser/{parserName}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String addParser(@PathParam("parserName") String parserName, String json) {
		BulkRequestBuilder bulkRequestBuilder = client.prepareBulk()
				.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()));
		// Do a read and write of the parser to make sure it's valid.
		ExpressionParser expressionParser = this.expressionParserConverter.read(json);
		bulkRequestBuilder.add(client.prepareUpdate(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_PARSER, parserName)
			.setDoc(this.expressionParserConverter.write(expressionParser))
			.setDocAsUpsert(true)
			.setDetectNoop(true)
			.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
		);
		updateParserInEndpoints(bulkRequestBuilder, expressionParser);
		bulkRequestBuilder.get();
		return "{ \"status\": \"success\" }";
	}
	
	private void updateParserInEndpoints(BulkRequestBuilder bulkRequestBuilder, ExpressionParser expressionParser) {
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticSearchLayout.CONFIGURATION_INDEX_NAME)
				.setTypes(ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_ENDPOINT)
				.setFetchSource(true)
				.setQuery(QueryBuilders.termQuery(parserInEnpointTag, expressionParser.getName()))
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
		for (SearchHit searchHit : scrollableSearch) {
			boolean updated = false;
			EndpointConfiguration endpointConfig = this.endpointConfigurationConverter.read(searchHit.getSourceAsString());
			if (endpointConfig.eventEnhancer instanceof DefaultTelemetryEventEnhancer) {
				DefaultTelemetryEventEnhancer enhancer = (DefaultTelemetryEventEnhancer) endpointConfig.eventEnhancer;
				for (List<ExpressionParser> parsers : enhancer.getFields().values()) {
					if (parsers != null) {
						ListIterator<ExpressionParser> it = parsers.listIterator();
						while (it.hasNext()) {
							ExpressionParser parser = it.next();
							if (parser.getName().equals(expressionParser.getName())) {
								it.set(expressionParser);
								updated = true;
							}
						}
					}					
				}
			}
			if (updated) {
				bulkRequestBuilder.add(createEndpointUpdateRequest(endpointConfig));
			}
		}
	}

	@GET
	@Path("/endpoints")
	@Produces(MediaType.APPLICATION_JSON)	
	public String getEndpoints() {
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticSearchLayout.CONFIGURATION_INDEX_NAME)
			.setTypes(ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_ENDPOINT)
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
			if (ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_ENDPOINT_DEFAULT.equals(searchHit.getId())) {
				defaultFound = true;
			}
			first = false;
		}
		if (!defaultFound) {
			if (!first) {
				result.append(",");
			}
			EndpointConfiguration defaultEndpointConfiguration = new EndpointConfiguration();
			defaultEndpointConfiguration.name = "*";
			defaultEndpointConfiguration.eventEnhancer = new  DefaultTelemetryEventEnhancer();
			result.append(this.endpointConfigurationConverter.write(defaultEndpointConfiguration));			
		}
	
		result.append("]}");
		return result.toString();
	}
	
	
	@DELETE
	@Path("/endpoint/{endpointName}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String deleteEndpoint(@PathParam("endpointName") String endpointName) {
		client.prepareDelete(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_ENDPOINT, endpointName)
			.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.get();
		return "{\"status\":\"success\"}";
	}

	@PUT
	@Path("/endpoint/{endpointName}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String addEndpoint(@PathParam("endpointName") String endpointName, String json) {
		// Do a read and write of the endpoint to make sure it's valid.
		EndpointConfiguration endpointConfiguration = this.endpointConfigurationConverter.read(json);
		createEndpointUpdateRequest(endpointConfiguration).get();
		return "{ \"status\": \"success\" }";
	}
	
	private UpdateRequestBuilder createEndpointUpdateRequest(EndpointConfiguration endpointConfiguration) {
		return client.prepareUpdate(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_ENDPOINT, endpointConfiguration.name)
		.setDoc(this.endpointConfigurationConverter.write(endpointConfiguration))
		.setDocAsUpsert(true)
		.setDetectNoop(true)
		.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()))
		.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
		.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount());		
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
	
	@GET
	@Path("/users")
	@Produces(MediaType.APPLICATION_JSON)	
	public String getUsers() {
		EtmPrincipalTags tags = this.etmPrincipalConverter.getTags();
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticSearchLayout.CONFIGURATION_INDEX_NAME)
			.setTypes(ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER)
			.setFetchSource(new String[] {"*"}, new String[] {tags.getPasswordHashTag(), tags.getSearchTemplatesTag(), tags.getQueryHistoryTag()})
			.setQuery(QueryBuilders.matchAllQuery())
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
		if (!scrollableSearch.hasNext()) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		result.append("{\"users\": [");
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
	
	@GET
	@Path("/userroles")
	@Produces(MediaType.APPLICATION_JSON)	
	public String getUserRoles() {
		StringBuilder result = new StringBuilder();
		boolean first = true;
		result.append("{\"userRoles\": [");
		for (PrincipalRole role : PrincipalRole.values()) {
			if (!first) {
				result.append(",");
			}
			result.append("{");
			addStringElementToJsonBuffer("name", role.getRoleName(), result, true);
			result.append("}");
			first = false;
		}
		result.append("]}");
		return result.toString();
	}
	
	
	@DELETE
	@Path("/user/{userName}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String deleteUser(@PathParam("userName") String userName) {
		client.prepareDelete(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, userName)
			.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.get();
		return "{\"status\":\"success\"}";
	}

	@PUT
	@Path("/user/{userName}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String addUser(@PathParam("userName") String userName, String json) {
		// Do a read and write of the user to make sure it's valid.
		EtmPrincipal principal = this.etmPrincipalConverter.read(json);
		client.prepareUpdate(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, userName)
			.setDoc(this.etmPrincipalConverter.write(principal))
			.setDocAsUpsert(true)
			.setDetectNoop(true)
			.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
			.get();
		if (userName.equals(principal.getId())) {
			principal.forceReload = true;
		}
		return "{ \"status\": \"success\" }";
	}

}
