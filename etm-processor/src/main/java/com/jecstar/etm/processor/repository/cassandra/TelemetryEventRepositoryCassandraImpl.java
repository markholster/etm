package com.jecstar.etm.processor.repository.cassandra;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BatchStatement.Type;
import com.jecstar.etm.core.cassandra.PartitionKeySuffixCreator;
import com.jecstar.etm.processor.TelemetryEvent;
import com.jecstar.etm.processor.repository.AbstractTelemetryEventRepository;
import com.jecstar.etm.processor.repository.CorrelationBySourceIdResult;
import com.jecstar.etm.processor.repository.DataRetention;
import com.jecstar.etm.processor.repository.EndpointConfigResult;

/**
 * Implementation of the <code>TelemetryEventRepository</code> that is backed by
 * a Cassandra cluster. This class is NOT thread safe; each
 * <code>TelementryEventProcessor</code> handler should have it's own instance.
 * 
 * @author Mark Holster
 */
public class TelemetryEventRepositoryCassandraImpl extends AbstractTelemetryEventRepository {
	
	private final CassandraStatementExecutor cassandraStatementExecutor;
	private final BatchStatement batchStatement = new BatchStatement(Type.UNLOGGED);
	private final BatchStatement counterBatchStatement = new BatchStatement(Type.COUNTER);
	
	private final DateFormat format = new PartitionKeySuffixCreator();
	private final Map<String, EndpointConfigResult> endpointConfigs = new HashMap<String, EndpointConfigResult>();
	private String partitionKeySuffix;
	private String correlationPartitionKeySuffix;
	
	public TelemetryEventRepositoryCassandraImpl(final String nodeName, final CassandraStatementExecutor cassandraStatementExecutor, final Map<String, CorrelationBySourceIdResult> sourceCorrelations) {
		super(sourceCorrelations, nodeName);
		this.cassandraStatementExecutor = cassandraStatementExecutor;
    }
	
	@Override
	protected void startPersist(TelemetryEvent event, DataRetention dataRetention) {
		this.batchStatement.clear();
		this.counterBatchStatement.clear();
		// The following 2 suffixes are defining the diversity of the partition
		// key in cassandra. If a partition is to big for a single key, the
		// dateformat should be displayed in a less general format.
		this.partitionKeySuffix = this.format.format(event.creationTime);
		this.correlationPartitionKeySuffix = this.format.format(event.correlationCreationTime);
		dataRetention.partionKeySuffix = partitionKeySuffix;
	}
	
	@Override
	protected void endPersist() {
		if (this.batchStatement.size() != 0) {
			this.cassandraStatementExecutor.execute(this.batchStatement);
		}
		if (this.counterBatchStatement.size() != 0) {
			this.cassandraStatementExecutor.execute(this.counterBatchStatement);
		}
	}
	
	@Override
	protected void addTelemetryEvent(TelemetryEvent event) {
		this.cassandraStatementExecutor.addTelemetryEvent(event, this.batchStatement);
	}
	
	@Override
	protected void addSourceIdCorrelationData(TelemetryEvent event) {
		this.cassandraStatementExecutor.addSourceIdCorrelationData(event, this.batchStatement);
	}
	
	@Override
	protected void addCorrelationData(TelemetryEvent event, String key, String value) {
		this.cassandraStatementExecutor.addCorrelationData(event, key + partitionKeySuffix, key, value, this.batchStatement);
	}
	
	@Override
	protected void addApplicationCounter(long requestCount, long incomingRequestCount, long outgoingRequestCount, long responseCount,
	        long incomingResponseCount, long outgoingResponseCount, long datagramCount, long incomingDatagramCount,
	        long outgoingDatagramCount, long responseTime, long incomingResponseTime, long outgoingResponseTime, String application,
	        Date statisticsTimestamp) {
		this.cassandraStatementExecutor.addApplicationCounter(
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
		        application, 
		        statisticsTimestamp, 
		        application + this.partitionKeySuffix,
		        this.counterBatchStatement);
	}
	
	@Override
	protected void addEventOccurence(Date eventOccurrenceTimestamp, String occurrenceName, String occurrenceValue) {
		this.cassandraStatementExecutor.addEventOccurence(
				eventOccurrenceTimestamp, 
				occurrenceName, 
				occurrenceValue + this.partitionKeySuffix, 
				occurrenceValue, this.batchStatement);	
	}
	
	@Override
	protected void addEventNameCounter(long requestCount, long responseCount, long datagramCount, long responseTime, String eventName,
	        Date statisticsTimestamp) {
		this.cassandraStatementExecutor.addEventNameCounter(
			requestCount, 
			responseCount, 
			datagramCount, 
			responseTime, 
			eventName,
			statisticsTimestamp,
			eventName + this.partitionKeySuffix, 
			this.counterBatchStatement);
	}
	
	@Override
	protected void addMessageEventStart(TelemetryEvent event) {
		this.cassandraStatementExecutor.addMessageEventStart(event, event.name + this.partitionKeySuffix, this.batchStatement);
	}
	
	@Override
	protected void addMessageEventFinish(TelemetryEvent event) {
		this.cassandraStatementExecutor.addMessageEventFinish(event, event.correlationName + this.correlationPartitionKeySuffix, this.batchStatement);
	}
	
	@Override
	protected void addApplicationEventNameCounter(long requestCount, long incomingRequestCount, long outgoingRequestCount,
	        long responseCount, long incomingResponseCount, long outgoingResponseCount, long datagramCount, long incomingDatagramCount,
	        long outgoingDatagramCount, long responseTime, long incomingResponseTime, long outgoingResponseTime, String application,
	        String eventName, Date statisticsTimestamp) {
		this.cassandraStatementExecutor.addApplicationEventNameCounter(
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
			application,
			eventName, 
			statisticsTimestamp,
			application + this.partitionKeySuffix, 
			this.counterBatchStatement);	
	}
	
	@Override
	protected void addTransactionNameCounter(long requestCount, long responseCount, long responseTime, String transactionName,
	        Date statisticsTimestamp) {
		this.cassandraStatementExecutor.addTransactionNameCounter(
			requestCount, 
			responseCount, 
			responseTime, 
			transactionName, 
			statisticsTimestamp,
			transactionName + this.partitionKeySuffix, 
			this.counterBatchStatement);
	}
	
	@Override
	protected void addTransactionEventStart(TelemetryEvent event) {
		this.cassandraStatementExecutor.addTransactionEventStart(event, event.transactionName + this.partitionKeySuffix, this.batchStatement);
	}
	
	@Override
	protected void addTransactionEventFinish(TelemetryEvent event) {
		this.cassandraStatementExecutor.addTransactionEventFinish(event, event.transactionName + this.correlationPartitionKeySuffix, this.batchStatement);
	}
	
	@Override
	protected void addDataRetention(DataRetention dataRetention) {
		this.cassandraStatementExecutor.addDataRetention(dataRetention, this.batchStatement);
	}

	@Override
    public void doFindParent(String sourceId, String application, CorrelationBySourceIdResult result) {
		if (sourceId == null) {
			return;
		}
		this.cassandraStatementExecutor.findParent(sourceId, application, result);
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
			this.cassandraStatementExecutor.findAndMergeEndpointConfig("*", cachedResult);
			this.cassandraStatementExecutor.findAndMergeEndpointConfig(endpoint, cachedResult);
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

	@Override
    protected void persistPerformance(Date keyTime, String nodeName, Date performanceTime, int offerCount, long offerTime, int enhancingCount,
            long enhancingTime, int indexingCount, long indexingTime, int persistingCount, long persistingTime) {
	    this.cassandraStatementExecutor.persistPerformance(keyTime, nodeName, performanceTime, offerCount, offerTime, enhancingCount, enhancingTime, indexingCount, indexingTime, persistingCount, persistingTime);
	    
    }
}
