package com.holster.etm.processor.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.sf.saxon.xpath.XPathFactoryImpl;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;
import com.holster.etm.processor.TelemetryEvent;
import com.holster.etm.processor.TelemetryEventDirection;
import com.holster.etm.processor.parsers.ExpressionParser;
import com.holster.etm.processor.parsers.FixedPositionExpressionParser;
import com.holster.etm.processor.parsers.FixedValueExpressionParser;
import com.holster.etm.processor.parsers.XPathExpressionParser;

public class StatementExecutor {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(StatementExecutor.class);


	private final Session session;
	private final PreparedStatement insertTelemetryEventStatement;
	private final PreparedStatement insertSourceIdIdStatement;
	private final PreparedStatement insertCorrelationDataStatement;
	private final PreparedStatement insertEventOccurrenceStatement;
	private final PreparedStatement insertTransactionEventStartStatement;
	private final PreparedStatement insertTransactionEventFinishStatement;
	private final PreparedStatement updateApplicationCounterStatement;
	private final PreparedStatement updateEventNameCounterStatement;
	private final PreparedStatement updateApplicationEventNameCounterStatement;
	private final PreparedStatement updateTransactionNameCounterStatement;
	private final PreparedStatement findParentStatement;
	private final PreparedStatement findEndpointConfigStatement;
	
	private final XPath xPath;
	
	public StatementExecutor(final Session session, final String keySpace) {
		this.session = session;
		this.insertTelemetryEventStatement = session.prepare("insert into " + keySpace + ".telemetry_event ("
				+ "id, "
				+ "application, "
				+ "content, "
				+ "correlationId, "
				+ "creationTime, "
				+ "direction, "
				+ "endpoint, "
				+ "expiryTime, "
				+ "name, "
				+ "responseTime, "
				+ "sourceCorrelationId, "
				+ "sourceId, "
				+ "transactionId, "
				+ "transactionName, "
				+ "type"
				+ ") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
		this.insertSourceIdIdStatement = session.prepare("insert into " + keySpace + ".sourceid_id_correlation ("
				+ "sourceId, "
				+ "creationTime, "
				+ "id, "
				+ "transactionId, "
				+ "transactionName"
				+ ") values (?,?,?,?,?);");
		this.insertCorrelationDataStatement = session.prepare("insert into " + keySpace + ".correlation_data ("
				+ "id, "
				+ "name, "
				+ "value, "
				+ "transactionId, "
				+ "transactionName"
				+ ") values (?,?,?,?,?);");
		this.insertEventOccurrenceStatement = session.prepare("insert into " + keySpace + ".event_occurrences ("
				+ "timeunit, "
				+ "type, "
				+ "name"
				+ ") values (?,?,?);");
		this.insertTransactionEventStartStatement = session.prepare("insert into " + keySpace + ".transaction_event ("
				+ "transactionName, "
				+ "transactionId, "
				+ "startTime, "
				+ "expiryTime) values (?, ?, ?, ?);");
		this.insertTransactionEventFinishStatement = session.prepare("insert into " + keySpace + ".transaction_event ("
				+ "transactionName, "
				+ "transactionId, "
				+ "startTime, "
				+ "finishTime) values (?, ?, ?);");
		this.updateApplicationCounterStatement = session.prepare("update " + keySpace + ".application_counter set "
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
				+ "where application = ? and timeunit = ?;");
		this.updateEventNameCounterStatement = session.prepare("update " + keySpace + ".eventname_counter set "
				+ "count = count + 1, "
				+ "messageRequestCount = messageRequestCount + ?, "
				+ "messageResponseCount = messageResponseCount + ?, "
				+ "messageDatagramCount = messageDatagramCount + ?, "
				+ "messageResponseTime = messageResponseTime + ? "
				+ "where eventName = ? and timeunit = ?;");
		this.updateApplicationEventNameCounterStatement = session.prepare("update " + keySpace + ".application_event_counter set "
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
				+ "where application = ? and eventName = ? and timeunit = ?;");
		this.updateTransactionNameCounterStatement = session.prepare("update " + keySpace + ".transactionname_counter set "
				+ "count = count + 1, "
				+ "transactionStart = transactionStart + ?, "
				+ "transactionFinish = transactionFinish + ?, "
				+ "transactionResponseTime = transactionResponseTime + ? "
				+ "where transactionName = ? and timeunit = ?;");
		this.findParentStatement = session.prepare("select "
				+ "id, "
				+ "transactionId, "
				+ "transactionName, "
				+ "creationTime "
				+ "from " + keySpace + ".sourceid_id_correlation where sourceId = ?;");
		this.findEndpointConfigStatement = session.prepare("select "
				+ "direction, "
				+ "applicationParsers, "
				+ "eventNameParsers, "
				+ "correlationParsers, "
				+ "transactionNameParsers "
				+ "from " + keySpace + ".endpoint_config where endpoint = ?;");
		XPathFactory xpf = new XPathFactoryImpl();
		this.xPath = xpf.newXPath();

	}
	
	public void insertTelemetryEvent(final TelemetryEvent event, final boolean async) {
		final ResultSetFuture resultSetFuture = this.session.executeAsync(this.insertTelemetryEventStatement.bind(
				event.id, 
				event.application, 
				event.content, 
				event.correlationId,
				event.creationTime, 
		        event.direction != null ? event.direction.name() : null, 
		        event.endpoint,
		        event.expiryTime,
		        event.name, 
		        event.correlationCreationTime, 
		        event.sourceCorrelationId, 
		        event.sourceId, 
		        event.transactionId, 
		        event.transactionName,
		        event.type != null ? event.type.name() : null));
		if (!async) {
			resultSetFuture.getUninterruptibly();
		}
	}
	
	public void insertSourceIdCorrelationData(final TelemetryEvent event, final boolean async) {
		final ResultSetFuture resultSetFuture = this.session.executeAsync(this.insertSourceIdIdStatement.bind(
				event.sourceId,
				event.creationTime,
				event.id, 
				event.transactionId, 
				event.transactionName));
		if (!async) {
			resultSetFuture.getUninterruptibly();
		}
	}
	
	public void insertCorrelationData(final TelemetryEvent event, final String correlationDataName, final String correlationDataValue, final boolean async) {
		final ResultSetFuture resultSetFuture = this.session.executeAsync(this.insertCorrelationDataStatement.bind(
				event.id, 
				correlationDataName, 
				correlationDataValue, 
				event.transactionId, 
				event.transactionName));
		if (!async) {
			resultSetFuture.getUninterruptibly();
		}
	}
	
	public void insertEventOccurence(final Date timestamp, final String occurrenceName, final String occurrenceValue, boolean async) {
		final ResultSetFuture resultSetFuture = this.session.executeAsync(this.insertEventOccurrenceStatement.bind(
				timestamp, 
				occurrenceName, 
				occurrenceValue));
		if (!async) {
			resultSetFuture.getUninterruptibly();
		}
	}
	
	public void insertTransactionEventStart(TelemetryEvent event, boolean async) {
		final ResultSetFuture resultSetFuture = this.session.executeAsync(this.insertTransactionEventStartStatement.bind(
				event.transactionName,
				event.transactionId, 
		        event.creationTime, 
		        event.expiryTime));
		if (!async) {
			resultSetFuture.getUninterruptibly();
		}
	}
	
	public void insertTransactionEventFinish(TelemetryEvent event, boolean async) {
		final ResultSetFuture resultSetFuture = this.session.executeAsync(this.insertTransactionEventFinishStatement.bind(
				event.transactionName,
				event.transactionId,
				event.correlationCreationTime,
		        event.creationTime));
		if (!async) {
			resultSetFuture.getUninterruptibly();
		}
	}
	
	public void updateApplicationCounter(final long requestCount, final long incomingRequestCount, final long outgoingRequestCount,
	        final long responseCount, final long incomingResponseCount, final long outgoingResponseCount, final long datagramCount,
	        final long incomingDatagramCount, final long outgoingDatagramCount, final long responseTime, final long incomingResponseTime,
	        final long outgoingResponseTime, final String application, final Date timestamp, final boolean async) {
		final ResultSetFuture resultSetFuture = this.session.executeAsync(this.updateApplicationCounterStatement.bind(
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
		        timestamp));
		if (!async) {
			resultSetFuture.getUninterruptibly();
		}
	}
	
	public void updateEventNameCounter(final long requestCount, final long responseCount, final long datagramCount,
	        final long responseTime, final String eventName, final Date timestamp, final boolean async) {
		final ResultSetFuture resultSetFuture = this.session.executeAsync(this.updateEventNameCounterStatement.bind(
				requestCount, 
				responseCount, 
				datagramCount, 
				responseTime, 
				eventName, 
				timestamp));
		if (!async) {
			resultSetFuture.getUninterruptibly();
		}
	}
	
	public void updateApplicationEventNameCounter(final long requestCount, final long incomingRequestCount,
	        final long outgoingRequestCount, final long responseCount, final long incomingResponseCount, final long outgoingResponseCount,
	        final long datagramCount, final long incomingDatagramCount, final long outgoingDatagramCount, final long responseTime,
	        final long incomingResponseTime, final long outgoingResponseTime, final String application, final String eventName,
	        final Date timestamp, final boolean async) {
		final ResultSetFuture resultSetFuture = this.session.executeAsync(this.updateApplicationEventNameCounterStatement.bind(
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
				timestamp));
		if (!async) {
			resultSetFuture.getUninterruptibly();
		}
	}
	
	public void updateTransactionNameCounter(final long requestCount, final long responseCount, final long responseTime,
	        final String transactionName, final Date timestamp, final boolean async) {
		final ResultSetFuture resultSetFuture = this.session.executeAsync(this.updateTransactionNameCounterStatement.bind(
				requestCount, 
				responseCount, 
				responseTime, 
				transactionName, 
				timestamp));
		if (!async) {
			resultSetFuture.getUninterruptibly();
		}
	}
	
	public void findParent(final String sourceId, final CorrelationBySourceIdResult result) {
		final ResultSet resultSet = this.session.execute(this.findParentStatement.bind(sourceId));
		final Row row = resultSet.one();
		if (row != null) {
			result.id = row.getUUID(0);
			result.transactionId = row.getUUID(1);
			result.transactionName = row.getString(2);
			result.creationTime = row.getDate(3);
		}
	}
	
	public void findAndMergeEndpointConfig(final String endpoint, final EndpointConfigResult result) {
		final ResultSet resultSet = this.session.execute(this.findEndpointConfigStatement.bind(endpoint));
		Row row = resultSet.one();
		if (row != null) {
			mergeRowIntoEndpointConfig(row, result);
		}
	}
	
	private void mergeRowIntoEndpointConfig(Row row, EndpointConfigResult result) {
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
    }

	
	private List<ExpressionParser> createExpressionParsers(List<String> expressions) {
		if (expressions == null) {
			return null;
		}
		List<ExpressionParser> expressionParsers = new ArrayList<ExpressionParser>(expressions.size());
		for (String expression : expressions) {
			expressionParsers.add(createExpressionParser(expression));
		}
		return expressionParsers;
	}
	
	private ExpressionParser createExpressionParser(String expression) {
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
				String[] values = range.split("-");
				if (values.length != 2) {
                	if (log.isErrorLevelEnabled()) {
                		log.logErrorMessage("Could not create FixedPositionExpressionParser. Range '" + range + "' is invalid. Using FixedValueExpressionParser instead.");
                	}
                	new FixedValueExpressionParser(null);					
				}
				try {
					Integer start = null;
					Integer end = null;
					if (values[0].length() != 0) {
						start = Integer.valueOf(values[0]);
					} 
					if (values[1].length() != 0) {
						end = Integer.valueOf(values[0]);
					} 
					return new FixedPositionExpressionParser(start, end);
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
}
