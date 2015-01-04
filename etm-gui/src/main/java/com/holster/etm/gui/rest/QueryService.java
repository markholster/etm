package com.holster.etm.gui.rest;

import java.io.IOException;
import java.io.StringWriter;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import com.holster.etm.gui.rest.repository.QueryRepository;
import com.holster.etm.jee.configurator.core.GuiConfiguration;

@Path("/query")
public class QueryService {
	
	@GuiConfiguration
	@Inject
	private SolrServer solrServer;
	
	@Inject
	private QueryRepository queryRepository;

	private final JsonFactory jsonFactory = new JsonFactory();
	
	@GET
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String performUnformattedQuery(@QueryParam("queryString") String queryString ,@QueryParam("start") int start, @QueryParam("rows") int rows) {
		if (rows <= 0) {
			rows = 25;
		} else if (rows > 1000) {
			rows = 1000;
		}
		SolrQuery query = new SolrQuery(queryString);
		query.setStart(start);
		query.setSort("creationTime", ORDER.desc);
		query.setRows(rows);
		try {
	        SolrDocumentList results = this.solrServer.query(query).getResults();
	        StringWriter writer = new StringWriter();
	        JsonGenerator generator = this.jsonFactory.createJsonGenerator(writer);
	        generator.writeStartObject();
	        generator.writeNumberField("numFound", results.getNumFound());
	        generator.writeNumberField("start", results.getStart());
	        generator.writeArrayFieldStart("events");
	        this.queryRepository.addEvents(results, generator);
	        generator.writeEndArray();
	        generator.writeEndObject();
	        generator.close();
	        return writer.toString();
        } catch (SolrServerException e) {
	        // TODO Logging
        } catch (IOException e) {
        	// TODO Logging
        }
		return "{}";
	}
}
