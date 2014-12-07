package com.holster.etm;

import java.util.HashMap;
import java.util.Map;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.lmax.disruptor.EventHandler;

public class EnhancingEventHandler implements EventHandler<TelemetryEvent> {

	private static final String STATEMENT_FIND_PARENT_TELEMETRTY_EVENT = "findParentTelementryEvent";
	
	private final Session session;
	private final long ordinal;
	private final long numberOfConsumers;
	private final PreparedStatement findParentStatement;
	
	public static final Map<String,PreparedStatement> createPreparedStatements(Session session) {
		final String keySpace = "etm";
		Map<String, PreparedStatement> statements = new HashMap<String, PreparedStatement>();
		
		PreparedStatement statement = session.prepare("select id, transactionId, transactionName from " + keySpace + ".sourceid_id_correlation where sourceId = ?;");
		statement.setRetryPolicy(DefaultRetryPolicy.INSTANCE);
		statements.put(STATEMENT_FIND_PARENT_TELEMETRTY_EVENT, statement);


		return statements;
	}
	
	public EnhancingEventHandler(final Session session, final Map<String,PreparedStatement> statements, final long ordinal, final long numberOfConsumers) {
		this.session = session;
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
		this.findParentStatement = statements.get(STATEMENT_FIND_PARENT_TELEMETRTY_EVENT);
	}

	@Override
	public void onEvent(final TelemetryEvent event, final long sequence, final boolean endOfBatch) throws Exception {
		if (event.ignore || (sequence % this.numberOfConsumers) != this.ordinal) {
			return;
		}
		if (event.sourceCorrelationId != null && (event.correlationId == null || event.transactionId == null || event.transactionName == null)) {
			ResultSet resultSet = this.session.execute(this.findParentStatement.bind(event.sourceCorrelationId));
			Row row = resultSet.one();
			if (row != null) {
				if (event.correlationId == null) {
					event.correlationId = row.getUUID(0);
				}
				if (event.transactionId == null) {
					event.transactionId = row.getUUID(1);
				}
				if (event.transactionName == null) {
					event.transactionName = row.getString(2);
				}
			}
		}
	}
}
