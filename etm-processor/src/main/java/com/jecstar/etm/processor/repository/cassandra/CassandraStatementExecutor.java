package com.jecstar.etm.processor.repository.cassandra;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.jecstar.etm.core.TelemetryEvent;
import com.jecstar.etm.core.TelemetryMessageEvent;
import com.jecstar.etm.core.TelemetryMessageEventType;
import com.jecstar.etm.core.parsers.ExpressionParser;
import com.jecstar.etm.core.parsers.ExpressionParserFactory;
import com.jecstar.etm.core.statistics.StatisticsTimeUnit;
import com.jecstar.etm.core.util.DateUtils;
import com.jecstar.etm.processor.repository.EndpointConfigResult;

public class CassandraStatementExecutor {
	
	private final Session session;
//	private final PreparedStatement insertTelemetryEventStatement;
	private final PreparedStatement insertCorrelationDataStatement;
	private final PreparedStatement insertCorrelationToParentStatement;
	private final PreparedStatement insertResponseTimeAndCorrelationToParentStatement;
//	private final PreparedStatement insertEventOccurrenceStatement;
//	private final PreparedStatement insertTransactionEventStartStatement;
//	private final PreparedStatement insertTransactionEventFinishStatement;
//	private final PreparedStatement insertMessageEventPerformanceStartStatement;
//	private final PreparedStatement insertMessageEventPerformanceFinishStatement;
//	private final PreparedStatement insertMessageEventExpirationStartStatement;
//	private final PreparedStatement insertMessageEventExpirationFinishStatement;
//	private final PreparedStatement insertSlaStartStatement;
//	private final PreparedStatement insertSlaFinishStatement;
//	private final PreparedStatement insertDataRetentionStatement;
//	private final PreparedStatement updateApplicationCounterStatement;
	private final PreparedStatement updateIncomingApplicationEventNameCounterStatement;
	private final PreparedStatement updateOutgoingApplicationEventNameCounterStatement;
//	private final PreparedStatement updateTransactionNameCounterStatement;
	private final PreparedStatement findEndpointConfigStatement;
//	private final PreparedStatement findTelemetryEventByIdStatement;
//	private final PreparedStatement selectTelemetryEventStatement;
	
	public CassandraStatementExecutor(final Session session) {
		this.session = session;
		this.insertCorrelationDataStatement = session.prepare("insert into correlation_data ("
				+ "name, "
				+ "day, "
				+ "value, "
				+ "event_time, "
				+ "event_id) values (?,?,?,?,?);");
		this.insertCorrelationToParentStatement = session.prepare("update telemetry_event set "
				+ "correlations = correlations + ? "
				+ "where id = ?;");
		this.insertResponseTimeAndCorrelationToParentStatement = session.prepare("update telemetry_event set "
				+ "response_time = ?, "
				+ "correlations = correlations + ? "
				+ "where id = ?;");
//		this.insertEventOccurrenceStatement = session.prepare("insert into event_occurrences ("
//				+ "timeunit, "
//				+ "type, "
//				+ "name_timeframe,"
//				+ "name) values (?,?,?,?);");
//		this.insertTransactionEventStartStatement = session.prepare("insert into transaction_performance ("
//				+ "transactionName_timeunit, "
//				+ "transactionId, "
//				+ "transactionName,  "
//				+ "startTime, "
//				+ "expiryTime, "
//				+ "application) values (?,?,?,?,?,?);");
//		this.insertTransactionEventFinishStatement = session.prepare("insert into transaction_performance ("
//				+ "transactionName_timeunit, "
//				+ "transactionId, "
//				+ "startTime, "
//				+ "finishTime) values (?,?,?,?);");
//		this.insertMessageEventPerformanceStartStatement = session.prepare("insert into message_performance ("
//				+ "name_timeunit, "
//				+ "id, "
//				+ "name, "
//				+ "startTime, "
//				+ "expiryTime, "
//				+ "application) values (?,?,?,?,?,?);");
//		this.insertMessageEventPerformanceFinishStatement = session.prepare("insert into message_performance ("
//				+ "name_timeunit, "
//				+ "id, "
//				+ "startTime, "
//				+ "finishTime) values (?,?,?,?);");
//		this.insertMessageEventExpirationStartStatement = session.prepare("insert into message_expiration ("
//				+ "name_timeunit, "
//				+ "id, "
//				+ "name, "
//				+ "expiryTime, "
//				+ "startTime, "
//				+ "application) values (?,?,?,?,?,?);");
//		this.insertMessageEventExpirationFinishStatement = session.prepare("insert into message_expiration ("
//				+ "name_timeunit, "
//				+ "id, "
//				+ "expiryTime, "
//				+ "finishTime) values (?,?,?,?);");
//		this.insertSlaStartStatement = session.prepare("insert into transaction_sla ("
//				+ "transactionName_timeunit, "
//				+ "slaExpiryTime, "
//				+ "transactionId, "
//				+ "transactionName, "
//				+ "startTime, "
//				+ "application, "
//				+ "slaRule) values (?,?,?,?,?,?,?);");
//		this.insertSlaFinishStatement = session.prepare("insert into transaction_sla ("
//				+ "transactionName_timeunit, "
//				+ "slaExpiryTime, "
//				+ "transactionId, "
//				+ "finishTime) values (?,?,?,?);");
//		this.insertDataRetentionStatement = session.prepare("insert into data_retention ("
//				+ "timeunit, "
//				+ "id, "
//				+ "eventOccurrenceTimeunit, "
//				+ "sourceId, "
//				+ "partionKeySuffix, "
//				+ "applicationName, "
//				+ "eventName, "
//				+ "transactionName, "
//				+ "correlationData) values (?,?,?,?,?,?,?,?,?);");
//		this.updateApplicationCounterStatement = session.prepare("update application_counter set "
//				+ "count = count + 1, "
//				+ "messageRequestCount = messageRequestCount + ?, "
//				+ "incomingMessageRequestCount = incomingMessageRequestCount + ?, "
//				+ "outgoingMessageRequestCount = outgoingMessageRequestCount + ?, "
//				+ "messageResponseCount = messageResponseCount + ?, "
//				+ "incomingMessageResponseCount = incomingMessageResponseCount + ?, "
//				+ "outgoingMessageResponseCount = outgoingMessageResponseCount + ?, "
//				+ "messageDatagramCount = messageDatagramCount + ?, "
//				+ "incomingMessageDatagramCount = incomingMessageDatagramCount + ?, "
//				+ "outgoingMessageDatagramCount = outgoingMessageDatagramCount + ?, "
//				+ "messageResponseTime = messageResponseTime + ?, "
//				+ "incomingMessageResponseTime = incomingMessageResponseTime + ?, "
//				+ "outgoingMessageResponseTime = outgoingMessageResponseTime + ? "
//				+ "where application_timeunit = ? and timeunit = ? and application = ?;");
		this.updateIncomingApplicationEventNameCounterStatement = session.prepare("update application_event_counter set "
				+ "count = count + 1, "
				+ "incoming_count = incoming_count + 1, "
				+ "incoming_message_request_count = incoming_message_request_count + ?, "
				+ "incoming_message_response_count = incoming_message_response_count + ?, "
				+ "incoming_message_datagram_count = incoming_message_datagram_count + ? "
				+ "where application_name = ? and day = ? and aggregation = ? and event_time = ? and event_name = ?");
		this.updateOutgoingApplicationEventNameCounterStatement = session.prepare("update application_event_counter set "
				+ "count = count + 1, "
				+ "outgoing_count = outgoing_count + 1, "
				+ "outgoing_message_request_count = outgoing_message_request_count + ?, "
				+ "outgoing_message_response_count = outgoing_message_response_count + ?, "
				+ "outgoing_message_datagram_count = outgoing_message_datagram_count + ? "
				+ "where application_name = ? and day = ? and aggregation = ? and event_time = ? and event_name = ?");
//		this.updateTransactionNameCounterStatement = session.prepare("update transactionname_counter set "
//				+ "count = count + 1, "
//				+ "transactionStart = transactionStart + ?, "
//				+ "transactionFinish = transactionFinish + ?, "
//				+ "transactionResponseTime = transactionResponseTime + ? "
//				+ "where transactionName_timeunit = ? and timeunit = ? and transactionName = ?;");
//		this.findParentStatementWithSourceIdAndApplication = session.prepare("select "
//				+ "id "
//				+ "from sourceid_id_correlation where sourceId = ? and application = ?;");
		this.findEndpointConfigStatement = session.prepare("select "
				+ "reading_application_parsers, "
				+ "writing_application_parsers, "
				+ "event_name_parsers, "
				+ "correlation_parsers, "
				+ "transaction_name_parsers "
				+ "from endpoint_config where endpoint = ?;");
//		this.findTelemetryEventByIdStatement = session.prepare("select "
//				+ "application, "
//				+ "content, "
//				+ "correlationCreationTime, "
//				+ "correlationData, "
//				+ "correlationId, "
//				+ "correlationName, "
//				+ "creationTime, "
//				+ "direction, "
//				+ "endpoint, "
//				+ "expiryTime, "
//				+ "metadata, "
//				+ "name, "
//				+ "sourceCorrelationId, "
//				+ "sourceId, "
//				+ "transactionId, "
//				+ "transactionName, "
//				+ "type, "
//				+ "slaRule "
//				+ "from telemetry_event where id = ?");
	}
	
	public void addTelemetryMessageEvent(final TelemetryMessageEvent event, final BatchStatement batchStatement) {
		// TODO, check if the querybuilder prevents CQL injection.
		Insert insert = QueryBuilder.insertInto("telemetry_event");
		insert.value("id", event.id);
		if (event.correlationId != null) {
			insert.value("correlation_id", event.correlationId);
		}
		if (event.content != null) {
			insert.value("content", event.content);
		}
		if (event.endpoint != null) {
			insert.value("endpoint", event.endpoint);
		}
		if (event.expiryTime != null && event.writingEndpointHandler.handlingTime != null) {
			insert.value("expiry_time", DateUtils.toDate(event.writingEndpointHandler.handlingTime.plus(event.expiryTime)));
		}
		if (event.name != null) {
			insert.value("name", event.name);
		}
		if (event.writingEndpointHandler.applicationName != null) {
			insert.value("writing_application", event.writingEndpointHandler.applicationName);
		}
		if (event.writingEndpointHandler.handlingTime != null) {
			insert.value("writing_time", DateUtils.toDate(event.writingEndpointHandler.handlingTime));
		}
		batchStatement.add(insert);
		if (event.correlationId != null) {
			final List<String> correlation = new ArrayList<String>(1);
			correlation.add(event.id);
			if (TelemetryMessageEventType.MESSAGE_RESPONSE.equals(event.type) && event.writingEndpointHandler.handlingTime != null) {
				batchStatement.add(this.insertResponseTimeAndCorrelationToParentStatement.bind(DateUtils.toDate(event.writingEndpointHandler.handlingTime), correlation, event.correlationId));	
			} else {
				batchStatement.add(this.insertCorrelationToParentStatement.bind(correlation, event.correlationId));
			}
		}
	}
	
	public void addCorrelationData(final TelemetryEvent event, final String correlationDataName, final String correlationDataValue, final BatchStatement batchStatement) {
		batchStatement.add(this.insertCorrelationDataStatement.bind(
				correlationDataName,
				DateUtils.toUTCDay(event.getEventTime()),
				correlationDataValue,
				DateUtils.toDate(event.getEventTime()), 
				event.id));
	}
//	
//	public void addEventOccurence(final Date timestamp, final String occurrenceName, final String occurrenceValue, final String originalName, final BatchStatement batchStatement) {
//		batchStatement.add(this.insertEventOccurrenceStatement.bind(
//				timestamp, 
//				occurrenceName, 
//				occurrenceValue,
//				originalName));
//	}
//	
//	public void addTransactionEventStart(final TelemetryEvent event, final String key, final BatchStatement batchStatement) {
//		batchStatement.add(this.insertTransactionEventStartStatement.bind(
//				key,
//				event.transactionId,
//				event.transactionName,
//		        event.creationTime, 
//		        event.expiryTime,
//		        event.application));
//		if (event.slaRule != null) {
//			batchStatement.add(this.insertSlaStartStatement.bind(
//					key, 
//					new Date(event.creationTime.getTime() + event.slaRule.getSlaExpiryTime()), 
//					event.transactionId, 
//					event.transactionName, 
//					event.creationTime, 
//					event.application, 
//					event.slaRule.toConfiguration()));
//		}
//	}
//	
//	public void addTransactionEventFinish(final TelemetryEvent event, final String key, final BatchStatement batchStatement) {
//		batchStatement.add(this.insertTransactionEventFinishStatement.bind(
//				key,
//				event.transactionId,
//				event.correlationCreationTime,
//		        event.creationTime));
//		if (event.slaRule != null) {
//			// TODO -> ophalen starttijd van parent event en bepalen of de SLA is behaald of niet. Zo niet, dan alleen hier record wegschrijven, en niet ook in de addTransactionEventStart
//			batchStatement.add(this.insertSlaFinishStatement.bind(
//					key, 
//					new Date(event.correlationCreationTime.getTime() + event.slaRule.getSlaExpiryTime()),
//					event.transactionId,
//					event.creationTime));
//		}		
//	}
//
//	public void addMessageEventStart(final TelemetryEvent event, final String key, final BatchStatement batchStatement) {
//		batchStatement.add(this.insertMessageEventPerformanceStartStatement.bind(
//				key,
//				event.id, 
//				event.name,
//		        event.creationTime, 
//		        event.expiryTime,
//		        event.application));
//		batchStatement.add(this.insertMessageEventExpirationStartStatement.bind(
//				key,
//				event.id,
//				event.name,
//		        event.expiryTime,
//		        event.creationTime,
//		        event.application));
//	}
//	
//	public void addMessageEventFinish(final TelemetryEvent event, final String key, final BatchStatement batchStatement) {
//		batchStatement.add(this.insertMessageEventPerformanceFinishStatement.bind(
//				key,
//				event.correlationId,
//				event.correlationCreationTime,
//		        event.creationTime));
//		batchStatement.add(this.insertMessageEventExpirationFinishStatement.bind(
//				key,
//				event.correlationId,
//				event.correlationExpiryTime,
//		        event.creationTime));
//	}
//
//	public void addDataRetention(final DataRetention dataRetention, final BatchStatement batchStatement) {
//		batchStatement.add(this.insertDataRetentionStatement.bind(
//				dataRetention.retentionTimestamp,
//				dataRetention.id,
//				dataRetention.eventOccurrenceTimestamp,
//				dataRetention.sourceId,
//				dataRetention.partionKeySuffix,
//				dataRetention.applicationName,
//				dataRetention.eventName,
//				dataRetention.transactionName,
//				dataRetention.correlationData));
//	}
//	
	
	public void addOutgoingApplicationEventNameCounter(String applicationName, String eventName, LocalDateTime timestamp, StatisticsTimeUnit statisticsTimeUnit,
			long requestCount, long responseCount, long datagramCount, BatchStatement batchStatement) {
		batchStatement.add(this.updateOutgoingApplicationEventNameCounterStatement.bind(
				requestCount,
				responseCount,
				datagramCount,
				applicationName,
				DateUtils.toUTCDay(timestamp),
				statisticsTimeUnit.name(),
				statisticsTimeUnit.toDate(timestamp),
				eventName));
	}
	
	public void addIncomingApplicationEventNameCounter(String applicationName, String eventName, LocalDateTime timestamp, StatisticsTimeUnit statisticsTimeUnit,
			long requestCount, long responseCount, long datagramCount, BatchStatement batchStatement) {
		batchStatement.add(this.updateIncomingApplicationEventNameCounterStatement.bind(
				requestCount,
				responseCount,
				datagramCount,
				applicationName,
				DateUtils.toUTCDay(timestamp),
				statisticsTimeUnit.name(),
				statisticsTimeUnit.toDate(timestamp),
				eventName));
	}
	
//	
//	public void addTransactionNameCounter(final long requestCount, final long responseCount, final long responseTime,
//	        final String transactionName, final Date timestamp, final String key, final BatchStatement batchStatement) {
//		batchStatement.add(this.updateTransactionNameCounterStatement.bind(
//				requestCount, 
//				responseCount, 
//				responseTime, 
//				key, 
//				timestamp,
//				transactionName));
//	}
//	
//	
//	
//	public TelemetryEvent findTelemetryEventById(String id) {
//	    ResultSet resultSet = this.session.execute(this.findTelemetryEventByIdStatement.bind(id));
//		Row row = resultSet.one();
//		if (row != null) {
//			TelemetryMessageEvent event = new TelemetryMessageEvent();
			// TODO execute statement to find event.
//			event.id = id;
//			return event;
//		}	    
//	    return null;
//    }

	public void findAndMergeEndpointConfig(final String endpoint, final EndpointConfigResult result) {
		if (endpoint == null) {
			return;
		}
		final ResultSet resultSet = this.session.execute(this.findEndpointConfigStatement.bind(endpoint));
		Row row = resultSet.one();
		if (row != null) {
			mergeRowIntoEndpointConfig(row, result);
		}
	}
	
	public ResultSet execute(BatchStatement batchStatement) {
		return this.session.execute(batchStatement);
	}
	
	private void mergeRowIntoEndpointConfig(final Row row, final EndpointConfigResult result) {
		if (result.readingApplicationParsers == null) {
			result.readingApplicationParsers = createExpressionParsers(row.getList(0, String.class));
		} else {
			result.readingApplicationParsers.addAll(0, createExpressionParsers(row.getList(0, String.class)));
		}
		if (result.writingApplicationParsers == null) {
			result.writingApplicationParsers = createExpressionParsers(row.getList(1, String.class));
		} else {
			result.writingApplicationParsers.addAll(0, createExpressionParsers(row.getList(1, String.class)));
		}
		if (result.eventNameParsers == null) {
			result.eventNameParsers = createExpressionParsers(row.getList(2, String.class));
		} else {
			result.eventNameParsers.addAll(0, createExpressionParsers(row.getList(2, String.class)));
		}
		Map<String, String> map = row.getMap(3, String.class, String.class);
		Iterator<String> iterator = map.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			result.correlationDataParsers.put(key, ExpressionParserFactory.createExpressionParserFromConfiguration(map.get(key)));
		}
		if (result.transactionNameParsers == null) {
			result.transactionNameParsers = createExpressionParsers(row.getList(4, String.class));
		} else {
			result.transactionNameParsers.addAll(0, createExpressionParsers(row.getList(4, String.class)));
		}
    }

	private List<ExpressionParser> createExpressionParsers(final List<String> expressions) {
		if (expressions == null) {
			return null;
		}
		List<ExpressionParser> expressionParsers = new ArrayList<ExpressionParser>(expressions.size());
		for (String expression : expressions) {
			expressionParsers.add(ExpressionParserFactory.createExpressionParserFromConfiguration(expression));
		}
		return expressionParsers;
	}

}
