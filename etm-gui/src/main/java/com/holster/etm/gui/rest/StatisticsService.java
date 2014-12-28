package com.holster.etm.gui.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import com.holster.etm.gui.rest.repository.StatisticsRepository;

@Path("/statistics")
public class StatisticsService {
	
	@Inject
	private StatisticsRepository statisticsRepository;
	
	private final JsonFactory jsonFactory = new JsonFactory();

	@GET
	@Path("/transactions/{starttime}/{endtime}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getTransactionsFromTimePeriod(@PathParam("starttime") Long startTime, @PathParam("endtime") Long endTime) {
		Map<String, Map<Long, Long>> statistics = this.statisticsRepository.getTransactionStatistics(startTime, endTime, 5);
		StringWriter writer = new StringWriter();
		try {
	        JsonGenerator generator = this.jsonFactory.createJsonGenerator(writer);
	        generator.writeStartArray();
	        for (String name : statistics.keySet()) {
	        	generator.writeStartObject();
	        	generator.writeStringField("name", name);
	        	generator.writeStringField("id", name);
	        	generator.writeArrayFieldStart("data");
	        	Map<Long, Long> values = statistics.get(name);
	        	SortedSet<Long> keys = new TreeSet<Long>(values.keySet());
	        	for (long time: keys) { 
	        		generator.writeStartObject();
	        		generator.writeNumberField("id",time);
	        		generator.writeNumberField("x",time);
	        		generator.writeNumberField("y", values.get(time));
	        		generator.writeEndObject();
	        	}
	        	generator.writeEndArray();
	        	generator.writeEndObject();
	        }
	        generator.writeEndArray();
	        generator.close();
        } catch (IOException e) {
        }
		return writer.toString();
	}
	
	@GET
	@Path("/transactions/{starttime}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getTransactionsFromTime(@PathParam("starttime") Long startTime) {
		long endTime = System.currentTimeMillis();
		return getTransactionsFromTimePeriod(startTime, endTime);
	}
	
	@GET
	@Path("/transactions")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getTransaction() {
		long endTime = System.currentTimeMillis();
		long startTime = endTime - (1000 * 60 * 5);
		return getTransactionsFromTimePeriod(startTime, endTime);
	}

}
