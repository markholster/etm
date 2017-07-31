package com.jecstar.etm.launcher.http.session;

import org.elasticsearch.client.Client;

import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;

import io.undertow.server.session.SessionManager;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.SessionManagerFactory;

public class ElasticsearchSessionManagerFactory implements SessionManagerFactory {
	
	private final Client client;
	private final EtmConfiguration etmConfiguration;

	public ElasticsearchSessionManagerFactory(Client client, EtmConfiguration etmConfiguration) {
		this.client = client;
		this.etmConfiguration = etmConfiguration;
	}

	@Override
	public SessionManager createSessionManager(Deployment deployment) {
		return new ElasticsearchSessionManager(this.client, deployment.getDeploymentInfo().getSessionIdGenerator(), deployment.getDeploymentInfo().getDeploymentName(), this.etmConfiguration);
	}

}
