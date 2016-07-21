package com.jecstar.etm.gui.rest.services.dashboard;

import javax.ws.rs.Path;

import org.elasticsearch.client.Client;

import com.jecstar.etm.gui.rest.AbstractJsonService;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;

@Path("/dashboard")
public class DashboardService extends AbstractJsonService {

	private static Client client;
	private static EtmConfiguration etmConfiguration;
	
	public static void initialize(Client client, EtmConfiguration etmConfiguration) {
		DashboardService.client = client;
		DashboardService.etmConfiguration = etmConfiguration;
	}
}
