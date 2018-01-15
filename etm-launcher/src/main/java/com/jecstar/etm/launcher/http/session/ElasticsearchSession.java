package com.jecstar.etm.launcher.http.session;

import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import io.undertow.UndertowMessages;
import io.undertow.security.impl.SingleSignOnAuthenticationMechanism;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler;
import io.undertow.util.AttachmentKey;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class ElasticsearchSession implements Session {

	static final AttachmentKey<ElasticsearchSession> ETM_SESSION = AttachmentKey.create(ElasticsearchSession.class);
	private static final String SSO_ATTRIBUTE = SingleSignOnAuthenticationMechanism.class.getName() + ".SSOID";
	
	private final long creationTime;
	private final EtmConfiguration etmConfiguration;
	private final ElasticsearchSessionManager sessionManager;
	private final SessionConfig sessionConfig;
	private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<>();
	
	private String sessionId;
	private long lastAccessedTime;
	private boolean invalid;


	
	ElasticsearchSession(String sessionId, EtmConfiguration etmConfiguration, ElasticsearchSessionManager sessionManager, SessionConfig sessionConfig) {
		this.sessionId = sessionId;
		this.creationTime = lastAccessedTime = System.currentTimeMillis();
		this.etmConfiguration = etmConfiguration;
		this.sessionManager = sessionManager;
		this.sessionConfig = sessionConfig;
	}
	
	@Override
	public String getId() {
		return this.sessionId;
	}

	@Override
	public void requestDone(HttpServerExchange serverExchange) {
		if (this.invalid) {
			return;
		}
		this.lastAccessedTime = System.currentTimeMillis();
		String lowerCasePath = serverExchange.getRelativePath().toLowerCase();
		if (lowerCasePath.endsWith(".html") || lowerCasePath.contains("/rest/")) {
			this.sessionManager.persistSession(this);
		}
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
	
	void setLastAccessedTime(long lastAccessedTime) {
        if (this.invalid) {
            throw UndertowMessages.MESSAGES.sessionIsInvalid(sessionId);
        }
		this.lastAccessedTime = lastAccessedTime;
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
        return (int)(this.etmConfiguration.getSessionTimeout() / 1000);
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
        if (name.equals(CachedAuthenticatedSessionHandler.ATTRIBUTE_NAME) || name.equals(SSO_ATTRIBUTE)) {
        	// A little hack because these attributes are set after the requestDone method is called.
        	this.sessionManager.persistSession(this);
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
        this.sessionManager.getSessionListeners().sessionDestroyed(this, exchange, SessionListener.SessionDestroyedReason.INVALIDATED);
        this.sessionManager.removeSession(this);
        this.invalid = true;

        if (exchange != null && this.sessionConfig != null) {
            this.sessionConfig.clearSession(exchange, this.getId());
        }
        if(exchange != null) {
            exchange.removeAttachment(ETM_SESSION);
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
