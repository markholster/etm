package com.jecstar.etm.gui.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.elasticsearch.client.Client;

public class RestGuiApplication extends Application {

	public RestGuiApplication(Client client) {
		StatisticsService.setClient(client);
	}

	@Override
	public Set<Class<?>> getClasses() {
		HashSet<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(StatisticsService.class);
		return classes;
	}
}
