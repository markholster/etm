package com.holster.etm.processor.repository;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BatchStatement.Type;
import com.holster.etm.core.TelemetryEventDirection;
import com.holster.etm.core.TelemetryEventType;
import com.holster.etm.core.cassandra.PartitionKeySuffixCreator;
import com.holster.etm.processor.TelemetryEvent;

public class TelemetryEventRepositoryCassandraImpl implements TelemetryEventRepository {

	
	private final StatementExecutor statementExecutor;
	private final BatchStatement batchStatement = new BatchStatement(Type.UNLOGGED);
	private final BatchStatement counterBatchStatement = new BatchStatement(Type.COUNTER);
	private final Map<String, CorrelationBySourceIdResult> sourceCorrelations;
	
	private final Date statisticsTimestamp = new Date();
	private final Date eventOccurrenceTimestamp = new Date();
	private final DateFormat format = new PartitionKeySuffixCreator();
	private final Map<String, EndpointConfigResult> endpointConfigs = new HashMap<String, EndpointConfigResult>();
	
	public TelemetryEventRepositoryCassandraImpl(final StatementExecutor statementExecutor, final Map<String, CorrelationBySourceIdResult> sourceCorrelations) {
		this.statementExecutor = statementExecutor;
		this.sourceCorrelations = sourceCorrelations;
    }
	
	// TODO add all statements to a batchStatement and execute synchronous.
	@Override
    public void persistTelemetryEvent(final TelemetryEvent event, final TimeUnit smallestStatisticsTimeUnit) {
		this.statisticsTimestamp.setTime(normalizeTime(event.creationTime.getTime(), smallestStatisticsTimeUnit.toMillis(1)));
		this.eventOccurrenceTimestamp.setTime(normalizeTime(event.creationTime.getTime(), PartitionKeySuffixCreator.SMALLEST_TIMUNIT_UNIT.toMillis(1)));
		// The following 2 suffixes are defining the diversity of the partition
		// key in cassandra. If a partition is to big for a single key, the
		// dateformat should be displayed in a less general format.
		final String partitionKeySuffix = this.format.format(event.creationTime);
		final String correlationPartitionKeySuffix = this.format.format(event.correlationCreationTime);
		
		this.statementExecutor.addTelemetryEvent(event, this.batchStatement);
		if (event.sourceId != null) {
			// Synchronous execution because soureCorrelation list needs to be cleared with this event after the data is present in the database.
			this.statementExecutor.addSourceIdCorrelationData(event, this.batchStatement);
		}
		if (!event.correlationData.isEmpty()) {
			event.correlationData.forEach((k,v) ->  this.statementExecutor.addCorrelationData(event, k + partitionKeySuffix, k, v, this.batchStatement));
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
			this.statementExecutor.addApplicationCounter(
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
			        this.statisticsTimestamp,
			        event.application + partitionKeySuffix,
			        this.counterBatchStatement);
			this.statementExecutor.addEventOccurence(this.eventOccurrenceTimestamp, "Application", event.application + partitionKeySuffix, event.application, this.batchStatement);
		}
		if (event.name != null) {
			this.statementExecutor.addEventNameCounter(
					requestCount, 
					responseCount, 
					datagramCount, 
					responseTime, 
					event.name,
					this.statisticsTimestamp,
					event.name + partitionKeySuffix, 
					this.counterBatchStatement);
			if (TelemetryEventType.MESSAGE_REQUEST.equals(event.type) || TelemetryEventType.MESSAGE_RESPONSE.equals(event.type)
			        || TelemetryEventType.MESSAGE_DATAGRAM.equals(event.type)) {
				this.statementExecutor.addEventOccurence(this.eventOccurrenceTimestamp, "MessageName", event.name  + partitionKeySuffix, event.name, this.batchStatement);
			}
			if (TelemetryEventType.MESSAGE_REQUEST.equals(event.type)) {
				this.statementExecutor.addMessageEventStart(event, event.name + partitionKeySuffix, this.batchStatement);
			}
		}
		if (event.correlationId != null && event.correlationName != null && TelemetryEventType.MESSAGE_RESPONSE.equals(event.type)) {
			this.statementExecutor.addMessageEventFinish(event, event.correlationName + correlationPartitionKeySuffix, this.batchStatement);
		}
		if (event.application != null && event.name != null) {
			this.statementExecutor.addApplicationEventNameCounter(
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
					this.statisticsTimestamp,
					event.application + partitionKeySuffix, 
					this.counterBatchStatement);
			this.statementExecutor.addEventOccurence(this.eventOccurrenceTimestamp, "Application", event.application + partitionKeySuffix, event.application, this.batchStatement);
		}
		if (event.transactionName != null) {
			this.statementExecutor.addTransactionNameCounter(
					requestCount, 
					responseCount, 
					responseTime, 
					event.transactionName, 
					this.statisticsTimestamp,
					event.transactionName + partitionKeySuffix, this.counterBatchStatement);
			this.statementExecutor.addEventOccurence(this.eventOccurrenceTimestamp, "TransactionName", event.transactionName + partitionKeySuffix, event.transactionName, this.batchStatement);
			if (TelemetryEventType.MESSAGE_REQUEST.equals(event.type)) {
				this.statementExecutor.addTransactionEventStart(event, event.transactionName + partitionKeySuffix, this.batchStatement);
			} else if (TelemetryEventType.MESSAGE_RESPONSE.equals(event.type)) {
				this.statementExecutor.addTransactionEventFinish(event, event.transactionName + correlationPartitionKeySuffix, this.batchStatement);
			}
		}
		if (this.batchStatement.size() != 0) {
			this.statementExecutor.execute(this.batchStatement);
			this.batchStatement.clear();
			// TODO check this.sourceCorrelations on values that are in the map for longer than x minutes. If so, remove them to prevent garbage in the map.
			this.sourceCorrelations.remove(event.sourceId);
		}
		if (this.counterBatchStatement.size() != 0) {
			this.statementExecutor.execute(this.counterBatchStatement);
			this.counterBatchStatement.clear();
		}
    }

	private long normalizeTime(long timeInMillis, long factor) {
		return (timeInMillis / factor) * factor;
    }

	@Override
    public void findParent(String sourceId, CorrelationBySourceIdResult result) {
		if (sourceId == null) {
			return;
		}
		CorrelationBySourceIdResult parent = this.sourceCorrelations.get(sourceId);
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
		this.statementExecutor.findParent(sourceId, result);
    }

	@Override
    public void findEndpointConfig(String endpoint, EndpointConfigResult result, long cacheExpiryTime) {
		EndpointConfigResult cachedResult = this.endpointConfigs.get(endpoint);
		if (cachedResult == null || System.currentTimeMillis() - cachedResult.retrieved > cacheExpiryTime) {
			if (cachedResult == null) {
				cachedResult = new EndpointConfigResult();
			}
			cachedResult.initialize();
			// First check the global configuration
			this.statementExecutor.findAndMergeEndpointConfig("*", cachedResult);
			this.statementExecutor.findAndMergeEndpointConfig(endpoint, cachedResult);
			cachedResult.retrieved = System.currentTimeMillis();
			this.endpointConfigs.put(endpoint, cachedResult);
		} 
		result.applicationParsers.addAll(cachedResult.applicationParsers);
		result.eventNameParsers.addAll(cachedResult.eventNameParsers);
		result.correlationDataParsers.putAll(cachedResult.correlationDataParsers);
		result.eventDirection = cachedResult.eventDirection;
		result.transactionNameParsers.addAll(cachedResult.transactionNameParsers);
		result.slaRules.putAll(cachedResult.slaRules);
    }
	

}
