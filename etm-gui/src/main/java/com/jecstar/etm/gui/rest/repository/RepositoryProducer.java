package com.jecstar.etm.gui.rest.repository;

import javax.annotation.ManagedBean;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.elasticsearch.client.Client;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.gui.rest.repository.elastic.EndpointRepositoryElasticImpl;
import com.jecstar.etm.gui.rest.repository.elastic.NodeRepositoryElasticImpl;
import com.jecstar.etm.gui.rest.repository.elastic.QueryRepositoryElasticImpl;
import com.jecstar.etm.gui.rest.repository.elastic.StatisticsRepositoryElasticImpl;
import com.jecstar.etm.jee.configurator.core.GuiConfiguration;

@ManagedBean
@Singleton
public class RepositoryProducer {

	@Inject
	@GuiConfiguration
	private Client elasticClient;
	
	@GuiConfiguration
	@Inject
	private EtmConfiguration configuration;
	
	private StatisticsRepository statisticsRepository;
	private QueryRepository queryRepository;
	private EndpointRepository endpointRepository;
	private NodeRepository nodeRepository;

	@Produces
	public StatisticsRepository getStatisticsRepository() {
		synchronized (this) {
	        if (this.statisticsRepository == null) {
	        	this.statisticsRepository = new StatisticsRepositoryElasticImpl(this.elasticClient);
	        }
        }
		return this.statisticsRepository;
	}
	
	@Produces
	public QueryRepository getQueryRepository() {
		synchronized (this) {
	        if (this.queryRepository == null) {
	        	this.queryRepository = new QueryRepositoryElasticImpl(this.elasticClient);
	        }
        }
		return this.queryRepository;
	}
	
	@Produces
	public EndpointRepository getEndpointRepository() {
		synchronized (this) {
	        if (this.endpointRepository == null) {
	        	this.endpointRepository = new EndpointRepositoryElasticImpl(this.elasticClient);
	        }
        }
		return this.endpointRepository;
	}
	
	@Produces
	public NodeRepository getNodeRepository() {
		synchronized (this) {
	        if (this.nodeRepository == null) {
	        	this.nodeRepository = new NodeRepositoryElasticImpl(this.elasticClient);
	        }
        }
		return this.nodeRepository;
	}
	
}
