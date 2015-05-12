package com.jecstar.etm.processor.jee.configurator;

import java.util.List;

import javax.annotation.ManagedBean;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.codahale.metrics.MetricRegistry;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.jecstar.etm.core.configuration.CassandraConfiguration;
import com.jecstar.etm.core.configuration.ConfigurationChangeListener;
import com.jecstar.etm.core.configuration.ConfigurationChangedEvent;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.jee.configurator.core.ProcessorConfiguration;
import com.jecstar.etm.processor.jee.configurator.cassandra.PersistenceEnvironmentCassandraImpl;
import com.jecstar.etm.processor.jee.configurator.cassandra.ReconfigurableSession;
import com.jecstar.etm.processor.processor.PersistenceEnvironment;
import com.jecstar.etm.processor.repository.cassandra.CassandraMetricReporter;

@ManagedBean
@Singleton
public class PersistenceEnvironmentProducer implements ConfigurationChangeListener {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(PersistenceEnvironmentProducer.class);

	@ProcessorConfiguration
	@Inject
	private EtmConfiguration configuration;
	
	@ProcessorConfiguration
	@Inject
	private MetricRegistry metricRegistry;

	private PersistenceEnvironment persistenceEnvironment;
	
	private ReconfigurableSession session;

	private Cluster cluster;

	private CassandraMetricReporter internalMetricReporter;
	private CassandraMetricReporter databaseMetricReporter;
	
	@ProcessorConfiguration
	@Produces
	public PersistenceEnvironment getPersistenceEnvironment() {
		synchronized (this) {
			if (this.persistenceEnvironment == null) {
				this.cluster = createCluster();
				this.configuration.addCassandraConfigurationChangeListener(this);
				this.configuration.addEtmConfigurationChangeListener(this);
				this.session = new ReconfigurableSession(this.cluster.connect(this.configuration.getCassandraKeyspace()));
				this.persistenceEnvironment = new PersistenceEnvironmentCassandraImpl(this.session);
				this.internalMetricReporter = new CassandraMetricReporter(this.configuration.getNodeName(), this.metricRegistry, this.session, this.configuration.getStatisticsTimeUnit());
				this.internalMetricReporter.start(1, this.configuration.getStatisticsTimeUnit());
				this.databaseMetricReporter = new CassandraMetricReporter(this.configuration.getNodeName() + "-cassandra", this.cluster.getMetrics().getRegistry(), this.session, this.configuration.getStatisticsTimeUnit());
				this.databaseMetricReporter.start(1, this.configuration.getStatisticsTimeUnit());
			}
		}
		return this.persistenceEnvironment;
	}

	private Cluster createCluster() {
		Builder builder = Cluster.builder();
		List<String> contactPoints = this.configuration.getCassandraContactPoints();
		for (String contactPoint : contactPoints) {
			builder = builder.addContactPoint(contactPoint.trim());
		}
		String username = this.configuration.getCassandraUsername();
		String password = this.configuration.getCassandraPassword();
		if (username != null) {
			builder.withCredentials(username, password);
		}
		builder.withoutJMXReporting();
		return builder.build();
	}
	
	@PreDestroy
	public void preDestroy() {
		if (this.configuration != null) {
			this.configuration.removeCassandraConfigurationChangeListener(this);
		}
		this.persistenceEnvironment.close();
		if (this.internalMetricReporter != null) {
			this.internalMetricReporter.close();
		}
		this.internalMetricReporter = null;
		if (this.databaseMetricReporter != null) {
			this.databaseMetricReporter.close();
		}
		this.databaseMetricReporter = null;
		if (this.session != null) {
			this.session.close();
		}
		this.session = null;
		if (this.cluster != null) {
			this.cluster.close();
		}
		this.cluster = null;
	}

	@Override
    public void configurationChanged(ConfigurationChangedEvent event) {
		if (event.isAnyChanged(CassandraConfiguration.CASSANDRA_CONTACT_POINTS, CassandraConfiguration.CASSANDRA_PASSWORD,
		        CassandraConfiguration.CASSANDRA_USERNAME, CassandraConfiguration.CASSANDRA_KEYSPACE)) {
			if (this.session != null) {
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Detected a change in the configuration that needs ETM to reconnect to Cassandra cluster.");
				}
				Cluster newCluster = createCluster();
				if (this.databaseMetricReporter != null) {
					this.databaseMetricReporter.close();
				}
				this.session.switchToSession(newCluster.connect(this.configuration.getCassandraKeyspace()));
				this.databaseMetricReporter = new CassandraMetricReporter(this.configuration.getNodeName() + "-cassandra", newCluster.getMetrics().getRegistry(), this.session, this.configuration.getStatisticsTimeUnit());
				this.databaseMetricReporter.start(1, this.configuration.getStatisticsTimeUnit());
				this.cluster.closeAsync();
				this.cluster = newCluster;
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Reconnected to Cassandra cluster.");
				}
			}
		}
		if (event.isChanged(EtmConfiguration.ETM_STATISTICS_TIMEUNIT)) {
			if (this.internalMetricReporter != null) {
				this.internalMetricReporter.close();
			}
			this.internalMetricReporter = new CassandraMetricReporter(this.configuration.getNodeName(), this.metricRegistry, this.session, this.configuration.getStatisticsTimeUnit());
			this.internalMetricReporter.start(1, this.configuration.getStatisticsTimeUnit());
			if (this.databaseMetricReporter != null) {
				this.databaseMetricReporter.close();
			}
			this.databaseMetricReporter = new CassandraMetricReporter(this.configuration.getNodeName() + "-cassandra", this.cluster.getMetrics().getRegistry(), this.session, this.configuration.getStatisticsTimeUnit());
			this.databaseMetricReporter.start(1, this.configuration.getStatisticsTimeUnit());
		}
    }

}
