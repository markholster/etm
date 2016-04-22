package com.jecstar.etm.gui.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.elasticsearch.client.Client;

import com.jecstar.etm.core.configuration.EtmConfiguration;

public class RestGuiApplication extends Application {

	public RestGuiApplication(Client client, EtmConfiguration etmConfiguration) {
		SearchService.initialize(client, etmConfiguration);
		UserService.initialize(client);
	}


	@Override
	public Set<Class<?>> getClasses() {
		HashSet<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(SearchService.class);
		classes.add(UserService.class);
		return classes;
	}
}
