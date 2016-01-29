package com.jecstar.etm.gui.rest;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.client.Client;


@Path("/stats")
public class StatisticsService {

	private static Client client;
	
	public static void setClient(Client client) {
		StatisticsService.client = client;
	}
	
	
	@GET
	@Path("/test")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed("tester")
	public String lala() {
		return "{\"status\": \"ok\"}";
	}

}
