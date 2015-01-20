package com.holster.etm.gui.rest.repository;

import java.util.Properties;

import javax.annotation.ManagedBean;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.datastax.driver.core.Session;
import com.holster.etm.jee.configurator.core.GuiConfiguration;

@ManagedBean
@Singleton
public class RepositoryProducer {

	@Inject
	@GuiConfiguration
	private Session session;
	
	@GuiConfiguration
	@Inject
	private Properties configuration;
	
	private String keyspace;
	private StatisticsRepository statisticsRepository;
	private QueryRepository queryRepository;
	private AdminRepository adminRepository;
	
	
	private String getKeyspace() {
		if (this.keyspace == null) {
			this.keyspace = this.configuration.getProperty("cassandra.keyspace", "etm");
		}
		return this.keyspace;
	}
	

	@Produces
	public StatisticsRepository getStatisticsRepository() {
		synchronized (this) {
	        if (this.statisticsRepository == null) {
	        	this.statisticsRepository = new StatisticsRepository(this.session, getKeyspace());
	        }
        }
		return this.statisticsRepository;
	}
	
	@Produces
	public QueryRepository getQueryRepository() {
		synchronized (this) {
	        if (this.queryRepository == null) {
	        	this.queryRepository = new QueryRepository(this.session, getKeyspace());
	        }
        }
		return this.queryRepository;
	}
	
	@Produces
	public AdminRepository getAdminRepository() {
		synchronized (this) {
	        if (this.adminRepository == null) {
	        	this.adminRepository = new AdminRepository(this.session, getKeyspace());
	        }
        }
		return this.adminRepository;
	}
}
