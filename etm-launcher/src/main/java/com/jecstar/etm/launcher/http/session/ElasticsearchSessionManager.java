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

package com.jecstar.etm.launcher.http.session;

import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.DeleteRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.GetRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.IndexRequestBuilder;
import io.undertow.UndertowMessages;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.*;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

class ElasticsearchSessionManager implements SessionManager {

    private final DataRepository dataRepository;
    private final SessionIdGenerator sessionIdGenerator;
    private final String deploymentName;
    private final EtmConfiguration etmConfiguration;

    private final SessionListeners sessionListeners = new SessionListeners();
    private final ElasticsearchSessionConverter<String> converter = new ElasticsearchSessionConverterJsonImpl();


    public ElasticsearchSessionManager(DataRepository dataRepository, SessionIdGenerator sessionIdGenerator, String deploymentName, EtmConfiguration etmConfiguration) {
        this.dataRepository = dataRepository;
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
            sessionId = createSessionId();
            GetResponse getResponse = this.dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.STATE_INDEX_NAME,
                    ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION_ID_PREFIX + hashSessionId(sessionId))
                    .setFetchSource(false));
            if (getResponse.isExists()) {
                sessionId = null;
            }
            if (count++ == 100) {
                throw UndertowMessages.MESSAGES.couldNotGenerateUniqueSessionId();
            }
        }
        final ElasticsearchSession session = new ElasticsearchSession(sessionId, this.etmConfiguration, this, sessionCookieConfig);
        this.persistSession(session);
        sessionCookieConfig.setSessionId(serverExchange, session.getId());
        this.sessionListeners.sessionCreated(session, serverExchange);
        serverExchange.putAttachment(ElasticsearchSession.ETM_SESSION, session);
        return session;
    }

    @Override
    public Session getSession(HttpServerExchange serverExchange, SessionConfig sessionCookieConfig) {
        if (serverExchange != null) {
            ElasticsearchSession newSession = serverExchange.getAttachment(ElasticsearchSession.ETM_SESSION);
            if (newSession != null) {
                return newSession;
            }
        }
        String sessionId = sessionCookieConfig.findSessionId(serverExchange);
        return getSession(sessionId, sessionCookieConfig);
    }

    @Override
    public Session getSession(String sessionId) {
        return getSession(sessionId, null);
    }

    private Session getSession(String sessionId, SessionConfig sessionConfig) {
        if (sessionId == null) {
            return null;
        }
        GetResponse getResponse = this.dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.STATE_INDEX_NAME,
                ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION_ID_PREFIX + hashSessionId(sessionId))
                .setFetchSource(true));
        if (!getResponse.isExists()) {
            return null;
        }
        ElasticsearchSession session = new ElasticsearchSession(sessionId, this.etmConfiguration, this, sessionConfig);
        this.converter.read(getResponse.getSourceAsString(), session);
        long timeout = this.etmConfiguration.getSessionTimeout();
        if (session.getLastAccessedTime() + timeout < System.currentTimeMillis()) {
            return null;
        }
        return session;
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
    }

    @Override
    public Set<String> getTransientSessions() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getActiveSessions() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getAllSessions() {
        return Collections.emptySet();
    }

    @Override
    public SessionManagerStatistics getStatistics() {
        return null;
    }

    void persistSession(ElasticsearchSession session) {
        IndexRequestBuilder builder = new IndexRequestBuilder(ElasticsearchLayout.STATE_INDEX_NAME,
                ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION_ID_PREFIX + hashSessionId(session.getId()))
                .setSource(this.converter.write(session), XContentType.JSON)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
        this.dataRepository.index(builder);
    }

    void removeSession(ElasticsearchSession session) {
        DeleteRequestBuilder builder = new DeleteRequestBuilder(ElasticsearchLayout.STATE_INDEX_NAME,
                ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION_ID_PREFIX + hashSessionId(session.getId()))
                .setTimeout(TimeValue.timeValueMillis(this.etmConfiguration.getQueryTimeout()))
                .setWaitForActiveShards(getActiveShardCount(this.etmConfiguration));
        this.dataRepository.delete(builder);
    }

    String createSessionId() {
        return this.sessionIdGenerator.createSessionId();
    }

    void changeSessionId(String oldId, String newId) {
        GetResponse getResponse = this.dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.STATE_INDEX_NAME,
                ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION_ID_PREFIX + hashSessionId(oldId))
                .setFetchSource(true));
        if (getResponse.isExists()) {
            IndexRequestBuilder indexRequestBuilder = new IndexRequestBuilder(ElasticsearchLayout.STATE_INDEX_NAME,
                    ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION_ID_PREFIX + hashSessionId(newId))
                    .setSource(getResponse.getSource())
                    .setTimeout(TimeValue.timeValueMillis(this.etmConfiguration.getQueryTimeout()))
                    .setWaitForActiveShards(getActiveShardCount(this.etmConfiguration));
            this.dataRepository.index(indexRequestBuilder);

            DeleteRequestBuilder deleteRequestBuilder = new DeleteRequestBuilder(ElasticsearchLayout.STATE_INDEX_NAME,
                    ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION_ID_PREFIX + hashSessionId(oldId))
                    .setTimeout(TimeValue.timeValueMillis(this.etmConfiguration.getQueryTimeout()))
                    .setWaitForActiveShards(getActiveShardCount(this.etmConfiguration));
            this.dataRepository.delete(deleteRequestBuilder);
        }
    }

    private ActiveShardCount getActiveShardCount(EtmConfiguration etmConfiguration) {
        if (-1 == etmConfiguration.getWaitForActiveShards()) {
            return ActiveShardCount.ALL;
        } else if (0 == etmConfiguration.getWaitForActiveShards()) {
            return ActiveShardCount.NONE;
        }
        return ActiveShardCount.from(etmConfiguration.getWaitForActiveShards());
    }

    private String hashSessionId(String sessionId) {
        byte[] bytes = sessionId.getBytes(StandardCharsets.UTF_8);
        int h = 1, i = 0;
        for (; i + 7 < bytes.length; i += 8) {
            h = 31 * 31 * 31 * 31 * 31 * 31 * 31 * 31 * h
                    + 31 * 31 * 31 * 31 * 31 * 31 * 31 * bytes[i]
                    + 31 * 31 * 31 * 31 * 31 * 31 * bytes[i + 1]
                    + 31 * 31 * 31 * 31 * 31 * bytes[i + 2]
                    + 31 * 31 * 31 * 31 * bytes[i + 3]
                    + 31 * 31 * 31 * bytes[i + 4]
                    + 31 * 31 * bytes[i + 5]
                    + 31 * bytes[i + 6]
                    + bytes[i + 7];
        }
        for (; i + 3 < bytes.length; i += 4) {
            h = 31 * 31 * 31 * 31 * h
                    + 31 * 31 * 31 * bytes[i]
                    + 31 * 31 * bytes[i + 1]
                    + 31 * bytes[i + 2]
                    + bytes[i + 3];
        }
        for (; i < bytes.length; i++) {
            h = 31 * h + bytes[i];
        }
        return Integer.toString(h);
    }

}
