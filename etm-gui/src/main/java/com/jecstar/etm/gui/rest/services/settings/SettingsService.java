package com.jecstar.etm.gui.rest.services.settings;

import javax.ws.rs.Path;

import org.elasticsearch.client.Client;

import com.jecstar.etm.server.core.configuration.EtmConfiguration;

@Path("/settings")
public class SettingsService {

	private static Client client;
	private static EtmConfiguration etmConfiguration;
	
	public static void initialize(Client client, EtmConfiguration etmConfiguration) {
		SettingsService.client = client;
		SettingsService.etmConfiguration = etmConfiguration;
	}
}
