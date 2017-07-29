package com.jecstar.etm.gui.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.elasticsearch.client.Client;

import com.jecstar.etm.gui.rest.services.dashboard.DashboardService;
import com.jecstar.etm.gui.rest.services.search.SearchService;
import com.jecstar.etm.gui.rest.services.settings.AuditService;
import com.jecstar.etm.gui.rest.services.settings.SettingsService;
import com.jecstar.etm.gui.rest.services.user.UserService;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;

public class RestGuiApplication extends Application {

	public RestGuiApplication(Client client, EtmConfiguration etmConfiguration) {
		SearchService.initialize(client, etmConfiguration);
		UserService.initialize(client, etmConfiguration);
		SettingsService.initialize(client, etmConfiguration);
		AuditService.initialize(client, etmConfiguration);
		DashboardService.initialize(client, etmConfiguration);
		if (IIBApi.IIB_PROXY_ON_CLASSPATH) {
			com.jecstar.etm.gui.rest.services.iib.IIBService.initialize(client, etmConfiguration);
		}
	}


	@Override
	public Set<Class<?>> getClasses() {
		HashSet<Class<?>> classes = new HashSet<>();
		classes.add(SearchService.class);
		classes.add(UserService.class);
		classes.add(SettingsService.class);
		classes.add(AuditService.class);
		classes.add(DashboardService.class);
		if (IIBApi.IIB_PROXY_ON_CLASSPATH) {
			classes.add(com.jecstar.etm.gui.rest.services.iib.IIBService.class);
		}
		return classes;
	}
}
