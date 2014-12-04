package com.holster.etm;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.lmax.disruptor.EventHandler;

public class PersistingEventHandler implements EventHandler<TelemetryEvent> {

	private final Session session;
	private final long ordinal;
	private final long numberOfConsumers;
	private final PreparedStatement insertStatement;
	
	public static final PreparedStatement createInsertStatement(Session session) {
		PreparedStatement insertStatement = session.prepare("insert into etm.telemetryevent(" + "id, " + "application, " + "content, "
		        + "correlationId, " + "endpoint, " + "eventTime, " + "sourceId, " + "sourceCorrelationId, " + "transactionId, "
		        + "transactionName) values (?,?,?,?,?,?,?,?,?,?);");
		insertStatement.setRetryPolicy(DefaultRetryPolicy.INSTANCE);
		return insertStatement;
	}

	public PersistingEventHandler(final Session session, final PreparedStatement insertStatement, final long ordinal, final long numberOfConsumers) {
		this.session = session;
	    this.ordinal = ordinal;
	    this.numberOfConsumers = numberOfConsumers;
		this.insertStatement = insertStatement;
	}

	@Override
	public void onEvent(TelemetryEvent event, long sequence, boolean endOfBatch) throws Exception {
		if (event.ignore || (sequence % this.numberOfConsumers) != this.ordinal) {
			return;
		}
		// TODO insert counters
		// TODO execute async and validate in a later step?
		this.session.execute(this.insertStatement.bind(event.id, event.application, event.content, event.correlationId, event.endpoint,
		        event.eventTime, event.sourceId, event.sourceCorrelationId, event.transactionId, event.transactionName));
	}
	
	
}
