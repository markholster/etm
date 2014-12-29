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

import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;
import com.holster.etm.gui.rest.repository.Average;
import com.holster.etm.gui.rest.repository.StatisticsRepository;

@Path("/statistics")
public class StatisticsService {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(StatisticsService.class);

	
	@Inject
	private StatisticsRepository statisticsRepository;
	
	private final JsonFactory jsonFactory = new JsonFactory();

	@GET
	@Path("/transactions/count/{starttime}/{endtime}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getTransactionsCountFromTimePeriod(@PathParam("starttime") Long startTime, @PathParam("endtime") Long endTime) {
		Map<String, Map<Long, Long>> statistics = this.statisticsRepository.getTransactionCountStatistics(startTime, endTime, 5);
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
	        		generator.writeStartArray();
	        		generator.writeNumber(time);
	        		generator.writeNumber(values.get(time));
	        		generator.writeEndArray();
	        	}
	        	generator.writeEndArray();
	        	generator.writeEndObject();
	        }
	        generator.writeEndArray();
	        generator.close();
        } catch (IOException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Unable to generate statistics count response.", e);
        	}
        }
		return writer.toString();
	}
	
	@GET
	@Path("/transactions/count/{starttime}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getTransactionsCountFromTime(@PathParam("starttime") Long startTime) {
		long endTime = System.currentTimeMillis();
		return getTransactionsCountFromTimePeriod(startTime, endTime);
	}
	
	@GET
	@Path("/transactions/count/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getTransactionsCount() {
		long endTime = System.currentTimeMillis();
		long startTime = endTime - (1000 * 60 * 5);
		return getTransactionsCountFromTimePeriod(startTime, endTime);
	}
	
	@GET
	@Path("/transactions/performance/{starttime}/{endtime}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getTransactionsPerformanceFromTimePeriod(@PathParam("starttime") Long startTime, @PathParam("endtime") Long endTime) {
		Map<String, Map<Long, Average>> statistics = this.statisticsRepository.getTransactionPerformanceStatistics(startTime, endTime, 5);
		StringWriter writer = new StringWriter();
		try {
	        JsonGenerator generator = this.jsonFactory.createJsonGenerator(writer);
	        generator.writeStartArray();
	        for (String name : statistics.keySet()) {
	        	generator.writeStartObject();
	        	generator.writeStringField("name", name);
	        	generator.writeStringField("id", name);
	        	generator.writeArrayFieldStart("data");
	        	Map<Long, Average> values = statistics.get(name);
	        	SortedSet<Long> keys = new TreeSet<Long>(values.keySet());
	        	for (long time: keys) { 
	        		generator.writeStartArray();
	        		generator.writeNumber(time);
	        		generator.writeNumber(values.get(time).getAverage());
	        		generator.writeEndArray();
	        	}
	        	generator.writeEndArray();
	        	generator.writeEndObject();
	        }
	        generator.writeEndArray();
	        generator.close();
        } catch (IOException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Unable to generate statistics performance response.", e);
        	}
        }
		return writer.toString();
	}

	@GET
	@Path("/transactions/performance/{starttime}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getTransactionsPerformanceFromTime(@PathParam("starttime") Long startTime) {
		long endTime = System.currentTimeMillis();
		return getTransactionsPerformanceFromTimePeriod(startTime, endTime);
	}
	
	@GET
	@Path("/transactions/performance/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getTransactionsPerformanceCount() {
		long endTime = System.currentTimeMillis();
		long startTime = endTime - (1000 * 60 * 5);
		return getTransactionsPerformanceFromTimePeriod(startTime, endTime);
	}
}
