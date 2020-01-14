package com.jecstar.etm.signaler;

import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import com.jecstar.etm.server.core.domain.aggregator.metric.MetricValue;
import com.jecstar.etm.server.core.domain.cluster.notifier.Notifier;
import com.jecstar.etm.server.core.domain.cluster.notifier.converter.NotifierConverter;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.configuration.EtmSnmpConstants;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.EtmSecurityEntity;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.UpdateRequestBuilder;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.persisting.RequestEnhancer;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import com.jecstar.etm.server.core.persisting.internal.BusinessEventLogger;
import com.jecstar.etm.server.core.rest.AbstractJsonService;
import com.jecstar.etm.signaler.backoff.BackoffPolicy;
import com.jecstar.etm.signaler.domain.Signal;
import com.jecstar.etm.signaler.domain.Threshold;
import com.jecstar.etm.signaler.domain.converter.SignalConverter;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.HasAggregations;
import org.elasticsearch.search.aggregations.ParsedMultiBucketAggregation;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.ParsedSingleBucketAggregation;
import org.snmp4j.smi.OctetString;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Starting class for the alerting functionality.
 */
public class Signaler extends AbstractJsonService implements Runnable {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(Signaler.class);
    private static final long SYSTEM_START_TIME = System.currentTimeMillis();
    private static final int MAX_RETRIES = 5;

    private final String clusterName;
    private final DataRepository dataRepository;
    private final EtmConfiguration etmConfiguration;
    private final EtmPrincipalConverterJsonImpl etmPrincipalConverter = new EtmPrincipalConverterJsonImpl();
    private final EtmPrincipalTags etmPrincipalTags = etmPrincipalConverter.getTags();
    private final SignalConverter signalConverter = new SignalConverter();
    private final NotifierConverter notifierConverter = new NotifierConverter();
    private final RequestEnhancer requestEnhancer;
    private final byte[] snmpEngineId;

    public Signaler(String clusterName, EtmConfiguration etmConfiguration, DataRepository dataRepository) {
        this.clusterName = clusterName;
        this.etmConfiguration = etmConfiguration;
        this.requestEnhancer = new RequestEnhancer(etmConfiguration);
        this.dataRepository = dataRepository;
        OctetString engineId = createSnmpEngineId();
        this.snmpEngineId = engineId.getValue();
        BusinessEventLogger.logSnmpEngineIdAssignment(engineId.toHexString());

    }

    /**
     * Create a new engine id based on the Jecstar PEN and the host we're running on.
     *
     * @return The engine id that is unique for the machine ETM is running on.
     */
    private OctetString createSnmpEngineId() {
        byte[] engineID = new byte[5];
        engineID[0] = (byte) (0x80 | ((EtmSnmpConstants.JECSTAR_PEN >> 24) & 0xFF));
        engineID[1] = (byte) ((EtmSnmpConstants.JECSTAR_PEN >> 16) & 0xFF);
        engineID[2] = (byte) ((EtmSnmpConstants.JECSTAR_PEN >> 8) & 0xFF);
        engineID[3] = (byte) (EtmSnmpConstants.JECSTAR_PEN & 0xFF);
        engineID[4] = 2;
        OctetString os = new OctetString();
        try {
            byte[] b = InetAddress.getLocalHost().getAddress();
            if (b.length == 4) {
                engineID[4] = 1;
            }
            os.setValue(b);
        } catch (UnknownHostException e) {
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("Local host cannot be determined for creation of local engine ID", e);
            }
            engineID[4] = 4;
            os.setValue("ETM".getBytes());
        }
        OctetString ownEngineID = new OctetString(engineID);
        ownEngineID.append(os);
        return ownEngineID;
    }

    @Override
    public void run() {
        Instant batchStart = Instant.now();
        try (NotificationExecutor thresholdExceededNotifier = new NotificationExecutor(this.snmpEngineId)) {
            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
            boolQueryBuilder.should(new ExistsQueryBuilder(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP + "." + this.etmPrincipalTags.getSignalsTag()));
            boolQueryBuilder.should(new ExistsQueryBuilder(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.etmPrincipalTags.getSignalsTag()));
            boolQueryBuilder.minimumShouldMatch(1);

            ScrollableSearch scrollableSearch = new ScrollableSearch(
                    dataRepository,
                    this.requestEnhancer.enhance(
                            new SearchRequestBuilder()
                    ).setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                            .setFetchSource(false)
                            .setQuery(boolQueryBuilder)
            );
            for (SearchHit searchHit : scrollableSearch) {
                for (int i = 0; i < MAX_RETRIES; i++) {
                    if (Thread.currentThread().isInterrupted()) {
                        if (log.isInfoLevelEnabled()) {
                            log.logInfoMessage("Thread interrupted. Stopped processing of Signals.");
                        }
                        return;
                    }
                    try {
                        if (handleEntity(batchStart, thresholdExceededNotifier, searchHit.getIndex(), searchHit.getId())) {
                            break;
                        }
                    } catch (Exception e) {
                        if (log.isErrorLevelEnabled()) {
                            log.logErrorMessage("Failed to handle signal with id '" + searchHit.getId() + "'", e);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Failed to handle signals", e);
            }
        }
    }

    /**
     * Method handles an entity. The entity will be retrieved, tested for threshold exceedances and updated if necessary.
     *
     * @param batchStart           The moment the signal check batch started.
     * @param notificationExecutor The <code>NotificationExecutor</code> to be used to notify users.
     * @param index                The index of the entity.
     * @param id                   The id of the entity.
     * @return <code>true</code> when the entity is handled, otherwise <code>false</code> will be returned.
     */
    private boolean handleEntity(Instant batchStart, NotificationExecutor notificationExecutor, String index, String id) {
        GetResponse getResponse = this.dataRepository.get(new GetRequestBuilder(index, id)
                .setFetchSource(
                        new String[]{
                                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP + "." + this.etmPrincipalTags.getSignalsTag(),
                                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP + "." + this.etmPrincipalTags.getNameTag(),
                                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.etmPrincipalTags.getSignalsTag(),
                                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.etmPrincipalTags.getIdTag()
                        }
                        , null));
        Map<String, Object> sourceMap = getResponse.getSourceAsMap();
        Map<String, Object> entityObjectMap;
        EtmSecurityEntity etmSecurityEntity;
        if (sourceMap.containsKey(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP)) {
            entityObjectMap = toMapWithoutNamespace(sourceMap, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP);
            String groupName = getString(this.etmPrincipalTags.getNameTag(), entityObjectMap);
            etmSecurityEntity = getEtmGroup(groupName);
        } else {
            entityObjectMap = toMapWithoutNamespace(sourceMap, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER);
            String userId = getString(this.etmPrincipalTags.getIdTag(), entityObjectMap);
            etmSecurityEntity = getEtmPrincipal(userId);
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
            Instant lastExecuted = getInstant(Signal.LAST_EXECUTED, signalValues);
            Signal signal = this.signalConverter.read(signalValues);
            if (!signal.isEnabled()) {
                continue;
            }
            if (lastExecuted != null && Instant.now().isBefore(lastExecuted.plus(signal.getNotifications().getIntervalUnit().toDuration(signal.getNotifications().getInterval())))) {
                continue;
            }
            SignalTestResult result = testThresholds(signal, etmSecurityEntity);
            if (!result.isExecuted()) {
                break;
            }
            signalsUpdated = true;
            signalValues.put(Signal.LAST_EXECUTED, batchStart.toEpochMilli());
            if (result.isLimitExceeded(signal)) {
                signalValues.put(Signal.LAST_FAILED, batchStart.toEpochMilli());
                if (signalValues.containsKey(Signal.FAILED_SINCE)) {
                    long failedSince = getLong(Signal.FAILED_SINCE, signalValues);
                    result.setConsecutiveFailures(
                            Math.round(
                                    (float) (batchStart.toEpochMilli() - failedSince)
                                            / (float) result.getTestInterval().toMillis()
                            )
                                    + 1
                    );
                } else {
                    signalValues.put(Signal.FAILED_SINCE, batchStart.toEpochMilli());
                    result.setConsecutiveFailures(1);
                }
                notificationMap.put(signal, result);
            } else {
                if (signalValues.containsKey(Signal.FAILED_SINCE)) {
                    fixedSet.add(signal);
                }
                signalValues.remove(Signal.FAILED_SINCE);
                signalValues.put(Signal.LAST_PASSED, batchStart.toEpochMilli());
            }
        }
        if (!signalsUpdated) {
            return true;
        }
        try {
            UpdateRequestBuilder updateRequestBuilder = this.requestEnhancer.enhance(
                    new UpdateRequestBuilder().setIndex(index).setId(id)
            )
                    .setIfSeqNo(getResponse.getSeqNo())
                    .setIfPrimaryTerm(getResponse.getPrimaryTerm())
                    .setDoc(sourceMap)
                    // No retry because we specify the version
                    .setRetryOnConflict(0);
            this.dataRepository.update(updateRequestBuilder);
        } catch (ElasticsearchStatusException e) {
            // Another process has updated the entity. This could be another ETM instance that was running this signal
            // at exactly the same time. The only way to detect this is to retry this entire method. If another ETM instance
            // has fully executed this method the LAST_EXECUTED time is updated on all signals in this entity so it won't
            // be executed again.
            return !RestStatus.CONFLICT.equals(e.status());
        }
        // Current version updated, so no other node in the cluster has came this far. We have to send the notifications now.
        for (Map.Entry<Signal, SignalTestResult> entry : notificationMap.entrySet()) {
            sendFailureNotifications(notificationExecutor, entry.getKey(), entry.getValue(), etmSecurityEntity);
        }
        for (Signal signal : fixedSet) {
            sendFixedNotifications(notificationExecutor, signal, etmSecurityEntity);
        }
        return true;
    }

    /**
     * Test a signal if the threshold is exceeded more often that the limit in the signal. Also check if the
     * <code>EtmPrincipal</code> or <code>EtmGroup</code> is still authorized for the data source configured within the
     * <code>Signal</code>
     *
     * @param signal       The <code>Signal</code> to test.
     * @param etmSecurityEntity The <code>EtmSecurityEntity</code> that has stored the given <code>Signal</code>.
     * @return A <code>SignalTestResult</code> instance with the test and notifyExceedance results.
     */
    private SignalTestResult testThresholds(Signal signal, EtmSecurityEntity etmSecurityEntity) {
        SignalTestResult result = new SignalTestResult();
        result.setTestInterval(signal.getNotifications().getIntervalUnit().toDuration(signal.getNotifications().getInterval()));
        SignalSearchRequestBuilderBuilder builderBuilder = new SignalSearchRequestBuilderBuilder(this.dataRepository, this.etmConfiguration)
                .setSignal(signal);
        SearchRequestBuilder builder;

        if (!etmSecurityEntity.isAuthorizedForSignalDatasource(signal.getData().getDataSource())) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Signal '" + signal.getName() + "' is configured with data source '" + signal.getData().getDataSource() + "' but " + etmSecurityEntity.getType() + " '" + etmSecurityEntity.getDisplayName() + "' is not authorized for that datasource.");
            }
            return result.setExectued(false);
        }
        if (etmSecurityEntity instanceof EtmPrincipal) {
            EtmPrincipal etmPrincipal = (EtmPrincipal) etmSecurityEntity;
            builder = builderBuilder.build(q -> addFilterQuery(etmPrincipal, q), etmPrincipal);
        } else {
            EtmGroup etmGroup = (EtmGroup) etmSecurityEntity;
            builder = builderBuilder.build(q -> addFilterQuery(etmGroup, q, null), null);
        }

        SearchResponse searchResponse = this.dataRepository.search(builder);
        result.setExectued(true);
        MultiBucketsAggregation multiBucketsAggregation = searchResponse.getAggregations().get(SignalSearchRequestBuilderBuilder.CARDINALITY_AGGREGATION_KEY);
        for (MultiBucketsAggregation.Bucket bucket : multiBucketsAggregation.getBuckets()) {
            processAggregations(bucket, bucket, signal.getThreshold(), result);
        }
        return result;
    }

    private void processAggregations(MultiBucketsAggregation.Bucket root, HasAggregations aggregationHolder, Threshold threshold, SignalTestResult signalTestResult) {
        for (Aggregation aggregation : aggregationHolder.getAggregations()) {
            boolean showOnGraph = (boolean) aggregation.getMetaData().get(Aggregator.SHOW_ON_GRAPH);
            if (!showOnGraph) {
                continue;
            }
            if (aggregation instanceof ParsedMultiBucketAggregation) {
                ParsedMultiBucketAggregation multiBucketsAggregation = (ParsedMultiBucketAggregation) aggregation;
                for (MultiBucketsAggregation.Bucket subBucket : multiBucketsAggregation.getBuckets()) {
                    processAggregations(root, subBucket, threshold, signalTestResult);
                }
            } else if (aggregation instanceof ParsedSingleBucketAggregation) {
                ParsedSingleBucketAggregation singleBucketAggregation = (ParsedSingleBucketAggregation) aggregation;
                processAggregations(root, singleBucketAggregation, threshold, signalTestResult);
            } else {
                final MetricValue metricValue = new MetricValue(aggregation);
                if (metricValue.hasValidValue() && threshold.getComparison().isExceeded(threshold.getValue(), metricValue.getValue())) {
                    signalTestResult.addThresholdExceedance((ZonedDateTime) root.getKey(), metricValue.getValue());
                }
            }
        }
    }

    /**
     * Send the failure notifications to the user(s).
     *
     * @param notificationExecutor The <code>NotificationExecutor</code> to be used to notify users.
     * @param signal               The <code>Signal</code> that has a threshold exceedance.
     * @param signalTestResult     The <code>SignalTestResult</code> instance.
     * @param etmSecurityEntity    The <code>EtmSecurityEntity</code> that owns the <code>Signal</code>.
     */
    private void sendFailureNotifications(NotificationExecutor notificationExecutor, Signal signal, SignalTestResult signalTestResult, EtmSecurityEntity etmSecurityEntity) {
        if (signal.getNotifications().getNotifiers() == null) {
            return;
        }
        for (String notifierName : signal.getNotifications().getNotifiers()) {
            if (!etmSecurityEntity.isAuthorizedForNotifier(notifierName)) {
                if (log.isWarningLevelEnabled()) {
                    log.logWarningMessage("Signal '"
                            + signal.getName()
                            + "' of " + etmSecurityEntity.getType() + " '" + etmSecurityEntity.getDisplayName()
                            + "' has a notifier '" + notifierName + "' configured but that " + etmSecurityEntity.getType() + " is not authorized for this notifier.");
                }
                continue;
            }
            Notifier notifier = getNotifier(notifierName);
            if (notifier == null) {
                if (log.isWarningLevelEnabled()) {
                    log.logWarningMessage("Notifier with name '"
                            + notifierName
                            + "' not found. Signal '"
                            + signal.getName()
                            + "' of " + etmSecurityEntity.getType() + " '" + etmSecurityEntity.getDisplayName()
                            + "' has an exceeded limit that cannot be notified.");
                }
                continue;
            }
            BackoffPolicy backoffPolicy = signalTestResult.getNotificationBackoffPolicy(notifier);
            if (backoffPolicy.shouldBeNotified()) {
                notificationExecutor.notifyExceedance(this.dataRepository, this.etmConfiguration, this.clusterName, signal, notifier, signalTestResult.getThresholdExceedances(), etmSecurityEntity, SYSTEM_START_TIME);
            }
        }
    }

    /**
     * Send the fixed notifications to the user(s).
     *
     * @param notificationExecutor The <code>NotificationExecutor</code> to be used to notify users.
     * @param signal               The <code>Signal</code> that no longer has a threshold exceedance.
     * @param etmSecurityEntity    The <code>EtmSecurityEntity</code> that owns the <code>Signal</code>.
     */
    private void sendFixedNotifications(NotificationExecutor notificationExecutor, Signal signal, EtmSecurityEntity etmSecurityEntity) {
        if (signal.getNotifications().getNotifiers() == null) {
            return;
        }
        for (String notifierName : signal.getNotifications().getNotifiers()) {
            if (!etmSecurityEntity.isAuthorizedForNotifier(notifierName)) {
                if (log.isWarningLevelEnabled()) {
                    log.logWarningMessage("Signal '"
                            + signal.getName()
                            + "' of " + etmSecurityEntity.getType() + " '" + etmSecurityEntity.getDisplayName()
                            + "' has a notifier '" + notifierName + "' configured but that " + etmSecurityEntity.getType() + " is not authorized for this notifier.");
                }
                continue;
            }
            Notifier notifier = getNotifier(notifierName);
            if (notifier == null) {
                if (log.isWarningLevelEnabled()) {
                    log.logWarningMessage("Notifier with name '"
                            + notifierName
                            + "' not found. Signal '"
                            + signal.getName()
                            + "' of " + etmSecurityEntity.getType() + " '" + etmSecurityEntity.getDisplayName()
                            + "' has an exceeded limit that cannot be notified.");
                }
                continue;
            }
            notificationExecutor.notifyNoLongerExceeded(this.dataRepository, this.etmConfiguration, this.clusterName, signal, notifier, etmSecurityEntity);
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
        GetRequestBuilder requestBuilder = new GetRequestBuilder(
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + username);
        GetResponse getResponse = this.dataRepository.get(requestBuilder);
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
        GetRequestBuilder requestBuilder = new GetRequestBuilder(
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName
        );
        GetResponse getResponse = this.dataRepository.get(requestBuilder);
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
        GetRequestBuilder requestBuilder = new GetRequestBuilder(
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER_ID_PREFIX + notifierName
        );
        GetResponse getResponse = this.dataRepository.get(requestBuilder);
        if (!getResponse.isExists()) {
            return null;
        }
        return this.notifierConverter.read(getResponse.getSourceAsMap());
    }
}
