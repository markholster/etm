package com.jecstar.etm.launcher.http.session;

import java.util.Set;

import org.elasticsearch.client.Client;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionIdGenerator;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListeners;
import io.undertow.server.session.SessionManager;
import io.undertow.server.session.SessionManagerStatistics;

public class ElasticsearchSessionManager implements SessionManager {

	private final Client client;
	private final SessionIdGenerator sessionIdGenerator;
	private final String deploymentName;
	
	private final SessionListeners sessionListeners = new SessionListeners();

	public ElasticsearchSessionManager(Client client, SessionIdGenerator sessionIdGenerator, String deploymentName) {
		this.client = client;
		this.sessionIdGenerator = sessionIdGenerator;
		this.deploymentName = deploymentName;
	}

	@Override
	public String getDeploymentName() {
		return this.deploymentName;
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public Session createSession(HttpServerExchange serverExchange, SessionConfig sessionCookieConfig) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Session getSession(HttpServerExchange serverExchange, SessionConfig sessionCookieConfig) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Session getSession(String sessionId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void registerSessionListener(SessionListener listener) {
		this.sessionListeners.addSessionListener(listener);
	}

	@Override
	public void removeSessionListener(SessionListener listener) {
		this.sessionListeners.removeSessionListener(listener);
		
	}

	@Override
	public void setDefaultSessionTimeout(int timeout) {
		throw new UnsupportedOperationException("setDefaultSessionTimeout");
	}

	@Override
	public Set<String> getTransientSessions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getActiveSessions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getAllSessions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SessionManagerStatistics getStatistics() {
		// TODO Auto-generated method stub
		return null;
	}

}
