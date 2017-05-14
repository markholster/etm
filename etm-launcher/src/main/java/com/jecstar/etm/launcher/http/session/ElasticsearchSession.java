package com.jecstar.etm.launcher.http.session;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.undertow.UndertowLogger;
import io.undertow.UndertowMessages;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionManager;
import io.undertow.util.AttachmentKey;

public class ElasticsearchSession implements Session {

	protected static final AttachmentKey<ElasticsearchSession> ETM_SESSION = AttachmentKey.create(ElasticsearchSession.class);
	
	
	private final long creationTime;
	private final ElasticsearchSessionManager sessionManager;
	private final SessionConfig sessionConfig;
	private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<>();
	
	private String sessionId;
	private long lastAccessedTime;
	private boolean invalid;
	
	public ElasticsearchSession(ElasticsearchSessionManager sessionManager, String sessionId, SessionConfig sessionConfig) {
		this.creationTime = lastAccessedTime = System.currentTimeMillis();
		this.sessionManager = sessionManager;
		this.sessionConfig = sessionConfig;
	}
	
	@Override
	public String getId() {
		return this.sessionId;
	}

	@Override
	public void requestDone(HttpServerExchange serverExchange) {
		this.lastAccessedTime = System.currentTimeMillis();
		// TODO alleen opslaan als er een GET van *.html of POST of een GET van een rest endpoint van het dashboard, aangezien deze een auto update funtie heeft die de sessietimeout moet bumpen. 
		this.sessionManager.persistSession(this);
	}

	@Override
	public long getCreationTime() {
        if (this.invalid) {
            throw UndertowMessages.MESSAGES.sessionIsInvalid(sessionId);
        }
        return this.creationTime;

	}

	@Override
	public long getLastAccessedTime() {
        if (this.invalid) {
            throw UndertowMessages.MESSAGES.sessionIsInvalid(sessionId);
        }
		return this.lastAccessedTime;
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
        if (this.invalid) {
            throw UndertowMessages.MESSAGES.sessionIsInvalid(sessionId);
        }
		throw new UnsupportedOperationException("setMaxInactiveInterval");
	}

	@Override
	public int getMaxInactiveInterval() {
        if (this.invalid) {
            throw UndertowMessages.MESSAGES.sessionIsInvalid(sessionId);
        }
		throw new UnsupportedOperationException("getMaxInactiveInterval");
	}

	@Override
	public Object getAttribute(String name) {
        if (this.invalid) {
            throw UndertowMessages.MESSAGES.sessionIsInvalid(sessionId);
        }
        return this.attributes.get(name);
	}

	@Override
	public Set<String> getAttributeNames() {
        if (this.invalid) {
            throw UndertowMessages.MESSAGES.sessionIsInvalid(sessionId);
        }
        return this.attributes.keySet();
	}

	@Override
	public Object setAttribute(String name, Object value) {
        if (this.invalid) {
            throw UndertowMessages.MESSAGES.sessionIsInvalid(sessionId);
        }
        if (value == null) {
            return removeAttribute(name);
        }
        final Object existing = attributes.put(name, value);
        if (existing == null) {
        	this.sessionManager.getSessionListeners().attributeAdded(this, name, value);
        } else {
        	this.sessionManager.getSessionListeners().attributeUpdated(this, name, value, existing);
        }
        return existing;
	}

	@Override
	public Object removeAttribute(String name) {
        if (this.invalid) {
            throw UndertowMessages.MESSAGES.sessionIsInvalid(sessionId);
        }
        final Object existing = this.attributes.remove(name);
        this.sessionManager.getSessionListeners().attributeRemoved(this, name, existing);
        return existing;
	}

	@Override
	public void invalidate(HttpServerExchange exchange) {
        UndertowLogger.SESSION_LOGGER.debugf("Invalidating session %s for exchange %s", sessionId, exchange);
        this.invalid = true;
        this.sessionManager.removeSession(this);
        this.sessionManager.getSessionListeners().sessionDestroyed(this, exchange, SessionListener.SessionDestroyedReason.INVALIDATED);

        if (exchange != null && this.sessionConfig != null) {
            this.sessionConfig.clearSession(exchange, this.getId());
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
        final String oldId = this.sessionId;
        String newId = this.sessionManager.createSessionId();
        this.sessionId = newId;
        if(!this.invalid) {
            config.setSessionId(exchange, this.getId());
            this.sessionManager.changeSessionId(oldId, newId);
        }
        this.sessionManager.getSessionListeners().sessionIdChanged(this, oldId);
        return newId;
		
	}

}
