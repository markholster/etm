package com.jecstar.etm.launcher.http.session;

import java.util.Set;

import com.jecstar.etm.server.core.configuration.EtmConfiguration;

import io.undertow.UndertowLogger;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionManager;
import io.undertow.util.AttachmentKey;

public class ElasticsearchSession implements Session {

	protected static final AttachmentKey<ElasticsearchSession> ETM_SESSION = AttachmentKey.create(ElasticsearchSession.class);
	
	
	private final EtmConfiguration etmConfiguration;
	private final long creationTime;
	private final ElasticsearchSessionManager sessionManager;
	private final SessionConfig sessionCookieConfig;
	
	private String sessionId;
	private long lastAccessedTime;
	private boolean invalid;



	
	public ElasticsearchSession(EtmConfiguration etmConfiguration, ElasticsearchSessionManager sessionManager, String sessionId, SessionConfig sessionCookieConfig) {
		this.etmConfiguration = etmConfiguration;
		this.creationTime = lastAccessedTime = System.currentTimeMillis();
		this.sessionManager = sessionManager;
		this.sessionCookieConfig = sessionCookieConfig;
	}
	
	@Override
	public String getId() {
		return this.sessionId;
	}

	@Override
	public void requestDone(HttpServerExchange serverExchange) {
		this.lastAccessedTime = System.currentTimeMillis();
		// TODO persist to elasticsearch.
	}

	@Override
	public long getCreationTime() {
		return this.creationTime;
	}

	@Override
	public long getLastAccessedTime() {
		return this.lastAccessedTime;
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		// TODO Auto-generated method stub
	}

	@Override
	public int getMaxInactiveInterval() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Object getAttribute(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getAttributeNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object setAttribute(String name, Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object removeAttribute(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void invalidate(HttpServerExchange exchange) {
        UndertowLogger.SESSION_LOGGER.debugf("Invalidating session %s for exchange %s", sessionId, exchange);
        this.invalid = true;
        // TODO hier verwijderen uit elasticsearch of in een session listener or periodiek?
        this.sessionManager.getSessionListeners().sessionDestroyed(this, exchange, SessionListener.SessionDestroyedReason.INVALIDATED);

        if (exchange != null) {
            sessionCookieConfig.clearSession(exchange, this.getId());
        }
		
        if(exchange != null) {
            exchange.removeAttachment(this.sessionManager.ETM_SESSION);
        }

	}

	@Override
	public SessionManager getSessionManager() {
		return this.sessionManager;
	}

	@Override
	public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
		// TODO Auto-generated method stub
		return null;
	}

}
