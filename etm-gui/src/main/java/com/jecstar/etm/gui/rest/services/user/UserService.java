package com.jecstar.etm.gui.rest.services.user;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

import com.jecstar.etm.gui.rest.AbstractJsonService;
import com.jecstar.etm.gui.rest.IIBApi;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.EtmPrincipal;
import com.jecstar.etm.server.core.domain.EtmPrincipalRole;
import com.jecstar.etm.server.core.domain.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.converter.json.EtmPrincipalTagsJsonImpl;
import com.jecstar.etm.server.core.util.BCrypt;

@Path("/user")
public class UserService extends AbstractJsonService {
	
	private static Client client;
	private static EtmConfiguration etmConfiguration;
	private final String timezoneResponse;
	private final EtmPrincipalTags tags = new EtmPrincipalTagsJsonImpl();
	
	
	public static void initialize(Client client, EtmConfiguration etmConfiguration) {
		UserService.client = client;
		UserService.etmConfiguration = etmConfiguration;
	}
	
	public UserService() {
		this.timezoneResponse = "{\"time_zones\": [" + Arrays.stream(TimeZone.getAvailableIDs()).map(tz -> escapeToJson(tz, true)).collect(Collectors.joining(",")) + "], " + escapeObjectToJsonNameValuePair("default_time_zone", TimeZone.getDefault().getID()) + "}";
	}
	
	@GET
	@Path("/settings")
	@Produces(MediaType.APPLICATION_JSON)
	public String getUserSettings() {
		GetResponse getResponse = UserService.client.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, getEtmPrincipal().getId())
				.setFetchSource(null, new String[] {"searchtemplates", "dashboards", "password_hash"})
				.get();
		if (getResponse.isSourceEmpty()) {
			return "{}";
		}
		// Hack the max search history into the result. Dunno how to do this better.
		StringBuilder result = new StringBuilder(getResponse.getSourceAsString().substring(0, getResponse.getSourceAsString().lastIndexOf("}")));
		addIntegerElementToJsonBuffer("max_" + this.tags.getSearchHistorySizeTag(), etmConfiguration.getMaxSearchHistoryCount(), result, false);
		result.append("}");
		return result.toString();
	}
	
	@PUT
	@Path("/settings")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String setUserSettings(String json) throws Exception {
		Map<String, Object> valueMap = toMap(json);
		
		Map<String, Object> updateMap = new HashMap<String, Object>();
		updateMap.put(this.tags.getNameTag(), valueMap.get(this.tags.getNameTag()));
		updateMap.put(this.tags.getTimeZoneTag(), valueMap.get(this.tags.getTimeZoneTag()));
		updateMap.put(this.tags.getLocaleTag(), valueMap.get(this.tags.getLocaleTag()));
		Integer newHistorySize = getInteger(this.tags.getSearchHistorySizeTag(), valueMap, EtmPrincipal.DEFAULT_HISTORY_SIZE);
		if (newHistorySize > etmConfiguration.getMaxSearchHistoryCount()) {
			newHistorySize = etmConfiguration.getMaxSearchHistoryCount();
		}
		updateMap.put(this.tags.getSearchHistorySizeTag(), newHistorySize);
		
		EtmPrincipal etmPrincipal = getEtmPrincipal();
		UserService.client.prepareUpdate(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, etmPrincipal.getId())
		.setDoc(updateMap)
		.setDetectNoop(true)
		.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
		.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
		.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
		.get();
		
		if (newHistorySize < etmPrincipal.getHistorySize()) {
			// History size is smaller. Make sure the stored queries are sliced to the new size.
			Map<String, Object> scriptParams = new HashMap<String, Object>();
			scriptParams.put("history_size", newHistorySize);
			UserService.client.prepareUpdate(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, getEtmPrincipal().getId())
					.setScript(new Script(ScriptType.STORED, "painless", "etm_update-search-history", scriptParams))
					.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
					.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
					.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
					.execute();
		}
		etmPrincipal.forceReload = true;
		return "{ \"status\": \"success\" }";
	}
	
	@PUT
	@Path("/password")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String setPassword(String json) throws Exception {
		// TODO Deze functie mag niet beschikbaar zijn als er LDAP authenticatie gebruikt wordt.
		Map<String, Object> valueMap = toMap(json);
		
		String oldPassword = getString("current_password", valueMap);
		String newPassword = getString("new_password", valueMap);
				
		boolean valid = BCrypt.checkpw(oldPassword, getEtmPrincipal().getPasswordHash());
		if (!valid) {
			throw new EtmException(EtmException.INVALID_PASSWORD);
		}
		String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
		Map<String, Object> updateMap = new HashMap<String, Object>();
		updateMap.put(this.tags.getPasswordHashTag(), newHash);
		
		UserService.client.prepareUpdate(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, getEtmPrincipal().getId())
		.setDoc(updateMap)
		.setDetectNoop(true)
		.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
		.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
		.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
		.get();
		return "{ \"status\": \"success\" }";
	}
	
	@GET
	@Path("/timezones")
	@Produces(MediaType.APPLICATION_JSON)
	public String getTimeZones() {
		return this.timezoneResponse;
	}
	
	@GET
	@Path("/locales")
	@Produces(MediaType.APPLICATION_JSON)
	public String getLocales() {
		Locale requestedLocale = getEtmPrincipal().getLocale();
		return "{\"locales\": [" + Arrays.stream(Locale.getAvailableLocales()).filter(p -> p.getCountry().length() > 0).sorted(new Comparator<Locale>() {
			@Override
			public int compare(Locale o1, Locale o2) {
				return o1.getDisplayName(requestedLocale).compareTo(o2.getDisplayName(requestedLocale));
			}}).map(l -> "{\"name\": " + escapeToJson(l.getDisplayName(requestedLocale), true) + ", \"value\": " + escapeToJson(l.toLanguageTag(), true) + "}")
				.collect(Collectors.joining(",")) 
			+ "], \"default_locale\": {" + escapeObjectToJsonNameValuePair("name", Locale.getDefault().getDisplayName(requestedLocale)) 
			+ ", " + escapeObjectToJsonNameValuePair("value", Locale.getDefault().toLanguageTag())+ "}}";
	}
	
	@GET
	@Path("/menu")
	@Produces(MediaType.APPLICATION_JSON)
	public String getMenu() {
		StringBuilder result = new StringBuilder();
		EtmPrincipal principal = getEtmPrincipal();
		result.append("{");
		result.append("\"items\": [");
		boolean added = false;
		if (principal.isInAnyRole(EtmPrincipalRole.ADMIN, EtmPrincipalRole.SEARCHER)) {
			result.append("{");
			added = addStringElementToJsonBuffer("name", "search", result, true) || added;
			result.append("}");
		}
		if (principal.isInAnyRole(EtmPrincipalRole.ADMIN, EtmPrincipalRole.CONTROLLER)) {
			if (added) {
				result.append(",");
			}
			result.append("{");
			added = addStringElementToJsonBuffer("name", "dashboard", result, true) || added;
			GetResponse getResponse = UserService.client.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, getEtmPrincipal().getId())
					.setFetchSource(new String[] {"dashboards"}, null)
					.get();
			if (!getResponse.isSourceEmpty()) {
				List<Map<String, Object>> dashboards = getArray("dashboards", getResponse.getSource());
				if (dashboards != null) {
					result.append(",\"dashboards\": [");
					boolean first = true;
					for (Map<String, Object> dashboardValues : dashboards) {
						String name = getString("name", dashboardValues);
						if (name == null) {
							continue;
						}
						if (!first) {
							result.append(",");
						}
						result.append("\"" + name + "\"");
						first = false;
					}
					result.append("]");
				}
			}
			result.append("}");
		}
		if (principal.isInAnyRole(EtmPrincipalRole.ADMIN, EtmPrincipalRole.CONTROLLER, EtmPrincipalRole.SEARCHER)) {
			if (added) {
				result.append(",");
			}
			result.append("{");
			added = addStringElementToJsonBuffer("name", "preferences", result, true) || added;
			result.append("}");
		}
		if (principal.isInAnyRole(EtmPrincipalRole.ADMIN, EtmPrincipalRole.IIB_ADMIN)) {
			if (added) {
				result.append(",");
			}
			result.append("{");
			added = addStringElementToJsonBuffer("name", "settings", result, true) || added;
			result.append(", \"submenus\": [");
			boolean subMenuAdded = false;
			if (principal.isInRole(EtmPrincipalRole.ADMIN)) {
				result.append("\"admin\"");
				subMenuAdded = true;
			}
			if (principal.isInAnyRole(EtmPrincipalRole.ADMIN, EtmPrincipalRole.IIB_ADMIN) && IIBApi.IIB_PROXY_AVAILABLE) {
				if (subMenuAdded) {
					result.append(",");
				}
				result.append("\"iib_admin\"");
				subMenuAdded = true;
			}
			result.append("]");
			result.append("}");
		}
		result.append("]");
		addBooleanElementToJsonBuffer("license_expired", etmConfiguration.isLicenseExpired(), result, false);
		addBooleanElementToJsonBuffer("license_almost_expired", etmConfiguration.isLicenseAlmostExpired(), result, false);
		result.append("}"); 
		return result.toString();
	}
	
	@GET
	@Path("/eventstats")
	@Produces(MediaType.APPLICATION_JSON)
	public String getEvents() {
		StringBuilder result = new StringBuilder();
		NumberFormat numberFormat = NumberFormat.getInstance(getEtmPrincipal().getLocale());
		SearchResponse searchResponse = client.prepareSearch(ElasticSearchLayout.ETM_EVENT_INDEX_ALIAS_ALL)
			.setSize(0).setQuery(QueryBuilders.matchAllQuery())
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.get();
		result.append("{");
		addLongElementToJsonBuffer("event_count", searchResponse.getHits().getTotalHits(), result, true);
		addStringElementToJsonBuffer("event_count_as_string", numberFormat.format(searchResponse.getHits().getTotalHits()), result, false);
		result.append("}");
		return result.toString();
	}
	
}
