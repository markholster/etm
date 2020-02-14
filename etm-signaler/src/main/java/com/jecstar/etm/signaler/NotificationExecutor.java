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

package com.jecstar.etm.signaler;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.server.core.domain.cluster.notifier.EmailNotifier;
import com.jecstar.etm.server.core.domain.cluster.notifier.Notifier;
import com.jecstar.etm.server.core.domain.cluster.notifier.SnmpNotifier;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.EtmSecurityEntity;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.persisting.internal.BusinessEventLogger;
import com.jecstar.etm.signaler.domain.Signal;

import java.io.Closeable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Interface for all classes that are able execute a notification.
 */
public class NotificationExecutor implements Closeable {


    private final EmailSignal emailSignal = new EmailSignal();
    private final SnmpSignal snmpSignal;

    /**
     * Constructs a new <code>NotificationExecutor</code> instance.
     *
     * @param snmpEngineId The engine id used for SNMPv3 notifications.
     */
    NotificationExecutor(byte[] snmpEngineId) {
        this.snmpSignal = new SnmpSignal(snmpEngineId);
    }

    /**
     * Notify an exceedance <code>Signal</code> to a <code>Notifier</code>.
     * @param dataRepository       The Elasticsearch dataRepository.
     * @param etmConfiguration     The <code>EtmConfiguration</code> instance.
     * @param clusterName          The name of the ETM cluster.
     * @param signal               The <code>Signal</code> of which the threshold is exceeded more often that the configured limit.
     * @param notifier             The <code>Notifier</code> to be used to send the actual notification.
     * @param thresholdExceedances A <code>Map</code> with dates and their values when the threshold was exceeded.
     * @param etmSecurityEntity    An <code>EtmSecurityEntity/code> to which the <code>Signal</code> belongs.
     */
    public void notifyExceedance(DataRepository dataRepository,
                                 EtmConfiguration etmConfiguration,
                                 String clusterName,
                                 Signal signal,
                                 Notifier notifier,
                                 Map<ZonedDateTime, Double> thresholdExceedances,
                                 EtmSecurityEntity etmSecurityEntity,
                                 long systemStartTime
    ) {
        if (Notifier.NotifierType.EMAIL.equals(notifier.getNotifierType())) {
            this.emailSignal.sendExceedanceNotification(dataRepository, etmConfiguration, clusterName, signal, (EmailNotifier) notifier, thresholdExceedances, etmSecurityEntity);
        } else if (Notifier.NotifierType.SNMP.equals(notifier.getNotifierType())) {
            this.snmpSignal.sendExceedanceNotification(clusterName, signal, (SnmpNotifier) notifier, thresholdExceedances, systemStartTime);
        } else if (Notifier.NotifierType.ETM_BUSINESS_EVENT.equals(notifier.getNotifierType())) {
            final var builder = new JsonBuilder();
            builder.startObject();
            builder.field("signal", signal.getName());
            builder.field("owner", etmSecurityEntity.getType());
            builder.field("owner_id", etmSecurityEntity.getId());
            builder.field("threshold", signal.getThreshold().getValue());
            builder.field("max_frequency_of_exceedance", signal.getNotifications().getMaxFrequencyOfExceedance());
            List<ZonedDateTime> keys = new ArrayList<>(thresholdExceedances.keySet());
            Collections.sort(keys);
            builder.startArray("threshold_exceedances");
            for (var key : keys) {
                builder.startObject();
                builder.field("timestamp", key.toInstant());
                builder.field("value", thresholdExceedances.get(key));
                builder.endObject();
            }
            builder.endArray().endObject();
            BusinessEventLogger.logSignalThresholdExceeded(builder.build());
        }
    }

    /**
     * Notify an exceedance fixed <code>Signal</code> to a <code>Notifier</code>.
     *  @param dataRepository  The <code>DataRepository</code>.
     * @param etmConfiguration  The <code>EtmConfiguration</code> instance.
     * @param clusterName       The name of the ETM cluster.
     * @param signal            The <code>Signal</code> of which the threshold is no longer exceeded.
     * @param notifier          The <code>Notifier</code> to be used to send the actual notification.
     * @param etmSecurityEntity An <code>EtmSecurityEntity</code> to which the <code>Signal</code> belongs.
     */
    public void notifyNoLongerExceeded(DataRepository dataRepository,
                                       EtmConfiguration etmConfiguration,
                                       String clusterName,
                                       Signal signal,
                                       Notifier notifier,
                                       EtmSecurityEntity etmSecurityEntity
    ) {
        if (Notifier.NotifierType.EMAIL.equals(notifier.getNotifierType())) {
            this.emailSignal.sendNoLongerExceededNotification(dataRepository, etmConfiguration, clusterName, signal, (EmailNotifier) notifier, etmSecurityEntity);
        } else if (Notifier.NotifierType.ETM_BUSINESS_EVENT.equals(notifier.getNotifierType())) {
            final var builder = new JsonBuilder();
            builder.startObject();
            builder.field("signal", signal.getName());
            builder.field("owner", etmSecurityEntity.getType());
            builder.field("owner_id", etmSecurityEntity.getId());
            builder.field("threshold", signal.getThreshold().getValue());
            builder.field("max_frequency_of_exceedance", signal.getNotifications().getMaxFrequencyOfExceedance());
            builder.endObject();
            BusinessEventLogger.logSignalThresholdNoLongerExceeded(builder.build());
        }
    }

    @Override
    public void close() {
        this.emailSignal.close();
    }
}
