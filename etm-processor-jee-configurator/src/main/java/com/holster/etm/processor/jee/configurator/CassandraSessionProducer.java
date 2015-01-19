package com.holster.etm.processor.jee.configurator;

import java.util.Properties;

import javax.annotation.ManagedBean;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.Session;
import com.holster.etm.jee.configurator.core.ProcessorConfiguration;

@ManagedBean
@Singleton
public class CassandraSessionProducer {

	@ProcessorConfiguration
	@Inject
	private Properties configuration;
	
	private Session session;

	@Produces
	@ProcessorConfiguration
	public Session getSession() {
		synchronized (this) {
			if (this.session == null) {
				Builder builder = Cluster.builder();
				String contactPoints = this.configuration.getProperty("cassandra.contact_points", "127.0.0.1");
				String[] split = contactPoints.split(",");
				for (String contactPoint : split) {
					builder = builder.addContactPoint(contactPoint.trim());
				}
				String username = this.configuration.getProperty("cassandra.username");
				String password = this.configuration.getProperty("cassandra.password");
				if (username != null) {
					builder.withCredentials(username, password);
				}				
				Cluster cluster = builder.build();
				this.session = cluster.newSession().init();
			}
		}
		return this.session;
	}
}
