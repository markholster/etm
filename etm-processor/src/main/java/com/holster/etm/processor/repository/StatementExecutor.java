package com.holster.etm.processor.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.xpath.XPathFactoryImpl;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.holster.etm.core.TelemetryEventDirection;
import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;
import com.holster.etm.core.sla.SlaRule;
import com.holster.etm.processor.TelemetryEvent;
import com.holster.etm.processor.parsers.ExpressionParser;
import com.holster.etm.processor.parsers.FixedPositionExpressionParser;
import com.holster.etm.processor.parsers.FixedValueExpressionParser;
import com.holster.etm.processor.parsers.XPathExpressionParser;
import com.holster.etm.processor.parsers.XsltExpressionParser;

public class StatementExecutor {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(StatementExecutor.class);

	private final Session session;
	private final PreparedStatement insertTelemetryEventStatement;
	private final PreparedStatement insertSourceIdIdStatement;
	private final PreparedStatement insertCorrelationDataStatement;
	private final PreparedStatement insertCorrelationToParentStatement;
	private final PreparedStatement insertEventOccurrenceStatement;
	private final PreparedStatement insertTransactionEventStartStatement;
	private final PreparedStatement insertTransactionEventFinishStatement;
	private final PreparedStatement insertMessageEventPerformanceStartStatement;
	private final PreparedStatement insertMessageEventPerformanceFinishStatement;
	private final PreparedStatement insertMessageEventExpirationStartStatement;
	private final PreparedStatement insertMessageEventExpirationFinishStatement;
	private final PreparedStatement insertSlaStartStatement;
	private final PreparedStatement insertSlaFinishStatement;
	private final PreparedStatement updateApplicationCounterStatement;
	private final PreparedStatement updateEventNameCounterStatement;
	private final PreparedStatement updateApplicationEventNameCounterStatement;
	private final PreparedStatement updateTransactionNameCounterStatement;
	private final PreparedStatement findParentStatement;
	private final PreparedStatement findEndpointConfigStatement;
	private final PreparedStatement findSlaStartStatement;
	private final PreparedStatement deleteSlaStatement;
	
	private final XPath xPath;
	private final TransformerFactory transformerFactory;
	
	public StatementExecutor(final Session session, final String keyspace) {
		this.session = session;
		this.insertTelemetryEventStatement = session.prepare("insert into " + keyspace + ".telemetry_event ("
				+ "id, "
				+ "application, "
				+ "content, "
				+ "correlationCreationTime, "
				+ "correlationData, "
				+ "correlationId, "
				+ "correlationName, "
				+ "creationTime, "
				+ "direction, "
				+ "endpoint, "
				+ "expiryTime, "
				+ "name, "
				+ "sourceCorrelationId, "
				+ "sourceId, "
				+ "transactionId, "
				+ "transactionName, "
				+ "type, "
				+ "slaRule) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
		this.insertSourceIdIdStatement = session.prepare("insert into " + keyspace + ".sourceid_id_correlation ("
				+ "sourceId, "
				+ "creationTime, "
				+ "expiryTime, "
				+ "id, "
				+ "transactionId, "
				+ "transactionName, "
				+ "name, "
				+ "slaRule"
				+ ") values (?,?,?,?,?,?,?,?);");
		this.insertCorrelationDataStatement = session.prepare("insert into " + keyspace + ".correlation_data ("
				+ "name_timeunit, "
				+ "timeunit, "
				+ "name, "
				+ "value, "
				+ "id"
				+ ") values (?,?,?,?,?);");
		this.insertCorrelationToParentStatement = session.prepare("update " + keyspace + ".telemetry_event set "
				+ "correlations = correlations + ? "
				+ "where id = ?;");
		this.insertEventOccurrenceStatement = session.prepare("insert into " + keyspace + ".event_occurrences ("
				+ "timeunit, "
				+ "type, "
				+ "name_timeframe,"
				+ "name"
				+ ") values (?,?,?,?);");
		this.insertTransactionEventStartStatement = session.prepare("insert into " + keyspace + ".transaction_performance ("
				+ "transactionName_timeunit, "
				+ "transactionId,"
				+ "transactionName,  "
				+ "startTime, "
				+ "expiryTime, "
				+ "application) values (?, ?, ?, ?, ?, ?);");
		this.insertTransactionEventFinishStatement = session.prepare("insert into " + keyspace + ".transaction_performance ("
				+ "transactionName_timeunit, "
				+ "transactionId, "
				+ "startTime, "
				+ "finishTime) values (?, ?, ?, ?);");
		this.insertMessageEventPerformanceStartStatement = session.prepare("insert into " + keyspace + ".message_performance ("
				+ "name_timeunit, "
				+ "id, "
				+ "name, "
				+ "startTime, "
				+ "expiryTime, "
				+ "application) values (?, ?, ?, ?, ?, ?);");
		this.insertMessageEventPerformanceFinishStatement = session.prepare("insert into " + keyspace + ".message_performance ("
				+ "name_timeunit, "
				+ "id, "
				+ "startTime, "
				+ "finishTime) values (?, ?, ?, ?);");
		this.insertMessageEventExpirationStartStatement = session.prepare("insert into " + keyspace + ".message_expiration ("
				+ "name_timeunit, "
				+ "id, "
				+ "name, "
				+ "expiryTime, "
				+ "startTime, "
				+ "application) values (?, ?, ?, ?, ?, ?);");
		this.insertMessageEventExpirationFinishStatement = session.prepare("insert into " + keyspace + ".message_expiration ("
				+ "name_timeunit, "
				+ "id, "
				+ "expiryTime, "
				+ "finishTime) values (?, ?, ?, ?);");
		this.insertSlaStartStatement = session.prepare("insert into " + keyspace + ".transaction_sla ("
				+ "transactionName_timeunit, "
				+ "slaExpiryTime, "
				+ "transactionId, "
				+ "transactionName, "
				+ "startTime, "
				+ "application, "
				+ "slaRule) values (?, ?, ?, ?, ?, ?, ?);");
		this.insertSlaFinishStatement = session.prepare("insert into " + keyspace + ".transaction_sla ("
				+ "transactionName_timeunit, "
				+ "slaExpiryTime, "
				+ "transactionId, "
				+ "finishTime) values (?, ?, ?, ?);");
		this.updateApplicationCounterStatement = session.prepare("update " + keyspace + ".application_counter set "
				+ "count = count + 1, "
				+ "messageRequestCount = messageRequestCount + ?, "
				+ "incomingMessageRequestCount = incomingMessageRequestCount + ?, "
				+ "outgoingMessageRequestCount = outgoingMessageRequestCount + ?, "
				+ "messageResponseCount = messageResponseCount + ?, "
				+ "incomingMessageResponseCount = incomingMessageResponseCount + ?, "
				+ "outgoingMessageResponseCount = outgoingMessageResponseCount + ?, "
				+ "messageDatagramCount = messageDatagramCount + ?, "
				+ "incomingMessageDatagramCount = incomingMessageDatagramCount + ?, "
				+ "outgoingMessageDatagramCount = outgoingMessageDatagramCount + ?, "
				+ "messageResponseTime = messageResponseTime + ?, "
				+ "incomingMessageResponseTime = incomingMessageResponseTime + ?, "
				+ "outgoingMessageResponseTime = outgoingMessageResponseTime + ? "
				+ "where application_timeunit = ? and timeunit = ? and application = ?;");
		this.updateEventNameCounterStatement = session.prepare("update " + keyspace + ".eventname_counter set "
				+ "count = count + 1, "
				+ "messageRequestCount = messageRequestCount + ?, "
				+ "messageResponseCount = messageResponseCount + ?, "
				+ "messageDatagramCount = messageDatagramCount + ?, "
				+ "messageResponseTime = messageResponseTime + ? "
				+ "where eventName_timeunit = ? and timeunit = ? and eventName = ?;");
		this.updateApplicationEventNameCounterStatement = session.prepare("update " + keyspace + ".application_event_counter set "
				+ "count = count + 1, "
				+ "messageRequestCount = messageRequestCount + ?, "
				+ "incomingMessageRequestCount = incomingMessageRequestCount + ?, "
				+ "outgoingMessageRequestCount = outgoingMessageRequestCount + ?, "
				+ "messageResponseCount = messageResponseCount + ?, "
				+ "incomingMessageResponseCount = incomingMessageResponseCount + ?, "
				+ "outgoingMessageResponseCount = outgoingMessageResponseCount + ?, "
				+ "messageDatagramCount = messageDatagramCount + ?, "
				+ "incomingMessageDatagramCount = incomingMessageDatagramCount + ?, "
				+ "outgoingMessageDatagramCount = outgoingMessageDatagramCount + ?, "
				+ "messageResponseTime = messageResponseTime + ?, "
				+ "incomingMessageResponseTime = incomingMessageResponseTime + ?, "
				+ "outgoingMessageResponseTime = outgoingMessageResponseTime + ? "
				+ "where application_timeunit = ? and eventName = ? and timeunit = ? and application = ?;");
		this.updateTransactionNameCounterStatement = session.prepare("update " + keyspace + ".transactionname_counter set "
				+ "count = count + 1, "
				+ "transactionStart = transactionStart + ?, "
				+ "transactionFinish = transactionFinish + ?, "
				+ "transactionResponseTime = transactionResponseTime + ? "
				+ "where transactionName_timeunit = ? and timeunit = ? and transactionName = ?;");
		this.findParentStatement = session.prepare("select "
				+ "id, "
				+ "transactionId, "
				+ "transactionName, "
				+ "creationTime, "
				+ "expiryTime, "
				+ "name, "
				+ "slaRule "
				+ "from " + keyspace + ".sourceid_id_correlation where sourceId = ?;");
		this.findEndpointConfigStatement = session.prepare("select "
				+ "direction, "
				+ "applicationParsers, "
				+ "eventNameParsers, "
				+ "correlationParsers, "
				+ "transactionNameParsers, "
				+ "slaRules "
				+ "from " + keyspace + ".endpoint_config where endpoint = ?;");
		this.findSlaStartStatement = session.prepare("select "
				+ "startTime "
				+ "from " + keyspace + ".transaction_sla where transactionName_timeunit = ? and slaExpiryTime = ? and transactionId = ?;");
		this.deleteSlaStatement = session.prepare("delete from " + keyspace + ".transaction_sla where transactionName_timeunit = ? and slaExpiryTime = ? and transactionId = ?;");
		XPathFactory xpf = new XPathFactoryImpl();
		this.xPath = xpf.newXPath();
		this.transformerFactory = new TransformerFactoryImpl();

	}
	
	public void addTelemetryEvent(final TelemetryEvent event, final BatchStatement batchStatement) {
		batchStatement.add(this.insertTelemetryEventStatement.bind(
				event.id, 
				event.application, 
				event.content,
				event.correlationCreationTime,
				event.correlationData,
				event.correlationId,
				event.correlationName,
				event.creationTime, 
		        event.direction != null ? event.direction.name() : null, 
		        event.endpoint,
		        event.expiryTime,
		        event.name, 
		        event.sourceCorrelationId, 
		        event.sourceId, 
		        event.transactionId, 
		        event.transactionName,
		        event.type != null ? event.type.name() : null,
				event.slaRule != null ? event.slaRule.toConfiguration() : null));
		if (event.correlationId != null) {
			final List<UUID> correlation = new ArrayList<UUID>(1);
			correlation.add(event.id);
			batchStatement.add(this.insertCorrelationToParentStatement.bind(correlation, event.correlationId));
		}
	}
	
	public void addSourceIdCorrelationData(final TelemetryEvent event, final BatchStatement batchStatement) {
		batchStatement.add(this.insertSourceIdIdStatement.bind(
				event.sourceId,
				event.creationTime,
				event.expiryTime,
				event.id, 
				event.transactionId, 
				event.transactionName,
				event.name,
				event.slaRule != null ? event.slaRule.toConfiguration() : null));
	}
	
	public void addCorrelationData(final TelemetryEvent event, final String key, final String correlationDataName, final String correlationDataValue, final BatchStatement batchStatement) {
		batchStatement.add(this.insertCorrelationDataStatement.bind(
				key,
				event.creationTime,
				correlationDataName, 
				correlationDataValue,
				event.id));
	}
	
	public void addEventOccurence(final Date timestamp, final String occurrenceName, final String occurrenceValue, final String originalName, final BatchStatement batchStatement) {
		batchStatement.add(this.insertEventOccurrenceStatement.bind(
				timestamp, 
				occurrenceName, 
				occurrenceValue,
				originalName));
	}
	
	public void addTransactionEventStart(final TelemetryEvent event, final String key, final BatchStatement batchStatement) {
		batchStatement.add(this.insertTransactionEventStartStatement.bind(
				key,
				event.transactionId,
				event.transactionName,
		        event.creationTime, 
		        event.expiryTime,
		        event.application));
		if (event.slaRule != null) {
			batchStatement.add(this.insertSlaStartStatement.bind(
					key, 
					new Date(event.creationTime.getTime() + event.slaRule.getSlaExpiryTime()), 
					event.transactionId, 
					event.transactionName, 
					event.creationTime, 
					event.application, 
					event.slaRule.toConfiguration()));
		}
	}
	
	public void addTransactionEventFinish(final TelemetryEvent event, final String key, final BatchStatement batchStatement) {
		batchStatement.add(this.insertTransactionEventFinishStatement.bind(
				key,
				event.transactionId,
				event.correlationCreationTime,
		        event.creationTime));
		if (event.slaRule != null) {
			if (event.slaRule.compliesToSla(event.creationTime.getTime() - event.correlationCreationTime.getTime())) {
				Row row = this.session.execute(this.findSlaStartStatement.bind(
						key, 
						new Date(event.correlationCreationTime.getTime() + event.slaRule.getSlaExpiryTime()), 
						event.transactionId)).one();
				if (row != null) {
					batchStatement.add(this.deleteSlaStatement.bind(
							key,
							new Date(event.correlationCreationTime.getTime() + event.slaRule.getSlaExpiryTime()),
							event.transactionId));
				} else {
					// Transaction start not yet stored, we have to store the finish.
					batchStatement.add(this.insertSlaFinishStatement.bind(
							key, 
							new Date(event.correlationCreationTime.getTime() + event.slaRule.getSlaExpiryTime()),
							event.transactionId,
							event.creationTime));
					
				}
			} else {
				batchStatement.add(this.insertSlaFinishStatement.bind(
						key, 
						new Date(event.correlationCreationTime.getTime() + event.slaRule.getSlaExpiryTime()),
						event.transactionId,
						event.creationTime));
			}
		}		
	}

	public void addMessageEventStart(final TelemetryEvent event, final String key, final BatchStatement batchStatement) {
		batchStatement.add(this.insertMessageEventPerformanceStartStatement.bind(
				key,
				event.id, 
				event.name,
		        event.creationTime, 
		        event.expiryTime,
		        event.application));
		batchStatement.add(this.insertMessageEventExpirationStartStatement.bind(
				key,
				event.id,
				event.name,
		        event.expiryTime,
		        event.creationTime,
		        event.application));
	}
	
	public void addMessageEventFinish(final TelemetryEvent event, final String key, final BatchStatement batchStatement) {
		batchStatement.add(this.insertMessageEventPerformanceFinishStatement.bind(
				key,
				event.correlationId,
				event.correlationCreationTime,
		        event.creationTime));
		batchStatement.add(this.insertMessageEventExpirationFinishStatement.bind(
				key,
				event.correlationId,
				event.correlationExpiryTime,
		        event.creationTime));
	}

	
	public void addApplicationCounter(final long requestCount, final long incomingRequestCount, final long outgoingRequestCount,
	        final long responseCount, final long incomingResponseCount, final long outgoingResponseCount, final long datagramCount,
	        final long incomingDatagramCount, final long outgoingDatagramCount, final long responseTime, final long incomingResponseTime,
	        final long outgoingResponseTime, final String application, final Date timestamp, final String key, final BatchStatement batchStatement) {
		batchStatement.add(this.updateApplicationCounterStatement.bind(
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
		        key,
		        timestamp,
		        application));
	}
	
	public void addEventNameCounter(final long requestCount, final long responseCount, final long datagramCount,
	        final long responseTime, final String eventName, final Date timestamp, final String key, final BatchStatement batchStatement) {
		batchStatement.add(this.updateEventNameCounterStatement.bind(
				requestCount, 
				responseCount, 
				datagramCount, 
				responseTime, 
				key, 
				timestamp,
				eventName));
	}
	
	public void addApplicationEventNameCounter(final long requestCount, final long incomingRequestCount,
	        final long outgoingRequestCount, final long responseCount, final long incomingResponseCount, final long outgoingResponseCount,
	        final long datagramCount, final long incomingDatagramCount, final long outgoingDatagramCount, final long responseTime,
	        final long incomingResponseTime, final long outgoingResponseTime, final String application, final String eventName,
	        final Date timestamp, final String key, final BatchStatement batchStatement) {
		batchStatement.add(this.updateApplicationEventNameCounterStatement.bind(
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
				key, 
				eventName, 
				timestamp,
				application));
	}
	
	public void addTransactionNameCounter(final long requestCount, final long responseCount, final long responseTime,
	        final String transactionName, final Date timestamp, final String key, final BatchStatement batchStatement) {
		batchStatement.add(this.updateTransactionNameCounterStatement.bind(
				requestCount, 
				responseCount, 
				responseTime, 
				key, 
				timestamp,
				transactionName));
	}
	
	public void findParent(final String sourceId, final CorrelationBySourceIdResult result) {
		final ResultSet resultSet = this.session.execute(this.findParentStatement.bind(sourceId));
		final Row row = resultSet.one();
		if (row != null) {
			result.id = row.getUUID(0);
			result.transactionId = row.getUUID(1);
			result.transactionName = row.getString(2);
			result.creationTime = row.getDate(3);
			result.expiryTime = row.getDate(4);
			result.name = row.getString(5);
			result.slaRule = SlaRule.fromConfiguration(row.getString(6));
		}
	}
	
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
	
	private void mergeRowIntoEndpointConfig(final Row row, final EndpointConfigResult result) {
		String direction = row.getString(0);
		if (direction != null) {
			result.eventDirection = TelemetryEventDirection.valueOf(direction);
		}
		if (result.applicationParsers == null) {
			result.applicationParsers = createExpressionParsers(row.getList(1, String.class));
		} else {
			result.applicationParsers.addAll(0, createExpressionParsers(row.getList(1, String.class)));
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
			result.correlationDataParsers.put(key, createExpressionParser(map.get(key)));
		}
		if (result.transactionNameParsers == null) {
			result.transactionNameParsers = createExpressionParsers(row.getList(4, String.class));
		} else {
			result.transactionNameParsers.addAll(0, createExpressionParsers(row.getList(4, String.class)));
		}
		map = row.getMap(5, String.class, String.class);
		iterator = map.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			SlaRule slaRule = createSlaRule(map.get(key));
			if (slaRule != null) {
				result.slaRules.put(key, slaRule);
			}
		}
    }

	private List<ExpressionParser> createExpressionParsers(final List<String> expressions) {
		if (expressions == null) {
			return null;
		}
		List<ExpressionParser> expressionParsers = new ArrayList<ExpressionParser>(expressions.size());
		for (String expression : expressions) {
			expressionParsers.add(createExpressionParser(expression));
		}
		return expressionParsers;
	}
	
	private ExpressionParser createExpressionParser(final String expression) {
		if (expression.length() > 5) {
			if (expression.charAt(0) == 'x' &&
					expression.charAt(1) == 's' &&
					expression.charAt(2) == 'l' &&
					expression.charAt(3) == 't' &&
					expression.charAt(4) == ':') {
				try {
	                return new XsltExpressionParser(this.transformerFactory, expression.substring(5));
                } catch (TransformerConfigurationException e) {
                	if (log.isErrorLevelEnabled()) {
                		log.logErrorMessage("Could not create XsltExpressionParser. Using FixedValueExpressionParser instead.", e);
                	}
                	new FixedValueExpressionParser(null);
                }				
			}
		}
		if (expression.length() > 6) {
			if (expression.charAt(0) == 'x' &&
				expression.charAt(1) == 'p' &&
				expression.charAt(2) == 'a' &&
				expression.charAt(3) == 't' &&
				expression.charAt(4) == 'h' &&
				expression.charAt(5) == ':') {
				try {
	                return new XPathExpressionParser(this.xPath, expression.substring(6));
                } catch (XPathExpressionException e) {
                	if (log.isErrorLevelEnabled()) {
                		log.logErrorMessage("Could not create XPathExpressionParser. Using FixedValueExpressionParser instead.", e);
                	}
                	new FixedValueExpressionParser(null);
                }
			} else if (expression.charAt(0) == 'f' &&
					   expression.charAt(1) == 'i' &&
					   expression.charAt(2) == 'x' &&
					   expression.charAt(3) == 'e' &&
					   expression.charAt(4) == 'd' &&
					   expression.charAt(5) == ':') {
				String range = expression.substring(6);
				String[] values = range.split("-", 3);
				if (values.length != 3) {
                	if (log.isErrorLevelEnabled()) {
                		log.logErrorMessage("Could not create FixedPositionExpressionParser. Range '" + range + "' is invalid. Using FixedValueExpressionParser instead.");
                	}
                	new FixedValueExpressionParser(null);					
				}
				try {
					Integer line = null;
					Integer start = null;
					Integer end = null;
					if (values[0].trim().length() != 0) {
						line = Integer.valueOf(values[0].trim());
					}
					if (values[1].trim().length() != 0) {
						start = Integer.valueOf(values[1]);
					} 
					if (values[2].trim().length() != 0) {
						end = Integer.valueOf(values[2]);
					} 
					return new FixedPositionExpressionParser(line, start, end);
				} catch (NumberFormatException e) {
                	if (log.isErrorLevelEnabled()) {
                		log.logErrorMessage("Could not create FixedPositionExpressionParser. Range '" + range + "' is invalid. Using FixedValueExpressionParser instead.", e);
                	}
                	new FixedValueExpressionParser(null);										
				}
			}
		}
		return new FixedValueExpressionParser(expression);
	}
	
	private SlaRule createSlaRule(String slaConfiguration) {
	    return SlaRule.fromConfiguration(slaConfiguration);
    }


	public ResultSet execute(BatchStatement batchCounterStatement) {
	    return this.session.execute(batchCounterStatement);
    }
}
