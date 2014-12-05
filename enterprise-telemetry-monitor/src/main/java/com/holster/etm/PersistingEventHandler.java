package com.holster.etm;

import java.util.HashMap;
import java.util.Map;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.lmax.disruptor.EventHandler;

public class PersistingEventHandler implements EventHandler<TelemetryEvent> {

	private static final String STATEMENT_INSERT_TELEMETRTY_EVENT = "insertTelementryEvent";
	private static final String STATEMENT_INSERT_SOURCE_ID = "insertSourceId";
	private static final String STATEMENT_UPDATE_APPLICATION_COUNTER = "updateApplicationCounter";
	private static final String STATEMENT_UPDATE_EVENT_NAME_COUNTER = "updateEventNameCounter";
	private static final String STATEMENT_UPDATE_APPLICATION_EVENT_NAME_COUNTER = "updateApplicationEventNameCounter";
	private static final String STATEMENT_UPDATE_TRANSACTION_NAME_COUNTER = "updateTransactionNameCounter";
	
	private final Session session;
	private final long ordinal;
	private final long numberOfConsumers;
	private final PreparedStatement insertTelemetryEventStatement;
	private final PreparedStatement insertSourceIdIdStatement;
	private final PreparedStatement updateApplicationCounterStatement;
	private final PreparedStatement updateEventNameCounterStatement;
	private final PreparedStatement updateApplicationEventNameCounterStatement;
	private final PreparedStatement updateTransactionNameCounterStatement;
	
	private long timestamp;
	
	public static final Map<String,PreparedStatement> createPreparedStatements(Session session) {
		final String keySpace = "etm";
		Map<String, PreparedStatement> statements = new HashMap<String, PreparedStatement>();
		
		PreparedStatement statement = session.prepare("insert into " + keySpace + ".telemetryevent (id, application, content, correlationId, endpoint, eventTime, sourceId, sourceCorrelationId, transactionId, transactionName, type) values (?,?,?,?,?,?,?,?,?,?,?);");
		statement.setRetryPolicy(DefaultRetryPolicy.INSTANCE);
		statements.put(STATEMENT_INSERT_TELEMETRTY_EVENT, statement);
		
		statement = session.prepare("insert into " + keySpace + ".sourceid_id_correlation (sourceId, id) values (?,?);");
		statement.setRetryPolicy(DefaultRetryPolicy.INSTANCE);
		statements.put(STATEMENT_INSERT_SOURCE_ID, statement);

		statement = session.prepare("update " + keySpace + ".application_counter set count = count + 1, messageRequestCount = messageRequestCount + (?), messageResponseCount = messageResponseCount + (?), messageDatagramCount = messageDatagramCount + (?) where application = ? and timestamp = ?;");
		statement.setRetryPolicy(DefaultRetryPolicy.INSTANCE);
		statements.put(STATEMENT_UPDATE_APPLICATION_COUNTER, statement);

		statement = session.prepare("update " + keySpace + ".eventname_counter set count = count + 1, messageRequestCount = messageRequestCount + (?), messageResponseCount = messageResponseCount + (?), messageDatagramCount = messageDatagramCount + (?) where eventName = ? and timestamp = ?;");
		statement.setRetryPolicy(DefaultRetryPolicy.INSTANCE);
		statements.put(STATEMENT_UPDATE_EVENT_NAME_COUNTER, statement);

		statement = session.prepare("update " + keySpace + ".application_event_counter set count = count + 1, messageRequestCount = messageRequestCount + (?), messageResponseCount = messageResponseCount + (?), messageDatagramCount = messageDatagramCount + (?) where application = ? and eventName = ? and timestamp = ?;");
		statement.setRetryPolicy(DefaultRetryPolicy.INSTANCE);
		statements.put(STATEMENT_UPDATE_APPLICATION_EVENT_NAME_COUNTER, statement);

		statement = session.prepare("update " + keySpace + ".transactionname_counter set count = count + 1 where transactionName = ? and timestamp = ?;");
		statement.setRetryPolicy(DefaultRetryPolicy.INSTANCE);
		statements.put(STATEMENT_UPDATE_TRANSACTION_NAME_COUNTER, statement);
		return statements;
	}

	public PersistingEventHandler(final Session session, final Map<String,PreparedStatement> statements, final long ordinal, final long numberOfConsumers) {
		this.session = session;
	    this.ordinal = ordinal;
	    this.numberOfConsumers = numberOfConsumers;
		this.insertTelemetryEventStatement = statements.get(STATEMENT_INSERT_TELEMETRTY_EVENT);
		this.insertSourceIdIdStatement = statements.get(STATEMENT_INSERT_SOURCE_ID);
		this.updateApplicationCounterStatement = statements.get(STATEMENT_UPDATE_APPLICATION_COUNTER);
		this.updateEventNameCounterStatement = statements.get(STATEMENT_UPDATE_EVENT_NAME_COUNTER);
		this.updateApplicationEventNameCounterStatement = statements.get(STATEMENT_UPDATE_APPLICATION_EVENT_NAME_COUNTER);
		this.updateTransactionNameCounterStatement = statements.get(STATEMENT_UPDATE_TRANSACTION_NAME_COUNTER);
	}

	@Override
	public void onEvent(TelemetryEvent event, long sequence, boolean endOfBatch) throws Exception {
		if (event.ignore || (sequence % this.numberOfConsumers) != this.ordinal) {
			return;
		}
		this.timestamp = normalizeTime(System.currentTimeMillis());
		this.session.executeAsync(this.insertTelemetryEventStatement.bind(event.id, event.application, event.content, event.correlationId, event.endpoint,
		        event.eventTime, event.sourceId, event.sourceCorrelationId, event.transactionId, event.transactionName, event.eventType != null ? event.eventType.name() : null));
		if (event.sourceId != null) {
			this.session.executeAsync(this.insertSourceIdIdStatement.bind(event.sourceId, event.id));
		}
		int requestCount = TelemetryEventType.MESSAGE_REQUEST.equals(event.eventType) ? 1 : 0;
		int responseCount = TelemetryEventType.MESSAGE_RESPONSE.equals(event.eventType) ? 1 : 0;
		int datagramCount = TelemetryEventType.MESSAGE_DATAGRAM.equals(event.eventType) ? 1 : 0;
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
	
	
}
