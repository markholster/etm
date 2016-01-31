package com.jecstar.etm.gui.rest;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.elasticsearch.client.Client;


@Path("/stats")
public class StatisticsService {

	private static Client client;
	
    @Context
    private SecurityContext securityContext;
	
	public static void setClient(Client client) {
		StatisticsService.client = client;
	}
	
	
	@GET
	@Path("/test")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed("searcher")
	public String lala() {
		return "{\"status\": \"" + this.securityContext.getUserPrincipal().getName() + "\"}";
	}

}
