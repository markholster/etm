package com.jecstar.etm.processor.jee.configurator.cassandra;

import com.datastax.driver.core.Session;
import com.jecstar.etm.processor.processor.PersistenceEnvironment;
import com.jecstar.etm.processor.processor.SourceCorrelationCache;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;
import com.jecstar.etm.processor.repository.cassandra.CassandraStatementExecutor;
import com.jecstar.etm.processor.repository.cassandra.TelemetryEventRepositoryCassandraImpl;

public class PersistenceEnvironmentCassandraImpl implements PersistenceEnvironment {
	
	
	private final SourceCorrelationCache sourceCorrelations = new SourceCorrelationCache();
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
    public SourceCorrelationCache getProcessingMap() {
	    return this.sourceCorrelations;
    }

    public void close() {
	    this.cassandraSession.close();
    }

}
