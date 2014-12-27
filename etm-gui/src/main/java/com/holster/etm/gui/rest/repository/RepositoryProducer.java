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

	@Produces
	public StatisticsRepository getStatementRepository() {
		synchronized (this) {
	        if (this.statisticsRepository == null) {
	        	this.statisticsRepository = new StatisticsRepository(this.session);
	        }
        }
		return this.statisticsRepository;
	}
}
