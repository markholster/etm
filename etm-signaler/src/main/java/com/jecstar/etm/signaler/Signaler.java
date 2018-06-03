package com.jecstar.etm.signaler;

import com.jecstar.etm.server.core.domain.cluster.notifier.Notifier;
import com.jecstar.etm.server.core.domain.cluster.notifier.converter.NotifierConverter;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import com.jecstar.etm.server.core.rest.AbstractJsonService;
import com.jecstar.etm.signaler.backoff.BackoffPolicy;
import com.jecstar.etm.signaler.domain.Signal;
import com.jecstar.etm.signaler.domain.converter.SignalConverter;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentileRanks;
import org.elasticsearch.search.aggregations.metrics.percentiles.Percentiles;
import org.joda.time.DateTime;

import java.time.ZonedDateTime;
import java.util.*;

/**
 * Starting class for the alerting functionality. This class will request an alerting claim. When acknowledged by the
 * cluster, it will execute all alerts.
 */
public class Signaler extends AbstractJsonService implements Runnable {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(Signaler.class);
    private static final int MAX_RETRIES = 5;

    private final String clusterName;
    private final Client client;
    private final EtmConfiguration etmConfiguration;
    private final EtmPrincipalConverterJsonImpl etmPrincipalConverter = new EtmPrincipalConverterJsonImpl();
    private final EtmPrincipalTags etmPrincipalTags = etmPrincipalConverter.getTags();
    private final SignalConverter signalConverter = new SignalConverter();
    private final NotifierConverter notifierConverter = new NotifierConverter();

    public Signaler(String clusterName, EtmConfiguration etmConfiguration, Client client) {
        this.clusterName = clusterName;
        this.etmConfiguration = etmConfiguration;
        this.client = client;
    }

    @Override
    public void run() {
        ZonedDateTime batchStart = ZonedDateTime.now();
        try (NotificationExecutor thresholdExceededNotifier = new NotificationExecutor()) {
            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
            boolQueryBuilder.should(new ExistsQueryBuilder(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP + "." + this.etmPrincipalTags.getSignalsTag()));
            boolQueryBuilder.should(new ExistsQueryBuilder(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.etmPrincipalTags.getSignalsTag()));
            boolQueryBuilder.minimumShouldMatch(1);

            ScrollableSearch scrollableSearch = new ScrollableSearch(
                    client,
                    enhanceRequest(
                            this.client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME),
                            this.etmConfiguration
                    ).setFetchSource(false).setQuery(boolQueryBuilder)
            );
            for (SearchHit searchHit : scrollableSearch)
                for (int i = 0; i < MAX_RETRIES; i++) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    if (handleEntity(batchStart, thresholdExceededNotifier, searchHit.getIndex(), searchHit.getType(), searchHit.getId())) {
                        break;
                    }
                }
        } catch (Exception e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Failed to handle signals", e);
            }
        }
    }

    /**
     * Method handles an entity. The entity will be retrieved, tested vor threshold exceedances and updated if necessary.
     *
     * @param batchStart           The moment the signal check batch started.
     * @param notificationExecutor The <code>NotificationExecutor</code> to be used to notify users.
     * @param index                The index of the entity.
     * @param type                 The type of the entity.
     * @param id                   The id of the entity.
     * @return <code>true</code> when the entity is handled, otherwise <code>false</code> will be returned.
     */
    private boolean handleEntity(ZonedDateTime batchStart, NotificationExecutor notificationExecutor, String index, String type, String id) {
        GetResponse getResponse = this.client.prepareGet(index, type, id)
                .setFetchSource(
                        new String[]{
                                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP + "." + this.etmPrincipalTags.getSignalsTag(),
                                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP + "." + this.etmPrincipalTags.getNameTag(),
                                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.etmPrincipalTags.getSignalsTag(),
                                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.etmPrincipalTags.getIdTag()
                        }
                        , null)
                .get();
        Map<String, Object> sourceMap = getResponse.getSourceAsMap();
        Map<String, Object> entityObjectMap = null;
        EtmPrincipal etmPrincipal = null;
        EtmGroup etmGroup = null;
        if (sourceMap.containsKey(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP)) {
            entityObjectMap = toMapWithoutNamespace(sourceMap, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP);
            String groupName = getString(this.etmPrincipalTags.getNameTag(), entityObjectMap);
            etmGroup = getEtmGroup(groupName);
        } else if (sourceMap.containsKey(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER)) {
            entityObjectMap = toMapWithoutNamespace(sourceMap, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER);
            String userId = getString(this.etmPrincipalTags.getIdTag(), entityObjectMap);
            etmPrincipal = getEtmPrincipal(userId);
        }
        List<Map<String, Object>> signals = getArray(this.etmPrincipalTags.getSignalsTag(), entityObjectMap);
        if (signals == null || signals.size() < 1) {
            return true;
        }
        Map<Signal, SignalTestResult> notificationMap = new HashMap<>();
        Set<Signal> fixedSet = new HashSet<>();
        boolean signalsUpdated = false;
        for (Map<String, Object> signalValues : signals) {
            if (Thread.currentThread().isInterrupted()) {
                return false;
            }
            ZonedDateTime lastExecuted = getZonedDateTime(Signal.LAST_EXECUTED, signalValues);
            Signal signal = this.signalConverter.read(signalValues);
            if (lastExecuted != null && ZonedDateTime.now().isBefore(lastExecuted.plus(signal.getIntervalDuration()))) {
                continue;
            }
            SignalTestResult result = testThresholds(signal, etmPrincipal, etmGroup);
            if (!result.isExecuted()) {
                break;
            }
            signalsUpdated = true;
            signalValues.put(Signal.LAST_EXECUTED, batchStart.toInstant().toEpochMilli());
            if (result.isLimitExceeded(signal)) {
                signalValues.put(Signal.LAST_FAILED, batchStart.toInstant().toEpochMilli());
                if (signalValues.containsKey(Signal.FAILED_SINCE)) {
                    long failedSince = getLong(Signal.FAILED_SINCE, signalValues);
                    result.setConsecutiveFailures(
                            Math.round(
                                    (float) (batchStart.toInstant().toEpochMilli() - failedSince)
                                            / (float) result.getTestInterval().toMillis()
                            )
                                    + 1
                    );
                } else {
                    signalValues.put(Signal.FAILED_SINCE, batchStart.toInstant().toEpochMilli());
                    result.setConsecutiveFailures(1);
                }
                notificationMap.put(signal, result);
            } else {
                if (signalValues.containsKey(Signal.FAILED_SINCE)) {
                    fixedSet.add(signal);
                }
                signalValues.remove(Signal.FAILED_SINCE);
                signalValues.put(Signal.LAST_PASSED, batchStart.toInstant().toEpochMilli());
            }
        }
        if (!signalsUpdated) {
            return true;
        }
        try {
            enhanceRequest(
                    this.client.prepareUpdate(index, type, id),
                    this.etmConfiguration
            ).setVersion(getResponse.getVersion())
                    .setDoc(sourceMap)
                    .setDocAsUpsert(true)
                    // No retry because we specify the version
                    .setRetryOnConflict(0)
                    .get();
        } catch (VersionConflictEngineException e) {
            // Another process has updated the entity. This could be another ETM instance that was running this signal
            // at exactly the same time. The only way to detect this is to retry this entire method. If another ETM instance
            // has fully executed this method the LAST_EXECUTED time is updated on all signals in this entity so it won't
            // be executed again.
            return false;
        }
        // Current version updated, so no other node in the cluster has came this far. We have to send the notifications now.
        for (Map.Entry<Signal, SignalTestResult> entry : notificationMap.entrySet()) {
            sendFailureNotifications(notificationExecutor, entry.getKey(), entry.getValue(), etmPrincipal, etmGroup);
        }
        for (Signal signal : fixedSet) {
            sendFixedNotifications(notificationExecutor, signal, etmPrincipal, etmGroup);
        }
        return true;
    }

    /**
     * Test a signal if the threshold is exceeded more often that the limit in the signal.
     *
     * @param signal       The <code>Signal</code> to test.
     * @param etmPrincipal The <code>EtmPrincipal</code> that has stored the given <code>Signal</code>. Set this value
     *                     to <code>null</code> when the <code>Signal</code> was added to an <code>EtmGroup</code>.
     * @param etmGroup     The <code>EtmGroup</code> that has stored the given <code>Signal</code>. Set this value
     *                     to <code>null</code> when the <code>Signal</code> was added to an <code>EtmPrincipal</code>.
     * @return A <code>SignalTestResult</code> instance with the test and notifyExceedance results.
     */
    private SignalTestResult testThresholds(Signal signal, EtmPrincipal etmPrincipal, EtmGroup etmGroup) {
        SignalTestResult result = new SignalTestResult();
        result.setTestInterval(signal.getIntervalDuration());
        SignalSearchRequestBuilderBuilder builderBuilder = new SignalSearchRequestBuilderBuilder(this.client, this.etmConfiguration)
                .setSignal(signal);
        SearchRequestBuilder builder;

        if (etmPrincipal != null) {
            builder = builderBuilder.build(q -> addFilterQuery(etmPrincipal, q), etmPrincipal);
        } else if (etmGroup != null) {
            builder = builderBuilder.build(q -> addFilterQuery(etmGroup, q, null), null);
        } else {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("No EtmPrincipal or EtmGroup available. Unable to test signal '" + signal.getName() + "'.");
            }
            return result.setExectued(false);
        }
        SearchResponse searchResponse = builder.get();
        result.setExectued(true);
        MultiBucketsAggregation aggregation = searchResponse.getAggregations().get(SignalSearchRequestBuilderBuilder.CARDINALITY_AGGREGATION_KEY);
        for (MultiBucketsAggregation.Bucket bucket : aggregation.getBuckets()) {
            DateTime dateTime = (DateTime) bucket.getKey();
            for (Aggregation subAggregation : bucket.getAggregations()) {
                Double aggregationValue = getMetricAggregationValueFromAggregator(subAggregation);
                if (aggregationValue != null && !aggregationValue.isNaN() && !aggregationValue.isInfinite() && signal.getThreshold() != null && signal.getComparison() != null) {
                    if (Signal.Comparison.LT.equals(signal.getComparison()) && aggregationValue.compareTo(signal.getThreshold().doubleValue()) < 0) {
                        result.addThresholdExceedance(dateTime, aggregationValue);
                    } else if (Signal.Comparison.LTE.equals(signal.getComparison()) && aggregationValue.compareTo(signal.getThreshold().doubleValue()) <= 0) {
                        result.addThresholdExceedance(dateTime, aggregationValue);
                    } else if (Signal.Comparison.EQ.equals(signal.getComparison()) && aggregationValue.compareTo(signal.getThreshold().doubleValue()) == 0) {
                        result.addThresholdExceedance(dateTime, aggregationValue);
                    } else if (Signal.Comparison.GTE.equals(signal.getComparison()) && aggregationValue.compareTo(signal.getThreshold().doubleValue()) >= 0) {
                        result.addThresholdExceedance(dateTime, aggregationValue);
                    } else if (Signal.Comparison.GT.equals(signal.getComparison()) && aggregationValue.compareTo(signal.getThreshold().doubleValue()) >= 0) {
                        result.addThresholdExceedance(dateTime, aggregationValue);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Send the failure notifications to the user(s).
     *
     * @param notificationExecutor The <code>NotificationExecutor</code> to be used to notify users.
     * @param signal               The <code>Signal</code> that has a threshold exceedance.
     * @param signalTestResult     The <code>SignalTestResult</code> instance.
     * @param etmPrincipal         The <code>EtmPrincipal</code> that owns the <code>Signal</code>. <code>null</code> when the <code>Signal</code> is owned by an <code>EtmGroup</code>.
     * @param etmGroup             The <code>EtmGroup</code> that owns the <code>Signal</code>. <code>null</code> when the <code>Signal</code> is owned by an <code>EtmPrincipal</code>.
     */
    private void sendFailureNotifications(NotificationExecutor notificationExecutor, Signal signal, SignalTestResult signalTestResult, EtmPrincipal etmPrincipal, EtmGroup etmGroup) {
        if (signal.getNotifiers() == null) {
            return;
        }
        for (String notifierName : signal.getNotifiers()) {
            Notifier notifier = getNotifier(notifierName);
            if (notifier == null) {
                if (log.isWarningLevelEnabled()) {
                    log.logWarningMessage("Notifier with name '"
                            + notifierName
                            + "' not found. Signal '"
                            + signal.getName()
                            + "' of " + (etmPrincipal != null ? "user '" + etmPrincipal.getId() + "'" : " group '" + etmGroup.getName() + "'")
                            + " has an exceeded limit that cannot be notified.");
                }
                continue;
            }
            BackoffPolicy backoffPolicy = signalTestResult.getNotificationBackoffPolicy(notifier);
            if (backoffPolicy.shouldBeNotified()) {
                notificationExecutor.notifyExceedance(this.client, this.etmConfiguration, this.clusterName, signal, notifier, signalTestResult.getThresholdExceedances(), etmPrincipal, etmGroup);
            }
        }
    }

    /**
     * Send the fixed notifications to the user(s).
     *
     * @param notificationExecutor The <code>NotificationExecutor</code> to be used to notify users.
     * @param signal               The <code>Signal</code> that no longer has a threshold exceedance.
     * @param etmPrincipal         The <code>EtmPrincipal</code> that owns the <code>Signal</code>. <code>null</code> when the <code>Signal</code> is owned by an <code>EtmGroup</code>.
     * @param etmGroup             The <code>EtmGroup</code> that owns the <code>Signal</code>. <code>null</code> when the <code>Signal</code> is owned by an <code>EtmPrincipal</code>.
     */
    private void sendFixedNotifications(NotificationExecutor notificationExecutor, Signal signal, EtmPrincipal etmPrincipal, EtmGroup etmGroup) {
        if (signal.getNotifiers() == null) {
            return;
        }
        for (String notifierName : signal.getNotifiers()) {
            Notifier notifier = getNotifier(notifierName);
            if (notifier == null) {
                if (log.isWarningLevelEnabled()) {
                    log.logWarningMessage("Notifier with name '"
                            + notifierName
                            + "' not found. Signal '"
                            + signal.getName()
                            + "' of " + (etmPrincipal != null ? "user '" + etmPrincipal.getId() + "'" : " group '" + etmGroup.getName() + "'")
                            + " has an exceeded limit that cannot be notified.");
                }
                continue;
            }
            notificationExecutor.notifyNoLongerExceeded(this.client, this.etmConfiguration, this.clusterName, signal, notifier, etmPrincipal, etmGroup);
        }
    }

    /**
     * Loads an <code>EtmPrincipal</code> based on it's name.
     *
     * @param username The name of the <code>EtmPrincipal</code> to load.
     * @return The <code>EtmPrincipal</code> with the given username, or <code>null</code> when no such user exists.
     */
    private EtmPrincipal getEtmPrincipal(String username) {
        if (username == null) {
            return null;
        }
        GetResponse getResponse = this.client.prepareGet(
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.ETM_DEFAULT_TYPE,
                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + username)
                .get();
        if (!getResponse.isExists()) {
            return null;
        }
        return this.etmPrincipalConverter.readPrincipal(getResponse.getSourceAsMap());
    }

    /**
     * Loads an <code>EtmGroup</code> based on it's name.
     *
     * @param groupName The name of the <code>EtmGroup</code> to load.
     * @return The <code>EtmGroup</code> with the given groupName, or <code>null</code> when no such group exists.
     */
    private EtmGroup getEtmGroup(String groupName) {
        if (groupName == null) {
            return null;
        }
        GetResponse getResponse = this.client.prepareGet(
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.ETM_DEFAULT_TYPE,
                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName)
                .get();
        if (!getResponse.isExists()) {
            return null;
        }
        return this.etmPrincipalConverter.readGroup(getResponse.getSourceAsMap());
    }

    /**
     * Loads a <code>Notifier</code> based on it's name.
     *
     * @param notifierName The name of the <code>Notifier</code> to load.
     * @return The <code>Notifier</code> with the given notifierName, or <code>null</code> when no such notifier exists.
     */
    private Notifier getNotifier(String notifierName) {
        GetResponse getResponse = this.client.prepareGet(
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.ETM_DEFAULT_TYPE,
                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER_ID_PREFIX + notifierName)
                .get();
        if (!getResponse.isExists()) {
            return null;
        }
        return this.notifierConverter.read(getResponse.getSourceAsMap());
    }

    /**
     * Extracts the value of a metric <code>Aggregator</code>.
     *
     * @param aggregation The <code>Aggregator</code> to extract the value from.
     * @return The value stored in the <code>Aggregator</code>.
     * @throws IllegalArgumentException when the give <code>Aggregator</code> is't a metric <code>Aggregator</code>.
     */
    private Double getMetricAggregationValueFromAggregator(Aggregation aggregation) {
        if (aggregation instanceof Percentiles) {
            Percentiles percentiles = (Percentiles) aggregation;
            return percentiles.iterator().next().getValue();
        } else if (aggregation instanceof PercentileRanks) {
            PercentileRanks percentileRanks = (PercentileRanks) aggregation;
            return percentileRanks.iterator().next().getPercent();
        } else if (aggregation instanceof NumericMetricsAggregation.SingleValue) {
            NumericMetricsAggregation.SingleValue singleValue = (NumericMetricsAggregation.SingleValue) aggregation;
            return singleValue.value();
        }
        throw new IllegalArgumentException("'" + aggregation.getClass().getName() + "' is an invalid metric aggregator.");
    }
}
