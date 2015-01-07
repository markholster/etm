package com.holster.etm.gui.rest.repository;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class QueryRepository {

	private final String keyspace = "etm";
	private Session session;
	private PreparedStatement findEventForSearchResultsStatement;
	private PreparedStatement findEventForDetailsStatement;

	public QueryRepository(Session session) {
		this.session = session;
		this.findEventForSearchResultsStatement = this.session.prepare("select application, correlationId, creationTime, endpoint, id, name, sourceCorrelationId, sourceId from " + keyspace + ".telemetry_event where id = ?");
		this.findEventForDetailsStatement = this.session.prepare("select application, content, correlationId, correlations, creationTime, direction, endpoint, expiryTime, name, sourceCorrelationId, sourceId, transactionId, transactionName, type from " + keyspace + ".telemetry_event where id = ?");
    }

	public void addEvents(SolrDocumentList results, JsonGenerator generator) throws JsonGenerationException, IOException {
		for (SolrDocument solrDocument : results) {
			ResultSet resultSet = this.session.execute(this.findEventForSearchResultsStatement.bind(UUID.fromString((String) solrDocument.get("id"))));
			Row row = resultSet.one();
			if (row != null) {
				generator.writeStartObject();
				writeStringValue("application", row.getString(0), generator);
				writeUUIDValue("correlationId", row.getUUID(1), generator);
				writeDateValue("creationTime", row.getDate(2), generator);
				writeStringValue("endpoint", row.getString(3), generator);
				writeUUIDValue("id", row.getUUID(4), generator);
				writeStringValue("name", row.getString(5), generator);
				writeStringValue("sourceCorrelationId", row.getString(6), generator);
				writeStringValue("sourceId", row.getString(7), generator);
				generator.writeEndObject();
			}
		}
    }
	
	public void addEvent(UUID eventId, JsonGenerator generator) throws JsonGenerationException, IOException {
	    ResultSet resultSet = this.session.execute(this.findEventForDetailsStatement.bind(eventId));
		Row row = resultSet.one();
		if (row != null) {
			writeStringValue("application", row.getString(0), generator);
			writeStringValue("content", row.getString(1), generator);
			writeUUIDValue("correlationId", row.getUUID(2), generator);
			generator.writeArrayFieldStart("childCorrelationIds");
			List<UUID> correlations = row.getList(3, UUID.class);
			for (UUID childCorrelation : correlations) {
				generator.writeString(childCorrelation.toString());
			}
			generator.writeEndArray();
			writeDateValue("creationTime", row.getDate(4), generator);
			writeStringValue("direction", row.getString(5), generator);
			writeStringValue("endpoint", row.getString(6), generator);
			writeDateValue("expiryTime", row.getDate(7), generator);
			writeUUIDValue("id", eventId, generator);
			writeStringValue("name", row.getString(8), generator);
			writeStringValue("sourceCorrelationId", row.getString(9), generator);
			writeStringValue("sourceId", row.getString(10), generator);
			writeUUIDValue("transactionId", row.getUUID(11), generator);
			writeStringValue("transactionName", row.getString(12), generator);
			writeStringValue("type", row.getString(13), generator);
		}
    }

	private void writeUUIDValue(String fieldName, UUID fieldValue, JsonGenerator generator) throws JsonGenerationException, IOException {
	    if (fieldValue == null) {
	    	return;
	    }
	    generator.writeStringField(fieldName, fieldValue.toString());
    }

	private void writeStringValue(String fieldName, String fieldValue, JsonGenerator generator) throws JsonGenerationException, IOException {
	    if (fieldValue == null) {
	    	return;
	    }
	    generator.writeStringField(fieldName, fieldValue);
    }
	
	private void writeDateValue(String fieldName, Date fieldValue, JsonGenerator generator) throws JsonGenerationException, IOException {
	    if (fieldValue == null) {
	    	return;
	    }
	    generator.writeNumberField(fieldName, fieldValue.getTime());
    }



}
