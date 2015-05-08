package com.jecstar.etm.processor.repository;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.jecstar.etm.core.TelemetryEventDirection;
import com.jecstar.etm.core.TelemetryEventType;
import com.jecstar.etm.core.cassandra.PartitionKeySuffixCreator;
import com.jecstar.etm.core.util.DateUtils;
import com.jecstar.etm.processor.TelemetryEvent;

public abstract class AbstractTelemetryEventRepository implements TelemetryEventRepository {

	private final Map<String, CorrelationBySourceIdResult> sourceCorrelations;
	private final Date statisticsTimestamp = new Date();
	private final Date eventOccurrenceTimestamp = new Date();
	private final DataRetention dataRetention = new DataRetention();
	private final String nodeName;
	private final Date performanceTimestamp = new Date();
	private final Date performanceKeyTimestamp = new Date();
	
	public AbstractTelemetryEventRepository(final Map<String, CorrelationBySourceIdResult> sourceCorrelations, String nodeName) {
	    this.sourceCorrelations = sourceCorrelations;
	    this.nodeName = nodeName;
    }
	
	@Override
    public final void persistTelemetryEvent(TelemetryEvent event, TimeUnit statisticsTimeUnit) {
		this.dataRetention.clear();
		this.statisticsTimestamp.setTime(DateUtils.normalizeTime(event.creationTime.getTime(), statisticsTimeUnit.toMillis(1)));
		this.eventOccurrenceTimestamp.setTime(DateUtils.normalizeTime(event.creationTime.getTime(), PartitionKeySuffixCreator.SMALLEST_TIMUNIT_UNIT.toMillis(1)));
		this.dataRetention.eventOccurrenceTimestamp.setTime(this.eventOccurrenceTimestamp.getTime());
		this.dataRetention.retentionTimestamp .setTime(DateUtils.normalizeTime(event.retention.getTime() + (2 * PartitionKeySuffixCreator.SMALLEST_TIMUNIT_UNIT.toMillis(1)), PartitionKeySuffixCreator.SMALLEST_TIMUNIT_UNIT.toMillis(1)));
		this.dataRetention.id = event.id;
		startPersist(event, this.dataRetention);
		
		addTelemetryEvent(event);
		if (event.sourceId != null) {
			addSourceIdCorrelationData(event);
			this.dataRetention.sourceId = event.sourceId;
		}
		if (!event.correlationData.isEmpty()) {
			this.dataRetention.correlationData.putAll(event.correlationData);
			event.correlationData.forEach((k,v) ->  addCorrelationData(event, k, v));
		}
		long requestCount = TelemetryEventType.MESSAGE_REQUEST.equals(event.type) ? 1 : 0;
		long incomingRequestCount = TelemetryEventType.MESSAGE_REQUEST.equals(event.type) && TelemetryEventDirection.INCOMING.equals(event.direction) ? 1 : 0;
		long outgoingRequestCount = TelemetryEventType.MESSAGE_REQUEST.equals(event.type) && TelemetryEventDirection.OUTGOING.equals(event.direction) ? 1 : 0;
		long responseCount = TelemetryEventType.MESSAGE_RESPONSE.equals(event.type) ? 1 : 0;
		long incomingResponseCount = TelemetryEventType.MESSAGE_RESPONSE.equals(event.type) && TelemetryEventDirection.INCOMING.equals(event.direction) ? 1 : 0;
		long outgoingResponseCount = TelemetryEventType.MESSAGE_RESPONSE.equals(event.type) && TelemetryEventDirection.OUTGOING.equals(event.direction) ? 1 : 0;
		long datagramCount = TelemetryEventType.MESSAGE_DATAGRAM.equals(event.type) ? 1 : 0;
		long incomingDatagramCount = TelemetryEventType.MESSAGE_DATAGRAM.equals(event.type) && TelemetryEventDirection.INCOMING.equals(event.direction) ? 1 : 0;
		long outgoingDatagramCount = TelemetryEventType.MESSAGE_DATAGRAM.equals(event.type) && TelemetryEventDirection.OUTGOING.equals(event.direction) ? 1 : 0;
		long responseTime = 0;
		long incomingResponseTime = 0;
		long outgoingResponseTime = 0;
		if (responseCount > 0) {
			if (event.creationTime.getTime() != 0 && event.correlationCreationTime.getTime() != 0) {
				responseTime = event.creationTime.getTime() - event.correlationCreationTime.getTime(); 
			}
			incomingResponseTime = TelemetryEventDirection.INCOMING.equals(event.direction) ? responseTime : 0;
			outgoingResponseTime = TelemetryEventDirection.OUTGOING.equals(event.direction) ? responseTime : 0;
		}

		if (event.application != null) {
			this.dataRetention.applicationName = event.application;
			addApplicationCounter(
					requestCount, 
					incomingRequestCount, 
					outgoingRequestCount,
			        responseCount, 
			        incomingResponseCount, 
			        outgoingResponseCount, 
			        datagramCount, 
			        incomingDatagramCount,
			        outgoingDatagramCount, 
			        responseTime, 
			        incomingResponseTime, 
			        outgoingResponseTime, 
			        event.application,
			        this.statisticsTimestamp);
			addEventOccurence(this.eventOccurrenceTimestamp, "Application", event.application);
		}
		if (event.name != null) {
			this.dataRetention.eventName = event.name;
			addEventNameCounter(
					requestCount, 
					responseCount, 
					datagramCount, 
					responseTime, 
					event.name,
					this.statisticsTimestamp);
			if (TelemetryEventType.MESSAGE_REQUEST.equals(event.type) || TelemetryEventType.MESSAGE_RESPONSE.equals(event.type)
			        || TelemetryEventType.MESSAGE_DATAGRAM.equals(event.type)) {
				addEventOccurence(this.eventOccurrenceTimestamp, "MessageName", event.name);
			}
			if (TelemetryEventType.MESSAGE_REQUEST.equals(event.type)) {
				addMessageEventStart(event);
			}
		}
		if (event.correlationId != null && event.correlationName != null && TelemetryEventType.MESSAGE_RESPONSE.equals(event.type)) {
			addMessageEventFinish(event);
		}
		if (event.application != null && event.name != null) {
			addApplicationEventNameCounter(
					requestCount, 
					incomingRequestCount, 
					outgoingRequestCount,
			        responseCount, 
			        incomingResponseCount, 
			        outgoingResponseCount, 
			        datagramCount, 
			        incomingDatagramCount,
			        outgoingDatagramCount, 
			        responseTime, 
			        incomingResponseTime, 
			        outgoingResponseTime, 
					event.application,
					event.name, 
					this.statisticsTimestamp);
		}
		if (event.transactionName != null) {
			this.dataRetention.transactionName = event.transactionName;
			addTransactionNameCounter(
					requestCount, 
					responseCount, 
					responseTime, 
					event.transactionName, 
					this.statisticsTimestamp);
			addEventOccurence(this.eventOccurrenceTimestamp, "TransactionName", event.transactionName);
			if (TelemetryEventType.MESSAGE_REQUEST.equals(event.type)) {
				addTransactionEventStart(event);
			} else if (TelemetryEventType.MESSAGE_RESPONSE.equals(event.type)) {
				addTransactionEventFinish(event);
			}
		}
		addDataRetention(this.dataRetention);
		
		endPersist();
		// TODO check this.sourceCorrelations on values that are in the map for longer than x minutes. If so, remove them to prevent garbage in the map.
		this.sourceCorrelations.remove(event.sourceId + "_" + event.application);
    }
	
	@Override
    public void persistPerformance(TelemetryEvent telemetryEvent, TimeUnit statisticsTimeUnit) {
		final long currentTimeMillis = System.currentTimeMillis();
		this.performanceKeyTimestamp.setTime(DateUtils.normalizeTime(currentTimeMillis, PartitionKeySuffixCreator.SMALLEST_TIMUNIT_UNIT.toMillis(1)));
		this.performanceTimestamp.setTime(DateUtils.normalizeTime(currentTimeMillis, statisticsTimeUnit.toMillis(1)));
		persistPerformance(this.performanceKeyTimestamp, this.nodeName, this.performanceTimestamp, 
				telemetryEvent.offerTime == -1 ? 0 : 1, 
				telemetryEvent.offerTime == -1 ? 0 : telemetryEvent.offerTime, 
				telemetryEvent.enhancingTime == -1 ? 0 : 1, 
				telemetryEvent.enhancingTime == -1 ? 0 : telemetryEvent.enhancingTime, 
				telemetryEvent.indexingTime == -1 ? 0 : 1, 
				telemetryEvent.indexingTime == -1 ? 0 : telemetryEvent.indexingTime,
				telemetryEvent.persistingTime == -1 ? 0 : 1, 
				telemetryEvent.persistingTime == -1 ? 0 : telemetryEvent.persistingTime);
    }
	
	@Override
    public final void findParent(String sourceId, String application, CorrelationBySourceIdResult result) {
		if (sourceId == null) {
			return;
		}
		CorrelationBySourceIdResult parent = this.sourceCorrelations.get(sourceId + "_" + application);
		if (parent != null) {
			result.id = parent.id;
			result.transactionId = parent.transactionId;
			result.transactionName = parent.transactionName;
			result.creationTime = parent.creationTime;
			result.expiryTime = parent.expiryTime;
			result.name = parent.name;
			result.slaRule = parent.slaRule;
			return;
		}
		doFindParent(sourceId, application, result);
		if (result.id == null && application != null) {
			findParent(sourceId, null, result);
		}
    }
	
	protected abstract void startPersist(TelemetryEvent event, DataRetention dataRetention);
	protected abstract void endPersist();
	
	protected abstract void addTelemetryEvent(TelemetryEvent event);
	protected abstract void addSourceIdCorrelationData(TelemetryEvent event);
	protected abstract void addCorrelationData(TelemetryEvent event, String key, String value);
	protected abstract void addApplicationCounter(long requestCount, long incomingRequestCount, long outgoingRequestCount, long responseCount, long incomingResponseCount, long outgoingResponseCount, long datagramCount, long incomingDatagramCount, long outgoingDatagramCount, long responseTime, long incomingResponseTime, long outgoingResponseTime, String application, Date statisticsTimestamp);
	protected abstract void addEventOccurence(Date timestamp, String occurrenceName, String occurrenceValue);
	protected abstract void addEventNameCounter(long requestCount, long responseCount, long datagramCount, long responseTime, String eventName, Date timestamp);
	protected abstract void addMessageEventStart(TelemetryEvent event);
	protected abstract void addMessageEventFinish(TelemetryEvent event);
	protected abstract void addApplicationEventNameCounter(long requestCount, long incomingRequestCount, long outgoingRequestCount, long responseCount, long incomingResponseCount, long outgoingResponseCount, long datagramCount, long incomingDatagramCount, long outgoingDatagramCount, long responseTime, long incomingResponseTime, long outgoingResponseTime, String application, String eventName, Date timestamp);
	protected abstract void addTransactionNameCounter(long requestCount, long responseCount, long responseTime, String transactionName, Date timestamp);
	protected abstract void addTransactionEventStart(TelemetryEvent event);
	protected abstract void addTransactionEventFinish(TelemetryEvent event);
	protected abstract void addDataRetention(DataRetention dataRetention);
	
	protected abstract void doFindParent(String sourceId, String application, CorrelationBySourceIdResult result);
	
	protected abstract void persistPerformance(Date keyTime, String nodeName, Date performanceTime, int offerCount, long offerTime, int enhancingCount, long enhancingTime, int indexingCount, long indexingTime, int persistingCount, long persistingTime);
	
	

}
