package com.holster.etm;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.InputSource;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.lmax.disruptor.EventHandler;

public class EnhancingEventHandler implements EventHandler<TelemetryEvent> {

	private static final String STATEMENT_FIND_PARENT_TELEMETRTY_EVENT = "findParentTelementryEvent";
	private static final String STATEMENT_FIND_ENDPOINT_CONFIG = "findEndpointConfig";
	
	private final Session session;
	private final long ordinal;
	private final long numberOfConsumers;
	private final PreparedStatement findParentStatement;
	private final PreparedStatement findEndpointConfigStatement;
	private final XPath xPath;
	
	public static final Map<String,PreparedStatement> createPreparedStatements(Session session) {
		final String keySpace = "etm";
		Map<String, PreparedStatement> statements = new HashMap<String, PreparedStatement>();
		
		PreparedStatement statement = session.prepare("select id, transactionId, transactionName from " + keySpace + ".sourceid_id_correlation where sourceId = ?;");
		statement.setRetryPolicy(DefaultRetryPolicy.INSTANCE);
		statements.put(STATEMENT_FIND_PARENT_TELEMETRTY_EVENT, statement);

		statement = session.prepare("select application, eventNameParsers from " + keySpace + ".endpoint_config where endpoint = ?;");
		statement.setRetryPolicy(DefaultRetryPolicy.INSTANCE);
		statements.put(STATEMENT_FIND_ENDPOINT_CONFIG, statement);

		return statements;
	}
	
	public EnhancingEventHandler(final Session session, final Map<String,PreparedStatement> statements, final long ordinal, final long numberOfConsumers) {
		this.session = session;
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
		this.findParentStatement = statements.get(STATEMENT_FIND_PARENT_TELEMETRTY_EVENT);
		this.findEndpointConfigStatement = statements.get(STATEMENT_FIND_ENDPOINT_CONFIG);
		XPathFactory xpf = XPathFactory.newInstance();
		this.xPath = xpf.newXPath();
	}

	@Override
	public void onEvent(final TelemetryEvent event, final long sequence, final boolean endOfBatch) throws Exception {
		if (event.ignore || (sequence % this.numberOfConsumers) != this.ordinal) {
			return;
		}
		if (event.eventTime.getTime() == 0) {
			event.eventTime.setTime(System.currentTimeMillis());
		}
		if (event.sourceCorrelationId != null && (event.correlationId == null || event.transactionId == null || event.transactionName == null)) {
			ResultSet resultSet = this.session.execute(this.findParentStatement.bind(event.sourceCorrelationId));
			Row row = resultSet.one();
			if (row != null) {
				if (event.correlationId == null) {
					event.correlationId = row.getUUID(0);
				}
				if (event.transactionId == null) {
					event.transactionId = row.getUUID(1);
				}
				if (event.transactionName == null) {
					event.transactionName = row.getString(2);
				}
			}
		}
		if (event.endpoint != null && (event.application == null || event.eventName == null)) {
			ResultSet resultSet = this.session.execute(this.findEndpointConfigStatement.bind(event.endpoint));
			Row row = resultSet.one();
			if (row != null) {
				if (event.application == null) {
					event.application = row.getString(0);
				}
				if (event.eventName == null && event.content != null) {
					parseEventName(row.getList(1, String.class), event);
				}
			}
		}
	}

	private void parseEventName(List<String> expressions, TelemetryEvent event) {
		if (event.content == null) {
			return;
		}
		for (String expression : expressions) {
			if (expression.length() > 6) {
				if (expression.charAt(0) == 'x' &&
					expression.charAt(1) == 'p' &&
					expression.charAt(2) == 'a' &&
					expression.charAt(3) == 't' &&
					expression.charAt(4) == 'h' &&
					expression.charAt(5) == ':') {
					event.eventName = evaluateXpath(expression.substring(6), event.content);
					if (event.eventName != null) {
						return;
					}
				}
			}
			
		}
    }

	private String evaluateXpath(String expression, String content) {
		try (StringReader reader = new StringReader(content)) {
			return this.xPath.evaluate(expression, new InputSource(reader));
		} catch (XPathExpressionException e) {
		}
		return null;
    }
}
