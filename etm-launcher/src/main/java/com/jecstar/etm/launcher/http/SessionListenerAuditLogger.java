/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.launcher.http;

import com.jecstar.etm.server.core.domain.audit.builder.LogoutAuditLogBuilder;
import com.jecstar.etm.server.core.domain.audit.converter.LogoutAuditLogConverter;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.IndexRequestBuilder;
import com.jecstar.etm.server.core.util.DateUtils;
import com.jecstar.etm.server.core.util.IdGenerator;
import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;
import io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

class SessionListenerAuditLogger implements SessionListener {

    /**
     * An <code>IdGenerator</code> that will be used to create id's for audit logs.
     */
    private static final IdGenerator idGenerator = new IdGenerator();

    private static final DateTimeFormatter dateTimeFormatterIndexPerWeek = DateUtils.getIndexPerWeekFormatter();

    private final DataRepository dataRepository;
    private final EtmConfiguration etmConfiguration;
    private final LogoutAuditLogConverter auditLogConverter = new LogoutAuditLogConverter();

    public SessionListenerAuditLogger(DataRepository dataRepository, EtmConfiguration etmConfiguration) {
        this.dataRepository = dataRepository;
        this.etmConfiguration = etmConfiguration;
    }

    private void logLogout(String id, boolean expired) {
        Instant now = Instant.now();
        LogoutAuditLogBuilder auditLogBuilder = new LogoutAuditLogBuilder()
                .setId(idGenerator.createId())
                .setTimestamp(now)
                .setHandlingTime(now)
                .setPrincipalId(id)
                .setExpired(expired);
        dataRepository.indexAsync(new IndexRequestBuilder(ElasticsearchLayout.AUDIT_LOG_INDEX_PREFIX + dateTimeFormatterIndexPerWeek.format(now))
                        .setId(auditLogBuilder.getId())
                        .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                        .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                        .setSource(this.auditLogConverter.write(auditLogBuilder.build()), XContentType.JSON)
                , DataRepository.noopActionListener()
        );
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
        String id = null;
        if (exchange != null && exchange.getSecurityContext().getAuthenticatedAccount() != null) {
            id = ((EtmPrincipal) exchange.getSecurityContext().getAuthenticatedAccount().getPrincipal()).getId();
        } else {
            AuthenticatedSession authenticatedSession = (AuthenticatedSession) session.getAttribute(CachedAuthenticatedSessionHandler.ATTRIBUTE_NAME);
            if (authenticatedSession != null) {
                id = ((EtmPrincipal) authenticatedSession.getAccount().getPrincipal()).getId();
            }
        }
        if (id == null) {
            return;
        }
        switch (reason) {
            case INVALIDATED:
            case UNDEPLOY:
                logLogout(id, false);
                break;
            case TIMEOUT:
                logLogout(id, true);
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
