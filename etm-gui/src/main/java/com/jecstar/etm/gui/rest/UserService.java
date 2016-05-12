package com.jecstar.etm.gui.rest;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
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

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;

import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.converter.EtmPrincipalConverterTags;
import com.jecstar.etm.core.domain.converter.json.EtmPrincipalConverterTagsJsonImpl;
import com.jecstar.etm.core.util.BCrypt;

@Path("/user")
public class UserService extends AbstractJsonService {

	private static Client client;
	private static EtmConfiguration etmConfiguration;
	private final String timezoneResponse;
	private final EtmPrincipalConverterTags tags = new EtmPrincipalConverterTagsJsonImpl();
	
	public static void initialize(Client client, EtmConfiguration etmConfiguration) {
		UserService.client = client;
		UserService.etmConfiguration = etmConfiguration;
	}
	
	public UserService() {
		this.timezoneResponse = "{\"time_zones\": [" + Arrays.stream(TimeZone.getAvailableIDs()).map(tz -> escapeToJson(tz, true)).collect(Collectors.joining(",")) + "]}";
	}
	
	@GET
	@Path("/settings")
	@Produces(MediaType.APPLICATION_JSON)
	public String getUserSettings() {
		GetResponse getResponse = UserService.client.prepareGet("etm_configuration", "user", getEtmPrincipal().getId())
				.setFetchSource(null, new String[] {"searchtemplates"})
				.get();
		if (getResponse.isSourceEmpty()) {
			return "{}";
		}
		return getResponse.getSourceAsString();
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
		
		UserService.client.prepareUpdate("etm_configuration", "user", getEtmPrincipal().getId())
		.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()))
		.setDoc(updateMap)
		.setDetectNoop(true)
		.setRetryOnConflict(3)
		.get();
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
		
		UserService.client.prepareUpdate("etm_configuration", "user", getEtmPrincipal().getId())
		.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()))
		.setDoc(updateMap)
		.setDetectNoop(true)
		.setRetryOnConflict(3)
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
				.collect(Collectors.joining(",")) + "]}";
	}
	
	
}
