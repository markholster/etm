package com.holster.etm.gui.rest.repository;

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
	
	private StatisticsRepository statisticsRepository;
	private QueryRepository queryRepository;

	@Produces
	public StatisticsRepository getStatisticsRepository() {
		synchronized (this) {
	        if (this.statisticsRepository == null) {
	        	this.statisticsRepository = new StatisticsRepository(this.session);
	        }
        }
		return this.statisticsRepository;
	}
	
	@Produces
	public QueryRepository getQueryRepository() {
		synchronized (this) {
	        if (this.queryRepository == null) {
	        	this.queryRepository = new QueryRepository(this.session);
	        }
        }
		return this.queryRepository;
	}
}
