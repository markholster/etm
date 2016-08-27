package com.jecstar.etm.gui.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.elasticsearch.client.Client;

import com.jecstar.etm.gui.rest.services.dashboard.DashboardService;
import com.jecstar.etm.gui.rest.services.search.SearchService;
import com.jecstar.etm.gui.rest.services.settings.SettingsService;
import com.jecstar.etm.gui.rest.services.user.UserService;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;

public class RestGuiApplication extends Application {
	
	private static boolean iibProxyOnClasspath;
	
	static {
		try {
			Class.forName("com.ibm.broker.config.proxy.BrokerProxy");
			iibProxyOnClasspath = true;
		} catch (ClassNotFoundException e) {
			iibProxyOnClasspath = false;
		}
	}

	public RestGuiApplication(Client client, EtmConfiguration etmConfiguration) {
		SearchService.initialize(client, etmConfiguration);
		UserService.initialize(client, etmConfiguration);
		SettingsService.initialize(client, etmConfiguration);
		DashboardService.initialize(client, etmConfiguration);
		if (iibProxyOnClasspath) {
			com.jecstar.etm.gui.rest.services.iib.IIBService.initialize(client, etmConfiguration);
		}
	}


	@Override
	public Set<Class<?>> getClasses() {
		HashSet<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(SearchService.class);
		classes.add(UserService.class);
		classes.add(SettingsService.class);
		classes.add(DashboardService.class);
		if (iibProxyOnClasspath) {
			classes.add(com.jecstar.etm.gui.rest.services.iib.IIBService.class);
		}
		return classes;
	}
}
