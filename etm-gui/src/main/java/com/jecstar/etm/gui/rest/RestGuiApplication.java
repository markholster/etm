package com.jecstar.etm.gui.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.elasticsearch.client.Client;

import com.jecstar.etm.gui.rest.services.search.SearchService;
import com.jecstar.etm.gui.rest.services.user.UserService;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;

public class RestGuiApplication extends Application {

	public RestGuiApplication(Client client, EtmConfiguration etmConfiguration) {
		SearchService.initialize(client, etmConfiguration);
		UserService.initialize(client, etmConfiguration);
	}


	@Override
	public Set<Class<?>> getClasses() {
		HashSet<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(SearchService.class);
		classes.add(UserService.class);
		return classes;
	}
}
