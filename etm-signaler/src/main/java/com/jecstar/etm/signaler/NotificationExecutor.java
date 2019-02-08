package com.jecstar.etm.signaler;

import com.jecstar.etm.server.core.domain.cluster.notifier.EmailNotifier;
import com.jecstar.etm.server.core.domain.cluster.notifier.Notifier;
import com.jecstar.etm.server.core.domain.cluster.notifier.SnmpNotifier;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.domain.principal.EtmSecurityEntity;
import com.jecstar.etm.server.core.persisting.internal.BusinessEventLogger;
import com.jecstar.etm.signaler.domain.Signal;
import org.elasticsearch.client.Client;
import org.joda.time.DateTime;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Interface for all classes that are able execute a notification.
 */
public class NotificationExecutor implements Closeable {


    private final JsonConverter jsonConverter = new JsonConverter();
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
     *
     * @param client               The Elasticsearch client.
     * @param etmConfiguration     The <code>EtmConfiguration</code> instance.
     * @param clusterName          The name of the ETM cluster.
     * @param signal               The <code>Signal</code> of which the threshold is exceeded more often that the configured limit.
     * @param notifier             The <code>Notifier</code> to be used to send the actual notification.
     * @param thresholdExceedances A <code>Map</code> with dates and their values when the threshold was exceeded.
     * @param etmSecurityEntity    An <code>EtmSecurityEntity/code> to which the <code>Signal</code> belongs.
     */
    public void notifyExceedance(Client client,
                                 EtmConfiguration etmConfiguration,
                                 String clusterName,
                                 Signal signal,
                                 Notifier notifier,
                                 Map<DateTime, Double> thresholdExceedances,
                                 EtmSecurityEntity etmSecurityEntity,
                                 long systemStartTime
    ) {
        if (Notifier.NotifierType.EMAIL.equals(notifier.getNotifierType())) {
            this.emailSignal.sendExceedanceNotification(client, etmConfiguration, clusterName, signal, (EmailNotifier) notifier, thresholdExceedances, etmSecurityEntity);
        } else if (Notifier.NotifierType.SNMP.equals(notifier.getNotifierType())) {
            this.snmpSignal.sendExceedanceNotification(clusterName, signal, (SnmpNotifier) notifier, thresholdExceedances, systemStartTime);
        } else if (Notifier.NotifierType.ETM_BUSINESS_EVENT.equals(notifier.getNotifierType())) {
            final StringBuilder buffer = new StringBuilder();
            buffer.append("{");
            this.jsonConverter.addStringElementToJsonBuffer("signal", signal.getName(), buffer, true);
            this.jsonConverter.addStringElementToJsonBuffer("owner", etmSecurityEntity.getType(), buffer, false);
            this.jsonConverter.addStringElementToJsonBuffer("owner_id", etmSecurityEntity.getId(), buffer, false);
            this.jsonConverter.addDoubleElementToJsonBuffer("threshold", signal.getThreshold().getValue(), buffer, false);
            this.jsonConverter.addIntegerElementToJsonBuffer("max_frequency_of_exceedance", signal.getNotifications().getMaxFrequencyOfExceedance(), buffer, false);
            List<DateTime> keys = new ArrayList<>(thresholdExceedances.keySet());
            Collections.sort(keys);
            buffer.append(", " + this.jsonConverter.escapeToJson("threshold_exceedances", true) + ": [");
            buffer.append(
                    keys.stream().map(k -> "{"
                            + "\"timestamp\": " + k.getMillis()
                            + ",\"value\": " + thresholdExceedances.get(k)
                            + "}").collect(Collectors.joining(","))
            );
            buffer.append("]}");

            BusinessEventLogger.logSignalThresholdExceeded(buffer.toString());
        }
    }

    /**
     * Notify an exceedance fixed <code>Signal</code> to a <code>Notifier</code>.
     *
     * @param client            The Elasticsearch client.
     * @param etmConfiguration  The <code>EtmConfiguration</code> instance.
     * @param clusterName       The name of the ETM cluster.
     * @param signal            The <code>Signal</code> of which the threshold is no longer exceeded.
     * @param notifier          The <code>Notifier</code> to be used to send the actual notification.
     * @param etmSecurityEntity An <code>EtmSecurityEntity</code> to which the <code>Signal</code> belongs.
     */
    public void notifyNoLongerExceeded(Client client,
                                       EtmConfiguration etmConfiguration,
                                       String clusterName,
                                       Signal signal,
                                       Notifier notifier,
                                       EtmSecurityEntity etmSecurityEntity
    ) {
        if (Notifier.NotifierType.EMAIL.equals(notifier.getNotifierType())) {
            this.emailSignal.sendNoLongerExceededNotification(client, etmConfiguration, clusterName, signal, (EmailNotifier) notifier, etmSecurityEntity);
        } else if (Notifier.NotifierType.ETM_BUSINESS_EVENT.equals(notifier.getNotifierType())) {
            final StringBuilder buffer = new StringBuilder();
            buffer.append("{");
            this.jsonConverter.addStringElementToJsonBuffer("signal", signal.getName(), buffer, true);
            this.jsonConverter.addStringElementToJsonBuffer("owner", etmSecurityEntity.getType(), buffer, false);
            this.jsonConverter.addStringElementToJsonBuffer("owner_id", etmSecurityEntity.getId(), buffer, false);
            this.jsonConverter.addDoubleElementToJsonBuffer("threshold", signal.getThreshold().getValue(), buffer, false);
            this.jsonConverter.addIntegerElementToJsonBuffer("max_frequency_of_exceedance", signal.getNotifications().getMaxFrequencyOfExceedance(), buffer, false);
            buffer.append("}");
            BusinessEventLogger.logSignalThresholdNoLongerExceeded(buffer.toString());
        }
    }

    @Override
    public void close() {
        this.emailSignal.close();
    }
}
