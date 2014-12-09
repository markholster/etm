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
import com.holster.etm.repository.CorrelationBySourceIdResult;
import com.holster.etm.repository.TelemetryEventRepository;
import com.lmax.disruptor.EventHandler;

public class EnhancingEventHandler implements EventHandler<TelemetryEvent> {

	
	private final long ordinal;
	private final long numberOfConsumers;
	
	private final XPath xPath;
	private final TelemetryEventRepository telemetryEventRepository;
	private final CorrelationBySourceIdResult correlationBySourceIdResult;
	
	public EnhancingEventHandler(final TelemetryEventRepository telemetryEventRepository, final long ordinal, final long numberOfConsumers) {
		this.telemetryEventRepository = telemetryEventRepository;
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
		XPathFactory xpf = XPathFactory.newInstance();
		this.xPath = xpf.newXPath();
		this.correlationBySourceIdResult = new CorrelationBySourceIdResult();
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
			this.telemetryEventRepository.findParent(event.sourceCorrelationId, this.correlationBySourceIdResult.initialize());
			if (event.correlationId == null) {
				event.correlationId = this.correlationBySourceIdResult.id;
			}
			if (event.transactionId == null) {
				event.transactionId = this.correlationBySourceIdResult.transactionId;
			}
			if (event.transactionName == null) {
				event.transactionName = this.correlationBySourceIdResult.transactionName;
			}
		}
//		if (event.endpoint != null && (event.application == null || event.eventName == null)) {
//			ResultSet resultSet = this.session.execute(this.findEndpointConfigStatement.bind(event.endpoint));
//			Row row = resultSet.one();
//			if (row != null) {
//				if (event.application == null) {
//					event.application = row.getString(0);
//				}
//				if (event.eventName == null && event.content != null) {
//					event.eventName = parseValue(row.getList(1, String.class), event.content); 
//				}
//			}
//		}
	}

	private String parseValue(List<String> expressions, String content) {
		if (content == null) {
			return null;
		}
		for (String expression : expressions) {
			if (expression.length() > 6) {
				if (expression.charAt(0) == 'v' &&
						expression.charAt(1) == 'a' &&
						expression.charAt(2) == 'l' &&
						expression.charAt(3) == 'u' &&
						expression.charAt(4) == 'e' &&
						expression.charAt(5) == ':') {
					return expression.substring(6);
				} else if (expression.charAt(0) == 'x' &&
					expression.charAt(1) == 'p' &&
					expression.charAt(2) == 'a' &&
					expression.charAt(3) == 't' &&
					expression.charAt(4) == 'h' &&
					expression.charAt(5) == ':') {
					String value = evaluateXpath(expression.substring(6), content);
					if (value != null) {
						return value;
					}
				} else if (expression.charAt(0) == 'f' &&
					expression.charAt(1) == 'i' &&
					expression.charAt(2) == 'x' &&
					expression.charAt(3) == 'e' &&
					expression.charAt(4) == 'd' &&
					expression.charAt(5) == ':') {
					// TODO implement an expression at a fixed position.
				}
			}
		}
		return null;
    }

	private String evaluateXpath(String expression, String content) {
		try (StringReader reader = new StringReader(content)) {
			return this.xPath.evaluate(expression, new InputSource(reader));
		} catch (XPathExpressionException e) {
		}
		return null;
    }
}
