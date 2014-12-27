package com.holster.etm.gui.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

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
	public String transaction(@PathParam("starttime") Long startTime, @PathParam("endtime") Long endTime) {
		Map<String, Map<Long, Long>> statistics = this.statisticsRepository.getTransactionStatistics(startTime, endTime, 5);
		Writer writer = new StringWriter();
		try {
	        JsonGenerator generator = this.jsonFactory.createJsonGenerator(writer);
	        for (String name : statistics.keySet()) {
	        	
	        	generator.writeStartObject();
	        	generator.writeStringField("name", name);
	        	generator.writeArrayFieldStart("data");
	        	Map<Long, Long> values = statistics.get(name);
	        	for (long time : values.keySet()) {
	        		generator.writeStartArray();
	        		generator.writeNumber(time);
	        		generator.writeNumber(values.get(time));
	        		generator.writeStartArray();
	        	}
	        	generator.writeEndArray();
	        	generator.writeEndObject();
	        }
        } catch (IOException e) {
        }
		return writer.toString();
	}

}
