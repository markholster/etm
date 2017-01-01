package com.jecstar.etm.gui.rest.services.settings;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsAction;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequestBuilder;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.jecstar.etm.gui.rest.AbstractJsonService;
import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import com.jecstar.etm.gui.rest.services.search.SearchRequestParameters;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.configuration.License;
import com.jecstar.etm.server.core.domain.EndpointConfiguration;
import com.jecstar.etm.server.core.domain.EtmGroup;
import com.jecstar.etm.server.core.domain.EtmPrincipal;
import com.jecstar.etm.server.core.domain.EtmPrincipalRole;
import com.jecstar.etm.server.core.domain.converter.EndpointConfigurationConverter;
import com.jecstar.etm.server.core.domain.converter.EtmConfigurationConverter;
import com.jecstar.etm.server.core.domain.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.converter.ExpressionParserConverter;
import com.jecstar.etm.server.core.domain.converter.json.EndpointConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.json.EtmConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.json.ExpressionParserConverterJsonImpl;
import com.jecstar.etm.server.core.enhancers.DefaultTelemetryEventEnhancer;
import com.jecstar.etm.server.core.parsers.ExpressionParser;
import com.jecstar.etm.server.core.parsers.ExpressionParserField;
import com.jecstar.etm.server.core.util.BCrypt;

@Path("/settings")
public class SettingsService extends AbstractJsonService {
	
	private final EtmConfigurationConverter<String> etmConfigurationConverter = new EtmConfigurationConverterJsonImpl();
	private final ExpressionParserConverter<String> expressionParserConverter = new ExpressionParserConverterJsonImpl();
	private final EndpointConfigurationConverter<String> endpointConfigurationConverter = new EndpointConfigurationConverterJsonImpl();
	private final EtmPrincipalConverterJsonImpl etmPrincipalConverter = new EtmPrincipalConverterJsonImpl();
	
	private final String parserInEnpointTag = this.endpointConfigurationConverter.getTags().getEnhancerTag() +
			"." + this.endpointConfigurationConverter.getTags().getFieldsTag() +
			"." + this.endpointConfigurationConverter.getTags().getParsersTag() +
			"." + this.endpointConfigurationConverter.getTags().getNameTag();
	
	
	private final EtmPrincipalTags etmPrincipalTags = this.etmPrincipalConverter.getTags();
	
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
			.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
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
	@Path("/cluster")
	@Produces(MediaType.APPLICATION_JSON)		
	public String getClusterConfiguration() {
		GetResponse getResponse = client.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_NODE, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_NODE_DEFAULT)
				.setFetchSource(true)
				.get();
		EtmConfiguration config = this.etmConfigurationConverter.read(null, getResponse.getSourceAsString(), null);
		return this.etmConfigurationConverter.write(null, config);
	}
	
	@PUT
	@Path("/cluster")
	@Produces(MediaType.APPLICATION_JSON)		
	public String setClusterConfiguration(String json) {
		EtmConfiguration defaultConfig = this.etmConfigurationConverter.read(null, json, null);
		client.prepareUpdate(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_NODE, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_NODE_DEFAULT)
			.setDoc(this.etmConfigurationConverter.write(null, defaultConfig))
			.setDocAsUpsert(true)
			.setDetectNoop(true)
			.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
			.get();
		return "{\"status\":\"success\"}";
	}

	@GET
	@Path("/indicesstats")
	@Produces(MediaType.APPLICATION_JSON)
	public String getIndexStatistics() {
		IndicesStatsResponse indicesStatsResponse = client.admin().indices().prepareStats("etm_event_*")
				.clear()
				.setStore(true)
				.setDocs(true)
				.setIndexing(true)
				.setSearch(true)
				.get();
		Locale locale = getEtmPrincipal().getLocale();
		NumberFormat numberFormat = NumberFormat.getInstance(locale);
		StringBuilder result = new StringBuilder();
		result.append("{");
		result.append("\"d3_formatter\": " + getD3Formatter());
		result.append(",\"totals\": {");
		addLongElementToJsonBuffer("document_count", indicesStatsResponse.getTotal().docs.getCount(), result, true);	
		addStringElementToJsonBuffer("document_count_as_string", numberFormat.format(indicesStatsResponse.getTotal().docs.getCount()), result, false);	
		addLongElementToJsonBuffer("size_in_bytes", indicesStatsResponse.getTotal().store.getSizeInBytes(), result, false);	
		addStringElementToJsonBuffer("size_in_bytes_as_string", numberFormat.format(indicesStatsResponse.getTotal().store.getSizeInBytes()), result, false);	
		result.append("}, \"indices\": [");
		Map<String, IndexStats> indices = indicesStatsResponse.getIndices();
		boolean firstEntry = true;
		for (Entry<String, IndexStats> entry : indices.entrySet()) {
			if (!firstEntry) {
				result.append(",");
			}
			result.append("{");
			addStringElementToJsonBuffer("name", entry.getKey(), result, true);
			addLongElementToJsonBuffer("document_count", entry.getValue().getTotal().docs.getCount(), result, false);	
			addStringElementToJsonBuffer("document_count_as_string", numberFormat.format(entry.getValue().getTotal().docs.getCount()), result, false);	
			addLongElementToJsonBuffer("size_in_bytes", entry.getValue().getTotal().store.getSizeInBytes(), result, false);	
			addStringElementToJsonBuffer("size_in_bytes_as_string", numberFormat.format(entry.getValue().getTotal().store.getSizeInBytes()), result, false);			

			long count = entry.getValue().getTotal().indexing.getTotal().getIndexCount();
			if (count != 0) {
				long averageIndexTime = entry.getValue().getTotal().indexing.getTotal().getIndexTime().millis() / count;
				addLongElementToJsonBuffer("average_index_time", averageIndexTime, result, false);	
				addStringElementToJsonBuffer("average_index_time_as_string", numberFormat.format(averageIndexTime), result, false);	
			}
			
			count = entry.getValue().getTotal().search.getTotal().getQueryCount();
			if (count != 0) {
				long averageSearchTime = entry.getValue().getTotal().search.getTotal().getQueryTimeInMillis() / count;
				addLongElementToJsonBuffer("average_search_time", averageSearchTime, result, false);	
				addStringElementToJsonBuffer("average_search_time_as_string", numberFormat.format(averageSearchTime), result, false);	
			}
			
			result.append("}");
			firstEntry = false;
		}
		result.append("]}");
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
				.setWaitForActiveShards(getActiveShardCount(etmConfiguration));
		bulkRequestBuilder.add(client.prepareDelete(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_PARSER, parserName)
				.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
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
				.setWaitForActiveShards(getActiveShardCount(etmConfiguration));
		// Do a read and write of the parser to make sure it's valid.
		ExpressionParser expressionParser = this.expressionParserConverter.read(json);
		bulkRequestBuilder.add(client.prepareUpdate(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_PARSER, parserName)
			.setDoc(this.expressionParserConverter.write(expressionParser))
			.setDocAsUpsert(true)
			.setDetectNoop(true)
			.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
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
			.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
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
		.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
		.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
		.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount());		
	}
	
	@GET
	@Path("/userroles")
	@Produces(MediaType.APPLICATION_JSON)	
	public String getUserRoles() {
		StringBuilder result = new StringBuilder();
		boolean first = true;
		result.append("{\"user_roles\": [");
		for (EtmPrincipalRole role : EtmPrincipalRole.values()) {
			if (!first) {
				result.append(",");
			}
			result.append("{");
			addStringElementToJsonBuffer("name", role.getRoleName(), result, true);
			addStringElementToJsonBuffer("value", role.getRoleName(), result, false);
			result.append("}");
			first = false;
		}
		result.append("]}");
		return result.toString();
	}
	
	@GET
	@Path("/users")
	@Produces(MediaType.APPLICATION_JSON)	
	public String getUsers() {
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticSearchLayout.CONFIGURATION_INDEX_NAME)
			.setTypes(ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER)
			.setFetchSource(new String[] {"*"}, new String[] {this.etmPrincipalTags.getPasswordHashTag(), this.etmPrincipalTags.getSearchTemplatesTag(), this.etmPrincipalTags.getSearchHistoryTag()})
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
	
	@DELETE
	@Path("/user/{userId}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String deleteUser(@PathParam("userId") String userId) {
		EtmPrincipal principal = loadPrincipal(userId);
		if (principal == null) {
			return null;
		}
		if (principal.isInRole(EtmPrincipalRole.ADMIN)) {
			// The user was admin. Check if he/she is the latest admin. If so, block this operation because we don't want a user without the admin role.
			// This check should be skipped/changed when LDAP is supported.
			if (getNumberOfUsersWithAdminRole() <= 1) {
				throw new EtmException(EtmException.NO_MORE_ADMINS_LEFT);
			}
		}
		BulkRequestBuilder bulkDelete = client.prepareBulk()
			.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		bulkDelete.add(client.prepareDelete(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, userId));
		bulkDelete.add(client.prepareDelete(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_GRAPH, userId));
		bulkDelete.add(client.prepareDelete(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_DASHBOARD, userId));
		bulkDelete.get();
		return "{\"status\":\"success\"}";
	}

	@PUT
	@Path("/user/{userId}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String addUser(@PathParam("userId") String userId, String json) {
		// TODO pagina geeft geen error als een nieuwe gebruiker wordt opgevoerd en geen wachtwoorden worden in gegeven.
		// Do a read and write of the user to make sure it's valid.
		Map<String, Object> valueMap = toMap(json);
		EtmPrincipal newPrincipal = loadPrincipal(valueMap);
		String newPassword = getString("new_password", valueMap);
		if (newPassword != null) {
			newPrincipal.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
		}
		EtmPrincipal currentPrincipal = loadPrincipal(userId);
		if (currentPrincipal != null && currentPrincipal.isInRole(EtmPrincipalRole.ADMIN) && !newPrincipal.isInRole(EtmPrincipalRole.ADMIN)) {
			// The user was admin, but that role is revoked. Check if he/she is the latest admin. If so, block this operation because we don't want a user without the admin role.
			// This check should be skipped/changed when LDAP is supported.
			if (getNumberOfUsersWithAdminRole() <= 1) {
				throw new EtmException(EtmException.NO_MORE_ADMINS_LEFT);
			} 
		}
		client.prepareUpdate(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, userId)
			.setDoc(this.etmPrincipalConverter.writePrincipal(newPrincipal))
			.setDocAsUpsert(true)
			.setDetectNoop(true)
			.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
			.get();
		if (currentPrincipal == null && etmConfiguration.getMaxSearchTemplateCount() >= 3 && newPrincipal.isInAnyRole(EtmPrincipalRole.ADMIN, EtmPrincipalRole.SEARCHER)) {
			// Add some default templates to the user if he/she is able to search.
			StringBuilder templates = new StringBuilder();
			templates.append("{\"search_templates\":[");
			templates.append(new SearchRequestParameters("endpoints.writing_endpoint_handler.handling_time: [now-1h TO now]").toJsonSearchTemplate("Events of last 60 mins"));
			templates.append("," + new SearchRequestParameters("endpoints.writing_endpoint_handler.handling_time: [now/d TO now/d]").toJsonSearchTemplate("Events of today"));
			templates.append("," + new SearchRequestParameters("endpoints.writing_endpoint_handler.handling_time: [now-1d/d TO now-1d/d]").toJsonSearchTemplate("Events of yesterday"));
			templates.append("]}");
			client.prepareUpdate(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, userId)
				.setDoc(templates.toString())
				.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
				.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
				.get();		}
		if (userId.equals(getEtmPrincipal().getId())) {
			getEtmPrincipal().forceReload = true;
		}
		return "{ \"status\": \"success\" }";
	}
	
	@GET
	@Path("/groups")
	@Produces(MediaType.APPLICATION_JSON)	
	public String getGroups() {
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticSearchLayout.CONFIGURATION_INDEX_NAME)
			.setTypes(ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_GROUP)
			.setFetchSource(true)
			.setQuery(QueryBuilders.matchAllQuery())
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
		if (!scrollableSearch.hasNext()) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		result.append("{\"groups\": [");
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
	@Path("/group/{groupName}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String deleteGroup(@PathParam("groupName") String groupName) {
		List<String> adminGroups = getGroupsWithRole(EtmPrincipalRole.ADMIN);
		if (adminGroups.contains(groupName)) {
			// Check if there are admins left if this group is removed.
			// This check should be skipped/changed when LDAP is supported.
			adminGroups.remove(groupName);
			if (getNumberOfUsersWithAdminRole(adminGroups) < 1) {
				throw new EtmException(EtmException.NO_MORE_ADMINS_LEFT);
			}
		}		
		BulkRequestBuilder bulkRequestBuilder = client.prepareBulk()
				.setWaitForActiveShards(getActiveShardCount(etmConfiguration));
		bulkRequestBuilder.add(client.prepareDelete(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_GROUP, groupName)
				.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
		);
		removeGroupFromPrincipal(bulkRequestBuilder, groupName);
		bulkRequestBuilder.get();
		// Force a reload of the principal if he/she is in the deleted group.
		EtmPrincipal principal = getEtmPrincipal();
		if (principal.isInGroup(groupName)) {
			principal.forceReload = true;
		}
		return "{\"status\":\"success\"}";
	}
	
	private void removeGroupFromPrincipal(BulkRequestBuilder bulkRequestBuilder, String groupName) {
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticSearchLayout.CONFIGURATION_INDEX_NAME)
				.setTypes(ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER)
				.setFetchSource(false)
				.setQuery(QueryBuilders.termQuery(this.etmPrincipalTags.getGroupsTag(), groupName))
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
		for (SearchHit searchHit : scrollableSearch) {
			boolean updated = false;
			EtmPrincipal principal = loadPrincipal(searchHit.getId());
			if (principal.isInGroup(groupName)) {
				Iterator<EtmGroup> it = principal.getGroups().iterator();
				while (it.hasNext()) {
					EtmGroup group = it.next();
					if (group.getName().equals(groupName)) {
						it.remove();
						updated = true;
					}
				}
			}
			if (updated) {
				bulkRequestBuilder.add(createPrincipalUpdateRequest(principal));
			}
		}
	}
	
	private UpdateRequestBuilder createPrincipalUpdateRequest(EtmPrincipal principal) {
		return client.prepareUpdate(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, principal.getId())
		.setDoc(this.etmPrincipalConverter.writePrincipal(principal))
		.setDocAsUpsert(true)
		.setDetectNoop(true)
		.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
		.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
		.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount());		
	}


	@PUT
	@Path("/group/{groupName}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String addGroup(@PathParam("groupName") String groupName, String json) {
		// Do a read and write of the group to make sure it's valid.
		Map<String, Object> valueMap = toMap(json);
		EtmGroup newGroup = this.etmPrincipalConverter.readGroup(valueMap);
		
		EtmGroup currentGroup = loadGroup(groupName);
		if (currentGroup != null && currentGroup.isInRole(EtmPrincipalRole.ADMIN) && !newGroup.isInRole(EtmPrincipalRole.ADMIN)) {
			// The group had the admin role, but that role is revoked. Check if this removes all the admins. If so, block this operation because we don't want a user without the admin role.
			// This check should be skipped/changed when LDAP is supported.
			if (getNumberOfUsersWithAdminRole() <= 1) {
				throw new EtmException(EtmException.NO_MORE_ADMINS_LEFT);
			}
		}
		client.prepareUpdate(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_GROUP, groupName)
			.setDoc(this.etmPrincipalConverter.writeGroup(newGroup))
			.setDocAsUpsert(true)
			.setDetectNoop(true)
			.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
			.get();
		// Force a reload of the principal if he/she is in the deleted group.
		EtmPrincipal principal = getEtmPrincipal();
		if (principal.isInGroup(groupName)) {
			principal.forceReload = true;
		}
		return "{ \"status\": \"success\" }";
	}
	
	/**
	 * Load an <code>EtmPrincipal</code> based on the user id.
	 * 
	 * @param userId
	 *            The id of the user to load.
	 * @return A fully loaded <code>EtmPrincipal</code>, or <code>null</code> if no user with the given userId exists.
	 */
	private EtmPrincipal loadPrincipal(String userId) {
		GetResponse getResponse = client.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, userId).get();
		if (!getResponse.isExists()) {
			return null;
		}
		return loadPrincipal(getResponse.getSourceAsMap());
	}
	
	/**
	 * Converts a map with json key/value pairs to an <code>EtmPrincipal</code>
	 * and reads the corresponding groups from the database.
	 * 
	 * @param principalValues
	 *            The map with json key/value pairs.
	 * @return A fully loaded <code>EtmPrincipal</code>
	 */
	private EtmPrincipal loadPrincipal(Map<String, Object> principalValues) {
		EtmPrincipal principal = this.etmPrincipalConverter.readPrincipal(principalValues);
		Collection<String> groups = getArray(this.etmPrincipalTags.getGroupsTag(), principalValues);
		if (groups != null && !groups.isEmpty()) {
			MultiGetRequestBuilder multiGetBuilder = client.prepareMultiGet();
			for (String group : groups) {
				multiGetBuilder.add(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_GROUP, group);
			}
			MultiGetResponse multiGetResponse = multiGetBuilder.get();
			Iterator<MultiGetItemResponse> iterator = multiGetResponse.iterator();
			while (iterator.hasNext()) {
				MultiGetItemResponse item = iterator.next();
				EtmGroup etmGroup = this.etmPrincipalConverter.readGroup(item.getResponse().getSourceAsString());
				principal.addGroup(etmGroup);
			}
		}
		return principal;		
	}
	
	/**
	 * Load an <code>EtmGroup</code> based on the group name.
	 * @param groupName The name of the group.
	 * @return The <code>EtmGroup</code> with the given name, or <code>null</code> when no such group exists.
	 */
	private EtmGroup loadGroup(String groupName) {
		GetResponse getResponse = client.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_GROUP, groupName).get();
		if (!getResponse.isExists()) {
			return null;
		}
		return this.etmPrincipalConverter.readGroup(getResponse.getSourceAsMap());
	}
	
	/**
	 * Gives a list with group names that have a gives role.
	 * 
	 * @param roles
	 *            The roles to search for.
	 * @return A list with group names that have one of the given roles, or an
	 *         empty list if non of the groups has any of the given roles.
	 */
	private List<String> getGroupsWithRole(EtmPrincipalRole... roles) {
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticSearchLayout.CONFIGURATION_INDEX_NAME)
				.setTypes(ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_GROUP)
				.setFetchSource(true)
				.setQuery(QueryBuilders.termsQuery(this.etmPrincipalTags.getRolesTag(), Arrays.stream(roles).map(c -> c.getRoleName()).collect(Collectors.toList())))
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
		List<String> groups = new ArrayList<>();
		if (scrollableSearch.hasNext()) {
			for (SearchHit searchHit : scrollableSearch) {
				String group = searchHit.getId();
				if (!groups.contains(group)) {
					groups.add(group);
				}
			}
		}		
		return groups;
	}
	
	/**
	 * Gives the number of users with the admin role. This is the number of
	 * users with the direct admin role added with the users that are in a group
	 * with the admin role.
	 * 
	 * @return The number of users with the admin role.
	 */
	private long getNumberOfUsersWithAdminRole() {
		List<String> adminGroups = getGroupsWithRole(EtmPrincipalRole.ADMIN);
		return getNumberOfUsersWithAdminRole(adminGroups);
	}

	/**
	 * Gives the number of users with the admin role for a given collection of
	 * admin groups. This is the number of users with the direct admin role
	 * added with the users that are in a group with the admin role.
	 * 
	 * @return The number of users with the admin role.
	 */
	private long getNumberOfUsersWithAdminRole(Collection<String> adminGroups) {
		QueryBuilder query = null;
		if (adminGroups == null || adminGroups.isEmpty()) {
			query = QueryBuilders.termsQuery(this.etmPrincipalTags.getRolesTag(), EtmPrincipalRole.ADMIN.getRoleName());
		} else {
			query = QueryBuilders.boolQuery()
					.should(QueryBuilders.termsQuery(this.etmPrincipalTags.getRolesTag(), EtmPrincipalRole.ADMIN.getRoleName()))
					.should(QueryBuilders.termsQuery(this.etmPrincipalTags.getGroupsTag(), adminGroups))
					.minimumNumberShouldMatch(1);
		}
		SearchResponse response = client.prepareSearch(ElasticSearchLayout.CONFIGURATION_INDEX_NAME)
				.setTypes(ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER)
				.setFetchSource(false)
				.setSize(0)
				.setQuery(query)
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
				.get();
		return response.getHits().getTotalHits();		
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
