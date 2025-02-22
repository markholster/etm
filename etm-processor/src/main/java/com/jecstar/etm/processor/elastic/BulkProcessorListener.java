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

package com.jecstar.etm.processor.elastic;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

class BulkProcessorListener implements BulkProcessor.Listener {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(PersistenceEnvironmentElasticImpl.class);

    private static final int MAX_MODIFICATIONS_BEFORE_BLACKLISTING = 500;
    private static final int BLACKLIST_TIME = 60 * 60 * 1000; // Blacklist potential dangerous events for an hour.

    private final Timer bulkTimer;
    private final Map<Long, Context> metricContext = new ConcurrentHashMap<>();
    private final Map<String, Long> blacklistedIds = new ConcurrentHashMap<>();

    BulkProcessorListener(final MetricRegistry metricRegistry) {
        this.bulkTimer = metricRegistry.timer("event-processor.persisting-repository-bulk-update");
        Gauge<Integer> blacklistGauge = BulkProcessorListener.this.blacklistedIds::size;
        metricRegistry.register("event-processor.blacklist_size", blacklistGauge);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void beforeBulk(long executionId, BulkRequest request) {
        this.metricContext.put(executionId, this.bulkTimer.time());
        cleanupBlacklist();
        Iterator<DocWriteRequest<?>> it = request.requests().iterator();
        while (it.hasNext()) {
            DocWriteRequest action = it.next();
            if (isBlacklisted(action)) {
                if (log.isDebugLevelEnabled()) {
                    log.logDebugMessage("Event with id '" + getEventId(action) + " is currently blacklisted and will not be persisted.");
                }
                it.remove();
            }
        }
    }

    @Override
    public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
        for (BulkItemResponse itemResponse : response.getItems()) {
            if (itemResponse.getVersion() > MAX_MODIFICATIONS_BEFORE_BLACKLISTING) {
                addToBlacklist(itemResponse.getId());
            }
        }
        if (response.hasFailures() && log.isErrorLevelEnabled()) {
            log.logErrorMessage(buildFailureMessage(response));
        }
        this.metricContext.remove(executionId).stop();
    }

    @Override
    public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
        if (log.isErrorLevelEnabled()) {
            log.logErrorMessage("Failure in bulk execution", failure);
        }
        this.metricContext.remove(executionId).stop();
    }

    private boolean isBlacklisted(DocWriteRequest<?> action) {
        String id = getEventId(action);
        if (id == null) {
            return false;
        }
        return this.blacklistedIds.containsKey(id);
    }

    private String getEventId(DocWriteRequest<?> action) {
        if (action instanceof IndexRequest) {
            return action.id();
        } else if (action instanceof UpdateRequest) {
            return action.id();
        } else if (action instanceof DeleteRequest) {
            return action.id();
        } else {
            throw new IllegalArgumentException("No support for request [" + action + "]");
        }
    }


    private void addToBlacklist(String id) {
        if (!this.blacklistedIds.containsKey(id)) {
            this.blacklistedIds.put(id, System.currentTimeMillis());
            if (log.isInfoLevelEnabled()) {
                log.logInfoMessage("Event id '" + id + "' blacklisted for " + (BLACKLIST_TIME / 1000) + " seconds.");
            }
        }
    }

    private void cleanupBlacklist() {
        final long startTime = System.currentTimeMillis();
        Iterator<Entry<String, Long>> iterator = this.blacklistedIds.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, Long> entry = iterator.next();
            if (startTime > (entry.getValue() + BLACKLIST_TIME)) {
                if (log.isInfoLevelEnabled()) {
                    log.logInfoMessage("Event id '" + entry.getKey() + "' no longer blacklisted.");
                }
                iterator.remove();
            }
        }
    }

    private String buildFailureMessage(BulkResponse bulkResponse) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("failure in bulk execution:");
        BulkItemResponse[] responses = bulkResponse.getItems();
        for (int i = 0; i < responses.length; i++) {
            BulkItemResponse response = responses[i];
            if (response.isFailed()) {
                buffer.append("\n[").append(i)
                        .append("]: index [").append(response.getIndex()).append("], type [").append(response.getType()).append("], id [").append(response.getId())
                        .append("], message [").append(getFailureMessage(response)).append("]");
                if (log.isDebugLevelEnabled() && response.getFailure() != null) {
                    log.logDebugMessage("Error persisting event with id '" + response.getId() + "'.", response.getFailure().getCause());
                }
            }
        }
        return buffer.toString();
    }

    private String getFailureMessage(BulkItemResponse response) {
        if (response.getFailure() == null) {
            return null;
        }
        Throwable cause = response.getFailure().getCause();
        if (cause != null) {
            List<Throwable> causes = new ArrayList<>();
            Throwable rootCause = getRootCause(cause, causes);
            return rootCause.getMessage();
        }
        return response.getFailureMessage();
    }

    private Throwable getRootCause(Throwable cause, List<Throwable> causes) {
        if (causes.contains(cause)) {
            return causes.get(causes.size() - 1);
        }
        causes.add(cause);
        Throwable parent = cause.getCause();
        if (parent == null) {
            return cause;
        }
        return getRootCause(parent, causes);
    }


}
