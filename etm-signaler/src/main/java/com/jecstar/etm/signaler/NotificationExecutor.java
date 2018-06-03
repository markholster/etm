package com.jecstar.etm.signaler;

import com.jecstar.etm.server.core.domain.cluster.notifier.Notifier;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
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

    /**
     * Notify an exceedance <code>Signal</code> to a <code>Notifier</code>.
     *
     * @param client               The Elasticsearch client.
     * @param etmConfiguration     The <code>EtmConfiguration</code> instance.
     * @param clusterName          The name of the ETM cluster.
     * @param signal               The <code>Signal</code> of which the threshold is exceeded more often that the configured limit.
     * @param notifier             The <code>Notifier</code> to be used to send the actual notification.
     * @param thresholdExceedances A <code>Map</code> with dates and their values when the threshold was exceeded.
     * @param etmPrincipal         An <code>EtmPrincipal</code> to which the <code>Signal</code> belongs. <code>null</code> if
     *                             the <code>Signal</code> belongs to an <code>EtmGroup</code>.
     * @param etmGroup             An <code>EtmGroup</code> to which the <code>Signal</code> belongs. <code>null</code> if the
     */
    public void notifyExceedance(Client client,
                                 EtmConfiguration etmConfiguration,
                                 String clusterName,
                                 Signal signal,
                                 Notifier notifier,
                                 Map<DateTime, Double> thresholdExceedances,
                                 EtmPrincipal etmPrincipal,
                                 EtmGroup etmGroup
    ) {
        if (Notifier.NotifierType.EMAIL.equals(notifier.getNotifierType())) {
            this.emailSignal.sendExceedanceNotification(client, etmConfiguration, clusterName, signal, notifier, thresholdExceedances, etmGroup);
        } else if (Notifier.NotifierType.ETM_BUSINESS_EVENT.equals(notifier.getNotifierType())) {
            final StringBuilder buffer = new StringBuilder();
            buffer.append("{");
            this.jsonConverter.addStringElementToJsonBuffer("signal", signal.getName(), buffer, true);
            this.jsonConverter.addStringElementToJsonBuffer("owner", etmPrincipal != null ? "user" : "group", buffer, false);
            this.jsonConverter.addStringElementToJsonBuffer("owner_id", etmPrincipal != null ? etmPrincipal.getId() : etmGroup.getName(), buffer, false);
            this.jsonConverter.addIntegerElementToJsonBuffer("threshold", signal.getThreshold(), buffer, false);
            this.jsonConverter.addIntegerElementToJsonBuffer("limit", signal.getLimit(), buffer, false);
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
     * @param client           The Elasticsearch client.
     * @param etmConfiguration The <code>EtmConfiguration</code> instance.
     * @param clusterName      The name of the ETM cluster.
     * @param signal           The <code>Signal</code> of which the threshold is no longer exceeded.
     * @param notifier         The <code>Notifier</code> to be used to send the actual notification.
     * @param etmPrincipal     An <code>EtmPrincipal</code> to which the <code>Signal</code> belongs. <code>null</code> if
     *                         the <code>Signal</code> belongs to an <code>EtmGroup</code>.
     * @param etmGroup         An <code>EtmGroup</code> to which the <code>Signal</code> belongs. <code>null</code> if the
     */
    public void notifyNoLongerExceeded(Client client,
                                       EtmConfiguration etmConfiguration,
                                       String clusterName,
                                       Signal signal,
                                       Notifier notifier,
                                       EtmPrincipal etmPrincipal,
                                       EtmGroup etmGroup
    ) {
        if (Notifier.NotifierType.EMAIL.equals(notifier.getNotifierType())) {
            this.emailSignal.sendNoLongerExceededNotification(client, etmConfiguration, clusterName, signal, notifier, etmGroup);
        } else if (Notifier.NotifierType.ETM_BUSINESS_EVENT.equals(notifier.getNotifierType())) {
            final StringBuilder buffer = new StringBuilder();
            buffer.append("{");
            this.jsonConverter.addStringElementToJsonBuffer("signal", signal.getName(), buffer, true);
            this.jsonConverter.addStringElementToJsonBuffer("owner", etmPrincipal != null ? "user" : "group", buffer, false);
            this.jsonConverter.addStringElementToJsonBuffer("owner_id", etmPrincipal != null ? etmPrincipal.getId() : etmGroup.getName(), buffer, false);
            this.jsonConverter.addIntegerElementToJsonBuffer("threshold", signal.getThreshold(), buffer, false);
            this.jsonConverter.addIntegerElementToJsonBuffer("limit", signal.getLimit(), buffer, false);
            buffer.append("}");
            BusinessEventLogger.logSignalThresholdNoLongerExceeded(buffer.toString());
        }
    }

    @Override
    public void close() {
        this.emailSignal.close();
    }
}
