package com.holster.etm;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.lmax.disruptor.EventHandler;

public class CorrelatingEventHandler implements EventHandler<TelemetryEvent> {

	private final Session session;
	private final long ordinal;
	private final long numberOfConsumers;
	private final PreparedStatement findParentStatement;
	
	public static final PreparedStatement createFindParentStatement(Session session) {
		return session.prepare("select id, transactionid, transactionname from etm.telemetryevent where sourceId = ?;");
	}
	
	public CorrelatingEventHandler(final Session session, final PreparedStatement findParentStatement, final long ordinal, final long numberOfConsumers) {
		this.session = session;
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
		this.findParentStatement = findParentStatement;
	}

	@Override
	public void onEvent(final TelemetryEvent event, final long sequence, final boolean endOfBatch) throws Exception {
		if (event.ignore || (sequence % this.numberOfConsumers) != this.ordinal) {
			return;
		}
		if (event.sourceCorrelationId != null) {
			ResultSet resultSet = this.session.execute(this.findParentStatement.bind(event.sourceCorrelationId));
			Row row = resultSet.one();
			if (row != null) {
				event.correlationId = row.getUUID(0);
				event.transactionId = row.getUUID(1);
				event.transactionName = row.getString(2);
			}
		}
	}
}
