package com.holster.etm.scheduler.jee.configurator;

import java.util.List;

import javax.annotation.ManagedBean;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.Session;
import com.holster.etm.core.configuration.EtmConfiguration;
import com.holster.etm.jee.configurator.core.SchedulerConfiguration;

@ManagedBean
@Singleton
public class CassandraSessionProducer {

	@SchedulerConfiguration
	@Inject
	private EtmConfiguration configuration;
	
	private Session session;

	private Cluster cluster;

	@Produces
	@SchedulerConfiguration
	public Session getSession() {
		synchronized (this) {
			if (this.session == null) {
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
				this.cluster = builder.build();
				this.session = this.cluster.newSession().init();
			}
		}
		return this.session;
	}
	
	@PreDestroy
	public void preDestroy() {
		if (this.session != null) {
			this.session.close();
		}
		this.session = null;
		if (this.cluster != null) {
			this.cluster.close();
		}
		this.cluster = null;
	}
}
