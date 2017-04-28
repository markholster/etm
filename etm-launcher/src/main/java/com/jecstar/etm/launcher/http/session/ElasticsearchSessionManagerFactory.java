package com.jecstar.etm.launcher.http.session;

import org.elasticsearch.client.Client;

import io.undertow.server.session.SessionManager;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.SessionManagerFactory;

public class ElasticsearchSessionManagerFactory implements SessionManagerFactory {
	
	private final Client client;

	public ElasticsearchSessionManagerFactory(Client client) {
		this.client = client;
	}

	@Override
	public SessionManager createSessionManager(Deployment deployment) {
		return new ElasticsearchSessionManager(this.client, deployment.getDeploymentInfo().getSessionIdGenerator(), deployment.getDeploymentInfo().getDeploymentName());
	}

}
