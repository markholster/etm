package com.jecstar.etm.processor.repository;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.jecstar.etm.core.TelemetryEvent;
import com.jecstar.etm.core.TelemetryMessageEvent;
import com.jecstar.etm.processor.processor.SourceCorrelationCache;

public abstract class AbstractTelemetryEventRepository implements TelemetryEventRepository {

	private final SourceCorrelationCache sourceCorrelations;
	private final Date statisticsTimestamp = new Date();
	private final Date eventOccurrenceTimestamp = new Date();
	private final Date retentionTimestamp = new Date();
	
	public AbstractTelemetryEventRepository(final SourceCorrelationCache sourceCorrelations) {
	    this.sourceCorrelations = sourceCorrelations;
    }
	
	@Override
    public final void persistTelemetryMessageEvent(TelemetryMessageEvent event, TimeUnit statisticsTimeUnit) {
//		this.statisticsTimestamp.setTime(DateUtils.normalizeTime(event.creationTime.getTime(), statisticsTimeUnit.toMillis(1)));
//		this.eventOccurrenceTimestamp.setTime(DateUtils.normalizeTime(event.creationTime.getTime(), PartitionKeySuffixCreator.SMALLEST_TIMUNIT_UNIT.toMillis(1)));
//		this.dataRetention.eventOccurrenceTimestamp.setTime(this.eventOccurrenceTimestamp.getTime());
//		this.dataRetention.retentionTimestamp.setTime(DateUtils.normalizeTime(event.retention.getTime() + (2 * PartitionKeySuffixCreator.SMALLEST_TIMUNIT_UNIT.toMillis(1)), PartitionKeySuffixCreator.SMALLEST_TIMUNIT_UNIT.toMillis(1)));
//		this.dataRetention.id = event.id;
		startPersist(event);
//		
		addTelemetryMessageEvent(event);
		if (!event.correlationData.isEmpty()) {
			event.correlationData.forEach((k,v) ->  addCorrelationData(event, k, v));
		}
//		long requestCount = TelemetryMessageEventType.MESSAGE_REQUEST.equals(event.type) ? 1 : 0;
//		long incomingRequestCount = TelemetryMessageEventType.MESSAGE_REQUEST.equals(event.type) && TelemetryEventDirection.INCOMING.equals(event.direction) ? 1 : 0;
//		long outgoingRequestCount = TelemetryMessageEventType.MESSAGE_REQUEST.equals(event.type) && TelemetryEventDirection.OUTGOING.equals(event.direction) ? 1 : 0;
//		long responseCount = TelemetryMessageEventType.MESSAGE_RESPONSE.equals(event.type) ? 1 : 0;
//		long incomingResponseCount = TelemetryMessageEventType.MESSAGE_RESPONSE.equals(event.type) && TelemetryEventDirection.INCOMING.equals(event.direction) ? 1 : 0;
//		long outgoingResponseCount = TelemetryMessageEventType.MESSAGE_RESPONSE.equals(event.type) && TelemetryEventDirection.OUTGOING.equals(event.direction) ? 1 : 0;
//		long datagramCount = TelemetryMessageEventType.MESSAGE_DATAGRAM.equals(event.type) ? 1 : 0;
//		long incomingDatagramCount = TelemetryMessageEventType.MESSAGE_DATAGRAM.equals(event.type) && TelemetryEventDirection.INCOMING.equals(event.direction) ? 1 : 0;
//		long outgoingDatagramCount = TelemetryMessageEventType.MESSAGE_DATAGRAM.equals(event.type) && TelemetryEventDirection.OUTGOING.equals(event.direction) ? 1 : 0;
//		long responseTime = 0;
//		long incomingResponseTime = 0;
//		long outgoingResponseTime = 0;
//		if (responseCount > 0) {
//			if (event.creationTime.getTime() != 0 && event.correlationCreationTime.getTime() != 0) {
//				responseTime = event.creationTime.getTime() - event.correlationCreationTime.getTime(); 
//			}
//			incomingResponseTime = TelemetryEventDirection.INCOMING.equals(event.direction) ? responseTime : 0;
//			outgoingResponseTime = TelemetryEventDirection.OUTGOING.equals(event.direction) ? responseTime : 0;
//		}
//
//		if (event.application != null) {
//			this.dataRetention.applicationName = event.application;
//			addApplicationCounter(
//					requestCount, 
//					incomingRequestCount, 
//					outgoingRequestCount,
//			        responseCount, 
//			        incomingResponseCount, 
//			        outgoingResponseCount, 
//			        datagramCount, 
//			        incomingDatagramCount,
//			        outgoingDatagramCount, 
//			        responseTime, 
//			        incomingResponseTime, 
//			        outgoingResponseTime, 
//			        event.application,
//			        this.statisticsTimestamp);
//			addEventOccurence(this.eventOccurrenceTimestamp, "Application", event.application);
//		}
//		if (event.name != null) {
//			this.dataRetention.eventName = event.name;
//			addEventNameCounter(
//					requestCount, 
//					responseCount, 
//					datagramCount, 
//					responseTime, 
//					event.name,
//					this.statisticsTimestamp);
//			if (TelemetryMessageEventType.MESSAGE_REQUEST.equals(event.type) || TelemetryMessageEventType.MESSAGE_RESPONSE.equals(event.type)
//			        || TelemetryMessageEventType.MESSAGE_DATAGRAM.equals(event.type)) {
//				addEventOccurence(this.eventOccurrenceTimestamp, "MessageName", event.name);
//			}
//			if (TelemetryMessageEventType.MESSAGE_REQUEST.equals(event.type)) {
//				addMessageEventStart(event);
//			}
//		}
//		if (event.correlationId != null && event.correlationName != null && TelemetryMessageEventType.MESSAGE_RESPONSE.equals(event.type)) {
//			addMessageEventFinish(event);
//		}
//		if (event.application != null && event.name != null) {
//			addApplicationEventNameCounter(
//					requestCount, 
//					incomingRequestCount, 
//					outgoingRequestCount,
//			        responseCount, 
//			        incomingResponseCount, 
//			        outgoingResponseCount, 
//			        datagramCount, 
//			        incomingDatagramCount,
//			        outgoingDatagramCount, 
//			        responseTime, 
//			        incomingResponseTime, 
//			        outgoingResponseTime, 
//					event.application,
//					event.name, 
//					this.statisticsTimestamp);
//		}
//		if (event.transactionName != null) {
//			this.dataRetention.transactionName = event.transactionName;
//			addTransactionNameCounter(
//					requestCount, 
//					responseCount, 
//					responseTime, 
//					event.transactionName, 
//					this.statisticsTimestamp);
//			addEventOccurence(this.eventOccurrenceTimestamp, "TransactionName", event.transactionName);
//			if (TelemetryMessageEventType.MESSAGE_REQUEST.equals(event.type)) {
//				addTransactionEventStart(event);
//			} else if (TelemetryMessageEventType.MESSAGE_RESPONSE.equals(event.type)) {
//				addTransactionEventFinish(event);
//			}
//		}
//		addDataRetention(this.dataRetention);
//		
		endPersist();
		// TODO check this.sourceCorrelations on values that are in the map for longer than x minutes. If so, remove them to prevent garbage in the map.
//		this.sourceCorrelations.removeTelemetryEvent(event);
    }
	
	protected abstract void startPersist(TelemetryEvent event);
	protected abstract void endPersist();
//	
	protected abstract void addTelemetryMessageEvent(TelemetryMessageEvent event);
	protected abstract void addCorrelationData(TelemetryEvent event, String key, String value);
//	protected abstract void addApplicationCounter(long requestCount, long incomingRequestCount, long outgoingRequestCount, long responseCount, long incomingResponseCount, long outgoingResponseCount, long datagramCount, long incomingDatagramCount, long outgoingDatagramCount, long responseTime, long incomingResponseTime, long outgoingResponseTime, String application, Date statisticsTimestamp);
//	protected abstract void addEventOccurence(Date timestamp, String occurrenceName, String occurrenceValue);
//	protected abstract void addEventNameCounter(long requestCount, long responseCount, long datagramCount, long responseTime, String eventName, Date timestamp);
//	protected abstract void addMessageEventStart(TelemetryEvent event);
//	protected abstract void addMessageEventFinish(TelemetryEvent event);
//	protected abstract void addApplicationEventNameCounter(long requestCount, long incomingRequestCount, long outgoingRequestCount, long responseCount, long incomingResponseCount, long outgoingResponseCount, long datagramCount, long incomingDatagramCount, long outgoingDatagramCount, long responseTime, long incomingResponseTime, long outgoingResponseTime, String application, String eventName, Date timestamp);
//	protected abstract void addTransactionNameCounter(long requestCount, long responseCount, long responseTime, String transactionName, Date timestamp);
//	protected abstract void addTransactionEventStart(TelemetryEvent event);
//	protected abstract void addTransactionEventFinish(TelemetryEvent event);
//	protected abstract void addDataRetention(DataRetention dataRetention);
	

}
