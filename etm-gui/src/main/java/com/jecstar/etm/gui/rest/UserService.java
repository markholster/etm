package com.jecstar.etm.gui.rest;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;

import com.jecstar.etm.core.domain.EtmPrincipal;

@Path("/user")
public class UserService extends AbstractJsonService {

	private static Client client;
	private final String timezoneResponse;
	
	public static void initialize(Client client) {
		UserService.client = client;
	}
	
	public UserService() {
		this.timezoneResponse = "{\"time_zones\": [" + Arrays.stream(TimeZone.getAvailableIDs()).map(tz -> "\"" + escapeToJson(tz) + "\"").collect(Collectors.joining(",")) + "]}";
	}
	
	@GET
	@Path("/settings")
	@Produces(MediaType.APPLICATION_JSON)
	public String getUserSettings() {
		GetResponse getResponse = UserService.client.prepareGet("etm_configuration", "user", ((EtmPrincipal)this.securityContext.getUserPrincipal()).getId())
				.setFetchSource(null, new String[] {"searchtemplates", "password_hash"})
				.get();
		if (getResponse.isSourceEmpty()) {
			return "{}";
		}
		return getResponse.getSourceAsString();
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
			}}).map(l -> "{\"name\": \"" + escapeToJson(l.getDisplayName(requestedLocale)) + "\", \"value\": \"" +l.toLanguageTag() + "\"}")
				.collect(Collectors.joining(",")) + "]}";
	}
	
	
}
