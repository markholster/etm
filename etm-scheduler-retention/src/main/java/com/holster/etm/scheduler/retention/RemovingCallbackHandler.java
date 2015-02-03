package com.holster.etm.scheduler.retention;

import java.io.Closeable;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.StreamingResponseCallback;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrDocument;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BatchStatement.Type;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.holster.etm.core.TelemetryEventDirection;
import com.holster.etm.core.TelemetryEventType;
import com.holster.etm.core.cassandra.PartitionKeySuffixCreator;
import com.holster.etm.core.configuration.EtmConfiguration;
import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;
import com.holster.etm.core.sla.SlaRule;

public class RemovingCallbackHandler extends StreamingResponseCallback implements Closeable {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(RemovingCallbackHandler.class);

	private final int nrOfDocumentsPerRequest = 50;
	
	private final Session session;
	private final SolrServer solrServer;
	private final EtmConfiguration etmConfiguration;
	
	private final DateFormat format = new PartitionKeySuffixCreator();
	private final UpdateRequest request = new UpdateRequest();
	private final List<String> idsToDelete = new ArrayList<String>();
	private final Map<String, Date> applicationPartitionKeys = new HashMap<String, Date>();
	private final Map<String, Date> eventNamePartitionKeys = new HashMap<String, Date>();
	private final Map<String, Date> transactionNamePartitionKeys = new HashMap<String, Date>();	
	private final BatchStatement batchStatement = new BatchStatement(Type.UNLOGGED);
	private final BatchStatement counterBatchStatement = new BatchStatement(Type.COUNTER);
	private final Date statisticsTimestamp = new Date();
	private final Date eventOccurrenceTimestamp = new Date();
	
	private final PreparedStatement selectEventStatement;
	private final PreparedStatement selectApplicationCounterStatement;
	private final PreparedStatement selectEventNameCounterStatement;
	private final PreparedStatement selectApplicationEventNameCounterStatement;
	private final PreparedStatement selectTransactionNameCounterStatement;
	private final PreparedStatement deleteMessagePerformanceStatement;
	private final PreparedStatement deleteMessageExpirationStatement;
	private final PreparedStatement deleteTransactionPerformanceStatement;
	private final PreparedStatement deleteApplicationCounterStatement;
	private final PreparedStatement deleteEventNameCounterStatement;
	private final PreparedStatement deleteApplicationEventNameCounterStatement;
	private final PreparedStatement deleteTransactionNameCounterStatement;
	private final PreparedStatement deleteCorrelationDataStatement;
	private final PreparedStatement deleteSourceIdCorrelationStatement;
	private final PreparedStatement deleteTelemetryEventStatement;
	private final PreparedStatement deleteEventOccurrenceStatement;
	private final PreparedStatement deleteTransactionSlaStatement;
	private final PreparedStatement updateApplicationCounterStatement;
	private final PreparedStatement updateEventNameCounterStatement;
	private final PreparedStatement updateApplicationEventNameCounterStatement;
	private final PreparedStatement updateTransactionNameCounterStatement;
	private final PreparedStatement countApplicationCountersStatement;
	private final PreparedStatement countApplicationEventCountersStatement;
	private final PreparedStatement countEventNameCountersStatement;
	private final PreparedStatement countTransactionNameCountersStatement;
	private final PreparedStatement countMessagePerformancesStatement;
	private final PreparedStatement countMessageExpirationsStatement;
	private final PreparedStatement countTransactionPerformancesStatement;

	public RemovingCallbackHandler(SolrServer solrServer, Session session, EtmConfiguration etmConfiguration) {
		this.solrServer = solrServer;
		this.session = session;
		this.etmConfiguration = etmConfiguration;
		String keyspace = this.etmConfiguration.getCassandraKeyspace();
		this.selectEventStatement = this.session.prepare("select application, correlationCreationTime, correlationData, creationTime, direction, expiryTime, name, sourceId, transactionId, transactionName, type, slaRule from " + keyspace + ".telemetry_event where id = ?;");
		this.selectApplicationCounterStatement = this.session.prepare("select count from " + keyspace + ".application_counter where application_timeunit = ? and  timeunit = ? and application = ?;");
		this.selectEventNameCounterStatement = this.session.prepare("select count from " + keyspace + ".eventname_counter where eventName_timeunit = ? and timeunit = ? and eventName = ?;");
		this.selectApplicationEventNameCounterStatement = this.session.prepare("select count from " + keyspace + ".application_event_counter where application_timeunit = ? and  timeunit = ? and application = ? and eventName = ?;");
		this.selectTransactionNameCounterStatement = this.session.prepare("select count from " + keyspace + ".transactionname_counter where transactionName_timeunit = ? and timeunit = ? and transactionName = ?;");
		this.deleteMessagePerformanceStatement = this.session.prepare("delete from " + keyspace + ".message_performance where name_timeunit = ? and startTime = ? and id = ?;");
		this.deleteMessageExpirationStatement = this.session.prepare("delete from " + keyspace + ".message_expiration where name_timeunit = ? and expiryTime = ? and id = ?;");
		this.deleteTransactionPerformanceStatement = this.session.prepare("delete from " + keyspace + ".transaction_performance where transactionName_timeunit = ? and startTime = ? and transactionId = ?;");
		this.deleteApplicationCounterStatement = this.session.prepare("delete from " + keyspace + ".application_counter where application_timeunit = ? and  timeunit = ? and application = ?;");
		this.deleteEventNameCounterStatement = this.session.prepare("delete from " + keyspace + ".eventname_counter where eventName_timeunit = ? and timeunit = ? and eventName = ?;");
		this.deleteApplicationEventNameCounterStatement = this.session.prepare("delete from " + keyspace + ".application_event_counter where application_timeunit = ? and  timeunit = ? and application = ? and eventName = ?;");
		this.deleteTransactionNameCounterStatement = this.session.prepare("delete from " + keyspace + ".transactionname_counter where transactionName_timeunit = ? and  timeunit = ? and transactionName = ?;");
		this.deleteCorrelationDataStatement = this.session.prepare("delete from " + keyspace + ".correlation_data where name_timeunit = ? and name = ? and value = ? and timeunit = ? and id = ?;");
		this.deleteSourceIdCorrelationStatement = this.session.prepare("delete from " + keyspace + ".sourceid_id_correlation where sourceId = ?;");
		this.deleteTelemetryEventStatement = this.session.prepare("delete from " + keyspace + ".telemetry_event where id = ?;");
		this.deleteEventOccurrenceStatement = this.session.prepare("delete from " + keyspace + ".event_occurrences where timeunit = ? and type = ? and name_timeframe = ?;");
		this.deleteTransactionSlaStatement = this.session.prepare("delete from " + keyspace + ".transaction_sla where transactionName_timeunit = ? and slaExpiryTime = ? and transactionId = ?;");
		this.updateApplicationCounterStatement = this.session.prepare("update " + keyspace + ".application_counter set "
				+ "count = count - 1, "
				+ "messageRequestCount = messageRequestCount - ?, "
				+ "incomingMessageRequestCount = incomingMessageRequestCount - ?, "
				+ "outgoingMessageRequestCount = outgoingMessageRequestCount - ?, "
				+ "messageResponseCount = messageResponseCount - ?, "
				+ "incomingMessageResponseCount = incomingMessageResponseCount - ?, "
				+ "outgoingMessageResponseCount = outgoingMessageResponseCount - ?, "
				+ "messageDatagramCount = messageDatagramCount - ?, "
				+ "incomingMessageDatagramCount = incomingMessageDatagramCount - ?, "
				+ "outgoingMessageDatagramCount = outgoingMessageDatagramCount - ?, "
				+ "messageResponseTime = messageResponseTime - ?, "
				+ "incomingMessageResponseTime = incomingMessageResponseTime - ?, "
				+ "outgoingMessageResponseTime = outgoingMessageResponseTime - ? "
				+ "where application_timeunit = ? and timeunit = ? and application = ?;");
		this.updateEventNameCounterStatement = session.prepare("update " + keyspace + ".eventname_counter set "
				+ "count = count - 1, "
				+ "messageRequestCount = messageRequestCount - ?, "
				+ "messageResponseCount = messageResponseCount - ?, "
				+ "messageDatagramCount = messageDatagramCount - ?, "
				+ "messageResponseTime = messageResponseTime - ? "
				+ "where eventName_timeunit = ? and timeunit = ? and eventName = ?;");
		this.updateApplicationEventNameCounterStatement = this.session.prepare("update " + keyspace + ".application_event_counter set "
				+ "count = count - 1, "
				+ "messageRequestCount = messageRequestCount - ?, "
				+ "incomingMessageRequestCount = incomingMessageRequestCount - ?, "
				+ "outgoingMessageRequestCount = outgoingMessageRequestCount - ?, "
				+ "messageResponseCount = messageResponseCount - ?, "
				+ "incomingMessageResponseCount = incomingMessageResponseCount - ?, "
				+ "outgoingMessageResponseCount = outgoingMessageResponseCount - ?, "
				+ "messageDatagramCount = messageDatagramCount - ?, "
				+ "incomingMessageDatagramCount = incomingMessageDatagramCount - ?, "
				+ "outgoingMessageDatagramCount = outgoingMessageDatagramCount - ?, "
				+ "messageResponseTime = messageResponseTime - ?, "
				+ "incomingMessageResponseTime = incomingMessageResponseTime - ?, "
				+ "outgoingMessageResponseTime = outgoingMessageResponseTime - ? "
				+ "where application_timeunit = ? and timeunit = ? and application = ? and eventName = ?;");
		this.updateTransactionNameCounterStatement = session.prepare("update " + keyspace + ".transactionname_counter set "
				+ "count = count - 1, "
				+ "transactionStart = transactionStart - ?, "
				+ "transactionFinish = transactionFinish - ?, "
				+ "transactionResponseTime = transactionResponseTime - ? "
				+ "where transactionName_timeunit = ? and timeunit = ? and transactionName = ?;");
		this.countApplicationCountersStatement = this.session.prepare("select application_timeunit from " + keyspace + ".application_counter where application_timeunit = ? limit 1");
		this.countApplicationEventCountersStatement = this.session.prepare("select application_timeunit from " + keyspace + ".application_event_counter where application_timeunit = ? limit 1");
		this.countEventNameCountersStatement = this.session.prepare("select eventName_timeunit from " + keyspace + ".eventname_counter where eventName_timeunit = ? limit 1");
		this.countMessagePerformancesStatement = this.session.prepare("select name_timeunit from " + keyspace + ".message_performance where name_timeunit = ? limit 1");
		this.countMessageExpirationsStatement = this.session.prepare("select name_timeunit from " + keyspace + ".message_expiration where name_timeunit = ? limit 1");
		this.countTransactionNameCountersStatement = this.session.prepare("select transactionName_timeunit from " + keyspace + ".transactionname_counter where transactionName_timeunit = ? limit 1");
		this.countTransactionPerformancesStatement = this.session.prepare("select transactionName_timeunit from " + keyspace + ".transaction_performance where transactionName_timeunit = ? limit 1");
	}

	@Override
	public void streamSolrDocument(SolrDocument doc) {
		this.idsToDelete.add((String) doc.get("id"));
		if (this.idsToDelete.size() >= this.nrOfDocumentsPerRequest) {
			removeEvents();
		}
	}

	private void removeEvents() {
		if (this.idsToDelete.size() == 0) {
			return;
		}
		final long statisticsFactor = this.etmConfiguration.getStatisticsTimeUnit().toMillis(1);
		try {
			// First remove events from search index.
			this.request.deleteById(this.idsToDelete);
			this.request.setCommitWithin(60000);
	        this.solrServer.request(this.request);
			this.request.clear();
			// Remove events from cassandra cluster.
			for (String idToDelete : this.idsToDelete) {
				UUID id = UUID.fromString(idToDelete);
				Row row = this.session.execute(this.selectEventStatement.bind(id)).one();
				if (row == null) {
					continue;
				}
				String application = row.getString(0);
				Date correlationCreationTime = row.getDate(1);
				Map<String, String> correlationData = row.getMap(2, String.class, String.class);
				Date creationTime = row.getDate(3);
				String direction = row.getString(4);
				Date expiryTime = row.getDate(5);
				String eventName = row.getString(6);
				String sourceId = row.getString(7);
				UUID transactionId = row.getUUID(8);
				String transactionName = row.getString(9);
				String type = row.getString(9);
				SlaRule slaRule = SlaRule.fromConfiguration(row.getString(10));
				final String partitionKeySuffix = this.format.format(creationTime);
				this.statisticsTimestamp.setTime(normalizeTime(creationTime.getTime(), statisticsFactor));
				this.eventOccurrenceTimestamp.setTime(normalizeTime(creationTime.getTime(), PartitionKeySuffixCreator.SMALLEST_TIMUNIT_UNIT.toMillis(1)));
				if (TelemetryEventType.MESSAGE_REQUEST.equals(type) && !this.etmConfiguration.isDataRetentionPreserveEventPerformances()) {
					removePerformances(id, creationTime, expiryTime, eventName, transactionId, transactionName, partitionKeySuffix);
				}
				if (TelemetryEventType.MESSAGE_REQUEST.equals(type) && !this.etmConfiguration.isDataRetentionPreserveEventSlas()) {
					removeTransactionSlas(creationTime, transactionId, transactionName, partitionKeySuffix, slaRule);
				}
				if (!this.etmConfiguration.isDataRetentionPreserveEventCounts()) {
					removeCounters(direction, type, application, eventName, transactionName, correlationCreationTime, creationTime, this.statisticsTimestamp, partitionKeySuffix);
				}
				removeEvent(id, creationTime, correlationData, sourceId, partitionKeySuffix);
				if (eventName != null && !this.eventNamePartitionKeys.containsKey(eventName + partitionKeySuffix)) {
					this.eventNamePartitionKeys.put(eventName + partitionKeySuffix, this.eventOccurrenceTimestamp);
				}
				if (application != null && !this.applicationPartitionKeys.containsKey(application + partitionKeySuffix)) {
					this.applicationPartitionKeys.put(application + partitionKeySuffix, this.eventOccurrenceTimestamp);
				}
				if (transactionName != null && !this.transactionNamePartitionKeys.containsKey(transactionName + partitionKeySuffix)) {
					this.transactionNamePartitionKeys.put(transactionName + partitionKeySuffix, this.eventOccurrenceTimestamp);
				}
				// First update the counters, as a delete statement of the same
				// row could be executed in the "non-counter-statement". If the
				// order is reverted the update would lead to an insert of the
				// row with a negative value.
				if (this.counterBatchStatement.size() != 0) {
					this.session.execute(this.counterBatchStatement);
					this.counterBatchStatement.clear();
				}
				if (this.batchStatement.size() != 0) {
					this.session.execute(this.batchStatement);
					this.batchStatement.clear();
				}
			}
			removeApplicationPartitionKeys(this.applicationPartitionKeys);
			removeEventNamePartitionKeys(this.eventNamePartitionKeys);
			removeTransactionNamePartitionKeys(this.transactionNamePartitionKeys);
			if (this.batchStatement.size() != 0) {
				this.session.execute(this.batchStatement);
				this.batchStatement.clear();
			}
			this.applicationPartitionKeys.clear();
			this.eventNamePartitionKeys.clear();
			this.transactionNamePartitionKeys.clear();
			this.idsToDelete.clear();
        } catch (SolrServerException | IOException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Error removing events from system.", e);
        	}
        } 
    }

	private void removeEvent(UUID id, Date creationTime, Map<String, String> correlationData, String sourceId, String partitionKeySuffix) {
		if (correlationData != null && !correlationData.isEmpty()) {
			correlationData.forEach((k,v) ->  this.batchStatement.add(this.deleteCorrelationDataStatement.bind(k + partitionKeySuffix, k, v, creationTime, id)));
		}
	    if (sourceId != null) {
	    	this.batchStatement.add(this.deleteSourceIdCorrelationStatement.bind(sourceId));
	    }
	    this.batchStatement.add(this.deleteTelemetryEventStatement.bind(id));
    }

	private void removePerformances(UUID id, Date creationTime, Date expiryTime, String eventName, UUID transactionId, String transactionName, String partitionKeySuffix) {
	    if (eventName != null) {
	    	this.batchStatement.add(this.deleteMessagePerformanceStatement.bind(eventName + partitionKeySuffix, creationTime, id));
	    	if (expiryTime != null) {
	    		this.batchStatement.add(this.deleteMessageExpirationStatement.bind(eventName + partitionKeySuffix, expiryTime, id));
	    	}
	    }
	    if (transactionId != null && transactionName != null) {
	    	this.batchStatement.add(this.deleteTransactionPerformanceStatement.bind(transactionName + partitionKeySuffix, creationTime, transactionId));
	    }
    }
	
	private void removeTransactionSlas(Date creationTime, UUID transactionId, String transactionName, String partitionKeySuffix, SlaRule slaRule) {
	    if (transactionId != null && transactionName != null && slaRule != null) {
	    	this.batchStatement.add(this.deleteTransactionSlaStatement.bind(transactionName + partitionKeySuffix, new Date(creationTime.getTime() + slaRule.getSlaExpiryTime()), transactionId));
	    }
    }

	
	private void removeCounters(String direction, String type, String application, String eventName, String transactionName, Date correlationCreationTime, Date creationTime, Date statisticsTimestamp, String partitionKeySuffix) {
		long requestCount = TelemetryEventType.MESSAGE_REQUEST.equals(type) ? 1 : 0;
		long incomingRequestCount = TelemetryEventType.MESSAGE_REQUEST.equals(type) && TelemetryEventDirection.INCOMING.equals(direction) ? 1 : 0;
		long outgoingRequestCount = TelemetryEventType.MESSAGE_REQUEST.equals(type) && TelemetryEventDirection.OUTGOING.equals(direction) ? 1 : 0;
		long responseCount = TelemetryEventType.MESSAGE_RESPONSE.equals(type) ? 1 : 0;
		long incomingResponseCount = TelemetryEventType.MESSAGE_RESPONSE.equals(type) && TelemetryEventDirection.INCOMING.equals(direction) ? 1 : 0;
		long outgoingResponseCount = TelemetryEventType.MESSAGE_RESPONSE.equals(type) && TelemetryEventDirection.OUTGOING.equals(direction) ? 1 : 0;
		long datagramCount = TelemetryEventType.MESSAGE_DATAGRAM.equals(type) ? 1 : 0;
		long incomingDatagramCount = TelemetryEventType.MESSAGE_DATAGRAM.equals(type) && TelemetryEventDirection.INCOMING.equals(direction) ? 1 : 0;
		long outgoingDatagramCount = TelemetryEventType.MESSAGE_DATAGRAM.equals(type) && TelemetryEventDirection.OUTGOING.equals(direction) ? 1 : 0;
		long responseTime = 0;
		long incomingResponseTime = 0;
		long outgoingResponseTime = 0;
		if (responseCount > 0) {
			if (creationTime.getTime() != 0 && correlationCreationTime.getTime() != 0) {
				responseTime = creationTime.getTime() - correlationCreationTime.getTime(); 
			}
			incomingResponseTime = TelemetryEventDirection.INCOMING.equals(direction) ? responseTime : 0;
			outgoingResponseTime = TelemetryEventDirection.OUTGOING.equals(direction) ? responseTime : 0;
		}
		if (application != null) {
			Row row = this.session.execute(this.selectApplicationCounterStatement.bind(application + partitionKeySuffix, statisticsTimestamp, application)).one();
			if (row != null) {
				long counter = row.getLong(0);
				if (counter <= 1) {
					// This is the only event at this statisticTimestamp -> the row can be deleted.
					this.counterBatchStatement.add(this.deleteApplicationCounterStatement.bind(application + partitionKeySuffix, statisticsTimestamp, application));
				} else {
					// More events at the same statisticTimestamp, decrease the counters.
					this.counterBatchStatement.add(this.updateApplicationCounterStatement.bind(requestCount, incomingRequestCount,
					        outgoingRequestCount, responseCount, incomingResponseCount, outgoingResponseCount, datagramCount,
					        incomingDatagramCount, outgoingDatagramCount, responseTime, incomingResponseTime, outgoingResponseTime,
					        application + partitionKeySuffix, statisticsTimestamp, application));
				}
			}
		}
		if (eventName != null) {
			Row row = this.session.execute(this.selectEventNameCounterStatement.bind(eventName + partitionKeySuffix, statisticsTimestamp, eventName)).one();
			if (row != null) {
				long counter = row.getLong(0);
				if (counter <= 1) {
					// This is the only event at this statisticTimestamp -> the row can be deleted.
					this.counterBatchStatement.add(this.deleteEventNameCounterStatement.bind(eventName + partitionKeySuffix, statisticsTimestamp, eventName));
				} else {
					// More events at the same statisticTimestamp, decrease the counters.
					this.counterBatchStatement.add(this.updateEventNameCounterStatement.bind(requestCount, responseCount, datagramCount,
					        responseTime, eventName + partitionKeySuffix, statisticsTimestamp, eventName));
				}
			}
		}
		if (application != null && eventName != null) {
			Row row = this.session.execute(this.selectApplicationEventNameCounterStatement.bind(application + partitionKeySuffix, statisticsTimestamp, application, eventName)).one();
			if (row != null) {
				long counter = row.getLong(0);
				if (counter <= 1) {
					// This is the only event at this statisticTimestamp -> the row can be deleted.
					this.counterBatchStatement.add(this.deleteApplicationEventNameCounterStatement.bind(application + partitionKeySuffix, statisticsTimestamp, application, eventName));
				} else {
					// More events at the same statisticTimestamp, decrease the counters.
					this.counterBatchStatement.add(this.updateApplicationEventNameCounterStatement.bind(requestCount, incomingRequestCount,
					        outgoingRequestCount, responseCount, incomingResponseCount, outgoingResponseCount, datagramCount,
					        incomingDatagramCount, outgoingDatagramCount, responseTime, incomingResponseTime, outgoingResponseTime,
					        application + partitionKeySuffix, statisticsTimestamp, application, eventName));
				}
			}			
		}
		if (transactionName != null) {
			Row row = this.session.execute(this.selectTransactionNameCounterStatement.bind(transactionName + partitionKeySuffix, statisticsTimestamp, transactionName)).one();
			if (row != null) {
				long counter = row.getLong(0);
				if (counter <= 1) {
					// This is the only event at this statisticTimestamp -> the row can be deleted.
					this.counterBatchStatement.add(this.deleteTransactionNameCounterStatement.bind(transactionName + partitionKeySuffix, statisticsTimestamp, transactionName));
				} else {
					// More events at the same statisticTimestamp, decrease the counters.
					this.counterBatchStatement.add(this.updateTransactionNameCounterStatement.bind(requestCount, responseCount,
					        responseTime, transactionName + partitionKeySuffix, statisticsTimestamp, transactionName));
				}
			}
		}
    }
	
	private void removeApplicationPartitionKeys(Map<String, Date> applicationPartitionKeys) {
	    if (applicationPartitionKeys.size() <= 0) {
	    	return;
	    }
	    for (String applicationPartitionKey : applicationPartitionKeys.keySet()) {
	    	Row row = this.session.execute(this.countApplicationCountersStatement.bind(applicationPartitionKey)).one();
	    	if (row != null) {
	    		continue;
	    	}
	    	row = this.session.execute(this.countApplicationEventCountersStatement.bind(applicationPartitionKey)).one();
	    	if (row != null) {
	    		continue;
	    	}
	    	this.batchStatement.add(this.deleteEventOccurrenceStatement.bind(applicationPartitionKeys.get(applicationPartitionKey), "Application", applicationPartitionKey));
	    }
    }

	private void removeEventNamePartitionKeys(Map<String, Date> eventNamePartitionKeys) {
	    if (eventNamePartitionKeys.size() <= 0) {
	    	return;
	    }
	    for (String eventNamePartitionKey : eventNamePartitionKeys.keySet()) {
	    	Row row = this.session.execute(this.countEventNameCountersStatement.bind(eventNamePartitionKey)).one();
	    	if (row != null) {
	    		continue;
	    	}
	    	row = this.session.execute(this.countMessagePerformancesStatement.bind(eventNamePartitionKey)).one();
	    	if (row != null) {
	    		continue;
	    	}
	    	row = this.session.execute(this.countMessageExpirationsStatement.bind(eventNamePartitionKey)).one();
	    	if (row != null) {
	    		continue;
	    	}
	    	this.batchStatement.add(this.deleteEventOccurrenceStatement.bind(eventNamePartitionKeys.get(eventNamePartitionKey), "MessageName", eventNamePartitionKey));
	    }
    }
	
	private void removeTransactionNamePartitionKeys(Map<String, Date> transactionNamePartitionKeys) {
	    if (transactionNamePartitionKeys.size() <= 0) {
	    	return;
	    }
	    for (String transactionNamePartitionKey : transactionNamePartitionKeys.keySet()) {
	    	Row row = this.session.execute(this.countTransactionNameCountersStatement.bind(transactionNamePartitionKey)).one();
	    	if (row != null) {
	    		continue;
	    	}
	    	row = this.session.execute(this.countTransactionPerformancesStatement.bind(transactionNamePartitionKey)).one();
	    	if (row != null) {
	    		continue;
	    	}
	    	this.batchStatement.add(this.deleteEventOccurrenceStatement.bind(transactionNamePartitionKeys.get(transactionNamePartitionKey), "TransactionName", transactionNamePartitionKey));
	    }    
	}


	private long normalizeTime(long timeInMillis, long factor) {
		return (timeInMillis / factor) * factor;
    }
	
	@Override
	public void streamDocListInfo(long numFound, long start, Float maxScore) {
	}

	@Override
    public void close() {
	    if (this.idsToDelete.size() != 0) {
	    	removeEvents();
	    }
    }

}
