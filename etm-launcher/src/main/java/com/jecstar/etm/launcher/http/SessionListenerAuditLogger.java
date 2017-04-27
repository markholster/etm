package com.jecstar.etm.launcher.http;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;

import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.EtmPrincipal;
import com.jecstar.etm.server.core.domain.audit.LogoutAuditLog;
import com.jecstar.etm.server.core.domain.audit.builders.LogoutAuditLogBuilder;
import com.jecstar.etm.server.core.domain.converter.AuditLogConverter;
import com.jecstar.etm.server.core.domain.converter.json.LogoutAuditLogConverterJsonImpl;
import com.jecstar.etm.server.core.util.DateUtils;

import io.undertow.security.idm.Account;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;

public class SessionListenerAuditLogger implements SessionListener {

	private static final DateTimeFormatter dateTimeFormatterIndexPerDay = DateUtils.getIndexPerDayFormatter();

	private final Client client;
	private final EtmConfiguration etmConfiguration;
	private final AuditLogConverter<String, LogoutAuditLog> auditLogConverter = new LogoutAuditLogConverterJsonImpl();

	public SessionListenerAuditLogger(Client client, EtmConfiguration etmConfiguration) {
		this.client = client;
		this.etmConfiguration = etmConfiguration;
	}

	private void logLogout(String id, boolean expired) {
		ZonedDateTime now = ZonedDateTime.now();
		LogoutAuditLogBuilder auditLogBuilder = new LogoutAuditLogBuilder().setTimestamp(now).setHandlingTime(now)
				.setPrincipalId(id).setExpired(expired);
		client.prepareIndex(ElasticSearchLayout.ETM_AUDIT_LOG_INDEX_PREFIX + dateTimeFormatterIndexPerDay.format(now),
				ElasticSearchLayout.ETM_AUDIT_LOG_INDEX_TYPE_LOGOUT)
				.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
				.setSource(this.auditLogConverter.write(auditLogBuilder.build()), XContentType.JSON).execute();
	}

	private ActiveShardCount getActiveShardCount(EtmConfiguration etmConfiguration) {
		if (-1 == etmConfiguration.getWaitForActiveShards()) {
			return ActiveShardCount.ALL;
		} else if (0 == etmConfiguration.getWaitForActiveShards()) {
			return ActiveShardCount.NONE;
		}
		return ActiveShardCount.from(etmConfiguration.getWaitForActiveShards());
	}

	@Override
	public void sessionCreated(Session session, HttpServerExchange exchange) {
	}

	@Override
	public void sessionDestroyed(Session session, HttpServerExchange exchange, SessionDestroyedReason reason) {
		Account account = exchange.getSecurityContext().getAuthenticatedAccount();
		String id = null;
		if (account != null) {
			id = ((EtmPrincipal)account.getPrincipal()).getId();
		}
		if (id == null) {
			return;
		}
		switch (reason) {
		case INVALIDATED:
			logLogout(id, false);
			break;
		case TIMEOUT:
			logLogout(id, true);
			break;
		case UNDEPLOY:
			logLogout(id, false);
			break;
		}
	}

	@Override
	public void attributeAdded(Session session, String name, Object value) {
	}

	@Override
	public void attributeUpdated(Session session, String name, Object newValue, Object oldValue) {
	}

	@Override
	public void attributeRemoved(Session session, String name, Object oldValue) {
	}

	@Override
	public void sessionIdChanged(Session session, String oldSessionId) {
	}

}
