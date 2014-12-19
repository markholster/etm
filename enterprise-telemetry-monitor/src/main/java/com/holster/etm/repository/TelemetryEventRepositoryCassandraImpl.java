package com.holster.etm.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.holster.etm.TelemetryEvent;
import com.holster.etm.TelemetryEventDirection;
import com.holster.etm.TelemetryEventType;
import com.holster.etm.parsers.ExpressionParser;
import com.holster.etm.parsers.FixedPositionExpressionParser;
import com.holster.etm.parsers.FixedValueExpressionParser;
import com.holster.etm.parsers.XPathExpressionParser;

public class TelemetryEventRepositoryCassandraImpl implements TelemetryEventRepository {

	private final Session session;
	private final Map<String, CorrelationBySourceIdResult> sourceCorrelations;
	
	private final PreparedStatement insertTelemetryEventStatement;
	private final PreparedStatement insertSourceIdIdStatement;
	private final PreparedStatement insertCorrelationDataStatement;
	private final PreparedStatement updateApplicationCounterStatement;
	private final PreparedStatement updateEventNameCounterStatement;
	private final PreparedStatement updateApplicationEventNameCounterStatement;
	private final PreparedStatement updateTransactionNameCounterStatement;
	private final PreparedStatement findParentStatement;
	private final PreparedStatement findEndpointConfigStatement;
	
	private final Date timestamp = new Date();
	private final Map<String, EndpointConfigResult> endpointConfigs = new HashMap<String, EndpointConfigResult>();
	private final XPath xPath;
	
	
	public TelemetryEventRepositoryCassandraImpl(final Session session, final Map<String, CorrelationBySourceIdResult> sourceCorrelations) {
		this.session = session;
		this.sourceCorrelations = sourceCorrelations;
		final String keySpace = "etm";
		
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
				+ "correlationParsers "
				+ "from " + keySpace + ".endpoint_config where endpoint = ?;");
		XPathFactory xpf = XPathFactory.newInstance();
		this.xPath = xpf.newXPath();
    }
	
	@Override
    public void persistTelemetryEvent(TelemetryEvent event) {
		this.timestamp.setTime(normalizeTime(event.creationTime.getTime()));
		this.session.executeAsync(this.insertTelemetryEventStatement.bind(
				event.id, 
				event.application, 
				event.content, 
				event.correlationId,
				event.creationTime, 
		        event.direction != null ? event.direction.name() : null, 
		        event.endpoint,
		        event.expiryTime,
		        event.name, 
		        event.responseTime, 
		        event.sourceCorrelationId, 
		        event.sourceId, 
		        event.transactionId, 
		        event.transactionName,
		        event.type != null ? event.type.name() : null));
		if (event.sourceId != null) {
			// Synchronouse execution because soureCorrelationd list needs to be cleared with this event after the data is present in the database.
			this.session.execute(this.insertSourceIdIdStatement.bind(
					event.sourceId,
					event.creationTime,
					event.id, 
					event.transactionId, 
					event.transactionName));
			this.sourceCorrelations.remove(event.sourceId);
		}
		if (!event.correlationData.isEmpty()) {
			event.correlationData.forEach((k,v) -> this.session.executeAsync(this.insertCorrelationDataStatement.bind(event.id, k, v, event.transactionId, event.transactionName)));
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
			responseTime = event.responseTime;
			incomingResponseTime = TelemetryEventDirection.INCOMING.equals(event.direction) ? event.responseTime : 0;
			outgoingResponseTime = TelemetryEventDirection.OUTGOING.equals(event.direction) ? event.responseTime : 0;
		}
		if (event.application != null) {
			this.session.executeAsync(this.updateApplicationCounterStatement.bind(
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
			        this.timestamp));
		}
		if (event.name != null) {
			this.session.executeAsync(this.updateEventNameCounterStatement.bind(
					requestCount, 
					responseCount, 
					datagramCount, 
					responseTime, 
					event.name, 
					this.timestamp));
		}
		if (event.application != null && event.name != null) {
			this.session.executeAsync(this.updateApplicationEventNameCounterStatement.bind(
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
					this.timestamp));
		}
		if (event.transactionName != null) {
			// event belongs to a transaction, and is correlated so it's the end of a transaction
			this.session.executeAsync(this.updateTransactionNameCounterStatement.bind(requestCount, responseCount, responseTime, event.transactionName, this.timestamp));
		}
    }

	private long normalizeTime(long currentTimeMillis) {
		return (currentTimeMillis / 1000) * 1000;
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
			return;
		}
		ResultSet resultSet = this.session.execute(this.findParentStatement.bind(sourceId));
		Row row = resultSet.one();
		if (row != null) {
			result.id = row.getUUID(0);
			result.transactionId = row.getUUID(1);
			result.transactionName = row.getString(2);
			result.creationTime = row.getDate(3);
		}
    }

	@Override
    public void findEndpointConfig(String endpoint, EndpointConfigResult result) {
		EndpointConfigResult cachedResult = this.endpointConfigs.get(endpoint);
		if (cachedResult == null || System.currentTimeMillis() - cachedResult.retrieved > 60000) {
			if (cachedResult == null) {
				cachedResult = new EndpointConfigResult();
			}
			cachedResult.correlationDataParsers.clear();
			// First check the global configuration
			ResultSet resultSet = this.session.execute(this.findEndpointConfigStatement.bind("*"));
			Row row = resultSet.one();
			if (row != null) {
				mergeRowIntoEndpointConfig(row, cachedResult);
			}
			resultSet = this.session.execute(this.findEndpointConfigStatement.bind(endpoint));
			row = resultSet.one();
			if (row != null) {
				mergeRowIntoEndpointConfig(row, cachedResult);
			}
			cachedResult.retrieved = System.currentTimeMillis();
			this.endpointConfigs.put(endpoint, cachedResult);
		} 
		result.applicationParsers = cachedResult.applicationParsers;
		result.eventNameParsers = cachedResult.eventNameParsers;
		result.correlationDataParsers = cachedResult.correlationDataParsers;
		result.eventDirection = cachedResult.eventDirection;
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
                	// TODO logging
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
                	// TODO logging
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
                	// TODO logging
                	new FixedValueExpressionParser(null);										
				}
			}
		}
		return new FixedValueExpressionParser(expression);
	}
}
