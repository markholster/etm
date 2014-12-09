package com.holster.etm.repository;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.holster.etm.TelemetryEvent;
import com.holster.etm.TelemetryEventType;

public class TelemetryEventRepositoryCassandraImpl implements TelemetryEventRepository {

	private final Session session;
	private final Map<String, UUID> sourceCorrelations;
	
	
	private final PreparedStatement insertTelemetryEventStatement;
	private final PreparedStatement insertSourceIdIdStatement;
	private final PreparedStatement updateApplicationCounterStatement;
	private final PreparedStatement updateEventNameCounterStatement;
	private final PreparedStatement updateApplicationEventNameCounterStatement;
	private final PreparedStatement updateTransactionNameCounterStatement;
	private final PreparedStatement findParentStatement;
	private final PreparedStatement findEndpointConfigStatement;
	
	private final Date timestamp = new Date();
	
	
	public TelemetryEventRepositoryCassandraImpl(final Session session, final Map<String, UUID> sourceCorrelations) {
		this.session = session;
		this.sourceCorrelations = sourceCorrelations;
		final String keySpace = "etm";
		
		this.insertTelemetryEventStatement = session.prepare("insert into " + keySpace + ".telemetryevent (id, application, content, correlationId, endpoint, eventName, eventTime, sourceId, sourceCorrelationId, transactionId, transactionName, type) values (?,?,?,?,?,?,?,?,?,?,?,?);");
		this.insertSourceIdIdStatement = session.prepare("insert into " + keySpace + ".sourceid_id_correlation (sourceId, id, transactionId, transactionName) values (?,?,?,?);");
		this.updateApplicationCounterStatement = session.prepare("update " + keySpace + ".application_counter set count = count + 1, messageRequestCount = messageRequestCount + ?, messageResponseCount = messageResponseCount + ?, messageDatagramCount = messageDatagramCount + ? where application = ? and timeunit = ?;");
		this.updateEventNameCounterStatement = session.prepare("update " + keySpace + ".eventname_counter set count = count + 1, messageRequestCount = messageRequestCount + ?, messageResponseCount = messageResponseCount + ?, messageDatagramCount = messageDatagramCount + ? where eventName = ? and timeunit = ?;");
		this.updateApplicationEventNameCounterStatement = session.prepare("update " + keySpace + ".application_event_counter set count = count + 1, messageRequestCount = messageRequestCount + ?, messageResponseCount = messageResponseCount + ?, messageDatagramCount = messageDatagramCount + ? where application = ? and eventName = ? and timeunit = ?;");
		this.updateTransactionNameCounterStatement = session.prepare("update " + keySpace + ".transactionname_counter set count = count + 1 where transactionName = ? and timeunit = ?;");
		this.findParentStatement = session.prepare("select id, transactionId, transactionName from " + keySpace + ".sourceid_id_correlation where sourceId = ?;");
		this.findEndpointConfigStatement = session.prepare("select application, eventNameParsers from " + keySpace + ".endpoint_config where endpoint = ?;");
    }
	
	@Override
    public void persistTelemetryEvent(TelemetryEvent event) {
		this.timestamp.setTime(normalizeTime(event.eventTime.getTime()));
		this.session.executeAsync(this.insertTelemetryEventStatement.bind(event.id, event.application, event.content, event.correlationId, event.endpoint,
		        event.eventName, event.eventTime, event.sourceId, event.sourceCorrelationId, event.transactionId, event.transactionName, event.eventType != null ? event.eventType.name() : null));
		if (event.sourceId != null) {
			this.session.execute(this.insertSourceIdIdStatement.bind(event.sourceId, event.id, event.transactionId, event.transactionName));
			this.sourceCorrelations.remove(event.sourceId);
		}
		long requestCount = TelemetryEventType.MESSAGE_REQUEST.equals(event.eventType) ? 1 : 0;
		long responseCount = TelemetryEventType.MESSAGE_RESPONSE.equals(event.eventType) ? 1 : 0;
		long datagramCount = TelemetryEventType.MESSAGE_DATAGRAM.equals(event.eventType) ? 1 : 0;
		if (event.application != null) {
			this.session.executeAsync(this.updateApplicationCounterStatement.bind(requestCount, responseCount, datagramCount, event.application, this.timestamp));
		}
		if (event.eventName != null) {
			this.session.executeAsync(this.updateEventNameCounterStatement.bind(requestCount, responseCount, datagramCount, event.eventName, this.timestamp));
		}
		if (event.application != null && event.eventName != null) {
			this.session.executeAsync(this.updateApplicationEventNameCounterStatement.bind(requestCount, responseCount, datagramCount, event.application, event.eventName, this.timestamp));
		}
		if (event.transactionName != null && event.correlationId == null) {
			// event belongs to a transaction, but is not correlated so it's the start event of the transaction
			this.session.executeAsync(this.updateTransactionNameCounterStatement.bind(event.transactionName, this.timestamp));
		}
    }

	private long normalizeTime(long currentTimeMillis) {
		return (currentTimeMillis / 1000) * 1000;
    }

	@Override
    public void findParent(String sourceId, CorrelationBySourceIdResult result) {
		if (sourceId == null) {
			return;
		}
		UUID uuid = this.sourceCorrelations.get(sourceId);
		if (uuid != null) {
			result.id = uuid;
			return;
		}
		ResultSet resultSet = this.session.execute(this.findParentStatement.bind(sourceId));
		Row row = resultSet.one();
		if (row != null) {
			result.id = row.getUUID(0);
			result.transactionId = row.getUUID(1);
			result.transactionName = row.getString(2);
		}
    }
}
