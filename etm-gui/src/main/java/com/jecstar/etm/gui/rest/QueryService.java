package com.jecstar.etm.gui.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.gui.rest.repository.QueryRepository;
import com.jecstar.etm.jee.configurator.core.GuiConfiguration;

@Path("/query")
public class QueryService {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(QueryService.class);

	
	@GuiConfiguration
	@Inject
	private Client elasticClient;
	
	@Inject
	private QueryRepository queryRepository;

	private final JsonFactory jsonFactory = new JsonFactory();
	
	@GET
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String performUnformattedQuery(@QueryParam("queryString") String queryString ,@QueryParam("start") int start, @QueryParam("rows") int rows, @QueryParam("sortField") String sortField, @QueryParam("sortOrder") String sortOrder) {
		if (rows <= 0) {
			rows = 25;
		} else if (rows > 1000) {
			rows = 1000;
		}
		SearchRequestBuilder searchRequest = this.elasticClient.prepareSearch("etm_event_all")
				.setFrom(start)
				.setSize(rows)
				.setQuery(QueryBuilders.queryStringQuery(queryString).defaultField("content").lowercaseExpandedTerms(false));
		if (sortField != null && sortField.trim().length() > 0) {
			if ("asc".equalsIgnoreCase(sortOrder)) {
				searchRequest.addSort(sortField, SortOrder.ASC);
			} else {
				searchRequest.addSort(sortField, SortOrder.DESC);
			}
		}
		try {
			long startTime = System.nanoTime();
			SearchResponse searchResponse = searchRequest.get();
	        StringWriter writer = new StringWriter();
	        JsonGenerator generator = this.jsonFactory.createGenerator(writer);
	        generator.writeStartObject();
	        generator.writeNumberField("numFound", searchResponse.getHits().getTotalHits());
	        generator.writeNumberField("numReturned", searchResponse.getHits().getHits().length);
	        generator.writeNumberField("start", start);
	        generator.writeNumberField("end", start + searchResponse.getHits().getHits().length - 1);
	        generator.writeArrayFieldStart("events");
	        this.queryRepository.addEvents(searchResponse.getHits(), generator);
	        generator.writeEndArray();
	        generator.writeNumberField("queryTime", TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS));
	        generator.writeEndObject();
	        generator.close();
	        return writer.toString();
        } catch (IOException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Error executing query '" + queryString + "'.", e);
        	}
        	throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
	}
	
	@GET
	@Path("/event/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getEventById(@PathParam("id") String id) {
		try {
	        StringWriter writer = new StringWriter();
	        JsonGenerator generator = this.jsonFactory.createGenerator(writer);
	        generator.writeStartObject();
	        this.queryRepository.addEvent(id, generator);
	        generator.writeEndObject();
	        generator.close();
	        return writer.toString();
		} catch (Exception e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Error retrieving event data with id '" + id + "'.", e);
        	}
		}
		return null;
	}

	
	@GET
	@Path("/event/{id}/overview")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getEventOverviewById(@PathParam("id") String id) {
		try {
	        StringWriter writer = new StringWriter();
	        JsonGenerator generator = this.jsonFactory.createGenerator(writer);
	        generator.writeStartObject();
	        this.queryRepository.addEventOverview(id, generator);
	        generator.writeEndObject();
	        generator.close();
	        return writer.toString();
		} catch (Exception e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Error retrieving event overview data for event with id '" + id + "'.", e);
        	}
		}
		return null;
	}

}
