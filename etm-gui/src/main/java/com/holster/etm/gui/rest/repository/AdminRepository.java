package com.holster.etm.gui.rest.repository;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class AdminRepository {

	private final String keyspace;
	private final Session session;
	private final PreparedStatement selectEndpointConfigs;

	public AdminRepository(Session session, String keyspace) {
	    this.session = session;
	    this.keyspace = keyspace;
	    this.selectEndpointConfigs = this.session.prepare("select endpoint, direction, applicationParsers, eventNameParsers, transactionNameParsers, correlationParsers from " + this.keyspace + ".endpoint_config ALLOW FILTERING");
    }

	public void addEndpoingConfigs(JsonGenerator generator) throws JsonGenerationException, IOException {
	    ResultSet resultSet = this.session.execute(this.selectEndpointConfigs.bind());
	    Iterator<Row> rowIterator = resultSet.iterator();
	    while (rowIterator.hasNext()) {
	    	Row row = rowIterator.next();
	    	generator.writeStartObject();
	    	writeStringValue("name", row.getString(0), generator);
	    	writeStringValue("direction", row.getString(1), generator);
	    	generator.writeArrayFieldStart("applicationParsers");
	    	List<String> applicationParsers = row.getList(2, String.class);
	    	for (String applicationParser : applicationParsers) {
	    		generator.writeString(applicationParser);
	    	}
	    	generator.writeEndArray();
	    	generator.writeArrayFieldStart("eventNameParsers");
	    	List<String> eventNameParsers = row.getList(3, String.class);
	    	for (String eventNameParser : eventNameParsers) {
	    		generator.writeString(eventNameParser);
	    	}
	    	generator.writeEndArray();
	    	generator.writeArrayFieldStart("transactionNameParsers");
	    	List<String> transactionNameParsers = row.getList(4, String.class);
	    	for (String transactionNameParser : transactionNameParsers) {
	    		generator.writeString(transactionNameParser);
	    	}
	    	generator.writeEndArray();
	    	generator.writeArrayFieldStart("correlationParsers");
	    	Map<String, String> correlationParsers = row.getMap(5, String.class, String.class);
	    	List<String> keys = correlationParsers.keySet().stream().sorted().collect(Collectors.toList());
	    	for (String key : keys) {
	    		generator.writeStartObject();
	    		generator.writeStringField("key", key);
	    		generator.writeStringField("value", correlationParsers.get(key));
	    		generator.writeEndObject();
	    	}
	    	generator.writeEndArray();
	    	generator.writeEndObject();
	    }
	    
    }

	private void writeStringValue(String fieldName, String fieldValue, JsonGenerator generator) throws JsonGenerationException, IOException {
		if (fieldValue == null) {
			return;
		}
		generator.writeStringField(fieldName, fieldValue);
	}
}
