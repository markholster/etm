package com.jecstar.etm.launcher.http.session;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.search.SearchHit;

import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import com.jecstar.etm.server.core.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;

import io.undertow.UndertowLogger;
import io.undertow.UndertowMessages;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionIdGenerator;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListeners;
import io.undertow.server.session.SessionManager;
import io.undertow.server.session.SessionManagerStatistics;
import io.undertow.util.AttachmentKey;

public class ElasticsearchSessionManager implements SessionManager {

	final AttachmentKey<ElasticsearchSession> ETM_SESSION = AttachmentKey.create(ElasticsearchSession.class);

	private final Client client;
	private final SessionIdGenerator sessionIdGenerator;
	private final String deploymentName;
	private final EtmConfiguration etmConfiguration;

	private final SessionListeners sessionListeners = new SessionListeners();


	public ElasticsearchSessionManager(Client client, SessionIdGenerator sessionIdGenerator, String deploymentName, EtmConfiguration etmConfiguration) {
		this.client = client;
		this.sessionIdGenerator = sessionIdGenerator;
		this.etmConfiguration = etmConfiguration;
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
		if (sessionCookieConfig == null) {
			throw UndertowMessages.MESSAGES.couldNotFindSessionCookieConfig();
		}
		String sessionId = sessionCookieConfig.findSessionId(serverExchange);
		int count = 0;
		while (sessionId == null) {
			sessionId = this.sessionIdGenerator.createSessionId();
			GetResponse getResponse = this.client.prepareGet(ElasticsearchLayout.STATE_INDEX_NAME, ElasticsearchLayout.STATE_INDEX_TYPE_SESSION, sessionId).setFetchSource(false).get();
			if (getResponse.isExists()) {
				sessionId = null;
			}
			if (count++ == 100) {
				throw UndertowMessages.MESSAGES.couldNotGenerateUniqueSessionId();
			}
		}
		final ElasticsearchSession session = new ElasticsearchSession(this.etmConfiguration, this, sessionId, sessionCookieConfig);
		// TODO store the session in elasticsearch.

		UndertowLogger.SESSION_LOGGER.debugf("Created session with id %s for exchange %s", sessionId, serverExchange);
		sessionCookieConfig.setSessionId(serverExchange, session.getId());
		this.sessionListeners.sessionCreated(session, serverExchange);
		serverExchange.putAttachment(ETM_SESSION, session);
		return session;
	}

	@Override
	public Session getSession(HttpServerExchange serverExchange, SessionConfig sessionCookieConfig) {
		if (serverExchange != null) {
			ElasticsearchSession newSession = serverExchange.getAttachment(ETM_SESSION);
			if (newSession != null) {
				return newSession;
			}
		}
		String sessionId = sessionCookieConfig.findSessionId(serverExchange);
		return getSession(sessionId);
	}

	@Override
	public Session getSession(String sessionId) {
		if (sessionId == null) {
			return null;
		}
		GetResponse getResponse = this.client.prepareGet(ElasticsearchLayout.STATE_INDEX_NAME, ElasticsearchLayout.STATE_INDEX_TYPE_SESSION, sessionId).setFetchSource(true).get();
		if (getResponse.isExists()) {
			return null;
		} else {
			Map<String, Object> valueMap = getResponse.getSourceAsMap();
			// TODO Eerst bepalen of de session nog geldig is. Als dat het geval is dan de map converteren naar een sessie object.
			return null;
		}
	}

	@Override
	public void registerSessionListener(SessionListener listener) {
		this.sessionListeners.addSessionListener(listener);
	}

	@Override
	public void removeSessionListener(SessionListener listener) {
		this.sessionListeners.removeSessionListener(listener);
	}
	
	public SessionListeners getSessionListeners() {
		return this.sessionListeners;
	}

	@Override
	public void setDefaultSessionTimeout(int timeout) {
		throw new UnsupportedOperationException("setDefaultSessionTimeout");
	}

	@Override
	public Set<String> getTransientSessions() {
		return Collections.emptySet();
	}

	@Override
	public Set<String> getActiveSessions() {
		return getAllSessions();
	}

	@Override
	public Set<String> getAllSessions() {
		Set<String> result = new HashSet<>();
		SearchRequestBuilder requestBuilder = client.prepareSearch(ElasticsearchLayout.STATE_INDEX_NAME)
			.setTypes(ElasticsearchLayout.STATE_INDEX_TYPE_SESSION)
			.setFetchSource(false)
			.setQuery(new MatchAllQueryBuilder())
			.setTimeout(TimeValue.timeValueMillis(this.etmConfiguration.getQueryTimeout()));
		ScrollableSearch scrollableSearch = new ScrollableSearch(this.client, requestBuilder);
		for (SearchHit searchHit : scrollableSearch) {
			result.add(searchHit.getId());
		}
		return result;
	}

	@Override
	public SessionManagerStatistics getStatistics() {
		return null;
	}

}
