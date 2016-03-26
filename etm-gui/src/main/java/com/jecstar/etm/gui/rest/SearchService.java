package com.jecstar.etm.gui.rest;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService.ScriptType;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.EtmPrincipal;
import com.jecstar.etm.core.domain.converter.json.AbstractJsonConverter;

@Path("/search")
public class SearchService extends AbstractJsonConverter {

	private static Client client;
	private static EtmConfiguration etmConfiguraton;
	
    @Context
    private SecurityContext securityContext;
	
	public static void initialize(Client client, EtmConfiguration etmConfiguration) {
		SearchService.client = client;
		SearchService.etmConfiguraton = etmConfiguration;
	}
	
	@GET
	@Path("/searchtemplates")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed("searcher")
	public String getSearchTemplates() {
		GetResponse getResponse = SearchService.client.prepareGet("etm_configuration", "user", ((EtmPrincipal)this.securityContext.getUserPrincipal()).getId())
				.setFetchSource("searchtemplates", null)
				.get();
		if (getResponse.isSourceEmpty()) {
			return "{}";
		}
		return getResponse.getSourceAsString();
	}
	
	@PUT
	@Path("/searchtemplates/{templateName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed("searcher")
	public String setSearchTemplates(@PathParam("templateName") String templateName, String json) {
		Map<String, Object> requestValues = toMap(json); 
		Map<String, Object> scriptParams = new HashMap<String, Object>();
		Map<String, Object> template = new HashMap<String, Object>();
		template.put("name", templateName);
		template.put("query", requestValues.get("query"));
		
		scriptParams.put("template", template);
		SearchService.client.prepareUpdate("etm_configuration", "user", ((EtmPrincipal)this.securityContext.getUserPrincipal()).getId())
				.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguraton.getWriteConsistency().name()))
				.setScript(new Script("etm_update-searchtemplate", ScriptType.FILE, "groovy", scriptParams))
				.setRetryOnConflict(3)
				.get();
		return "{ \"status\": \"success\" }";
	}


}
