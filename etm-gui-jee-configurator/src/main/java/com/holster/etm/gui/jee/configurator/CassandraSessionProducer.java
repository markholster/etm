package com.holster.etm.gui.jee.configurator;

import java.util.List;

import javax.annotation.ManagedBean;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.Session;
import com.holster.etm.core.configuration.CassandraConfiguration;
import com.holster.etm.core.configuration.ConfigurationChangeListener;
import com.holster.etm.core.configuration.ConfigurationChangedEvent;
import com.holster.etm.core.configuration.EtmConfiguration;
import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;
import com.holster.etm.jee.configurator.core.GuiConfiguration;

@ManagedBean
@Singleton
public class CassandraSessionProducer implements ConfigurationChangeListener {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(CassandraSessionProducer.class);

	@GuiConfiguration
	@Inject
	private EtmConfiguration configuration;

	private ReconfigurableSession session;

	private Cluster cluster;

	@GuiConfiguration
	@Produces
	public Session getSession() {
		synchronized (this) {
			if (this.session == null) {
				this.cluster = createCluster();
				this.configuration.addCassandraConfigurationChangeListener(this);
				this.session = new ReconfigurableSession(this.cluster.newSession().init());
			}
		}
		return this.session;
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
		return builder.build();
	}
	
	@PreDestroy
	public void preDestroy() {
		if (this.configuration != null) {
			this.configuration.removeCassandraConfigurationChangeListener(this);
		}
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
		        CassandraConfiguration.CASSANDRA_USERNAME)) {
			if (this.session != null) {
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Detected a change in the configuration that needs to reconnect to Cassandra cluster.");
				}
				Cluster newCluster = createCluster();
				this.session.switchToSession(newCluster.newSession().init());
				this.cluster.closeAsync();
				this.cluster = newCluster;
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Reconnected to Cassandra cluster.");
				}
			}
		}
    }
}
