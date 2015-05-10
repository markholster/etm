package com.jecstar.etm.processor.jee.configurator.cassandra;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.datastax.driver.core.Session;
import com.jecstar.etm.processor.processor.PersistenceEnvironment;
import com.jecstar.etm.processor.repository.CorrelationBySourceIdResult;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;
import com.jecstar.etm.processor.repository.cassandra.CassandraStatementExecutor;
import com.jecstar.etm.processor.repository.cassandra.TelemetryEventRepositoryCassandraImpl;

public class PersistenceEnvironmentCassandraImpl implements PersistenceEnvironment {
	
	//TODO proberen dit niet in een synchronised map te plaatsen, maar bijvoorbeeld in een ConcurrentMap
	private final Map<String, CorrelationBySourceIdResult> sourceCorrelations = Collections.synchronizedMap(new HashMap<String, CorrelationBySourceIdResult>());
	private Session cassandraSession;
	private CassandraStatementExecutor statementExecutor;

	public PersistenceEnvironmentCassandraImpl(final Session session) {
		this.cassandraSession = session;
	    this.statementExecutor = new CassandraStatementExecutor(this.cassandraSession);
    }

	@Override
    public TelemetryEventRepository createTelemetryEventRepository() {
	    return new TelemetryEventRepositoryCassandraImpl(this.statementExecutor, this.sourceCorrelations);
    }

	@Override
    public Map<String, CorrelationBySourceIdResult> getProcessingMap() {
	    return this.sourceCorrelations;
    }

    public void close() {
	    this.cassandraSession.close();
    }

}
