package com.holster.etm;

import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import com.holster.etm.repository.CorrelationBySourceIdResult;
import com.holster.etm.repository.EndpointConfigResult;
import com.holster.etm.repository.TelemetryEventRepository;
import com.lmax.disruptor.EventHandler;

public class EnhancingEventHandler implements EventHandler<TelemetryEvent> {

	
	private final long ordinal;
	private final long numberOfConsumers;
	
	private final XPath xPath;
	private final TelemetryEventRepository telemetryEventRepository;
	private final CorrelationBySourceIdResult correlationBySourceIdResult;
	private final EndpointConfigResult endpointConfigResult;
	
	public EnhancingEventHandler(final TelemetryEventRepository telemetryEventRepository, final long ordinal, final long numberOfConsumers) {
		this.telemetryEventRepository = telemetryEventRepository;
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
		XPathFactory xpf = XPathFactory.newInstance();
		this.xPath = xpf.newXPath();
		this.correlationBySourceIdResult = new CorrelationBySourceIdResult();
		this.endpointConfigResult = new EndpointConfigResult();
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
		if (event.endpoint != null && (event.application == null || event.eventName == null)) {
			this.telemetryEventRepository.findEndpointConfig(event.endpoint, this.endpointConfigResult);
			if (event.application == null) {
				event.application = parseValue(this.endpointConfigResult.applicationParsers, event.content);
			}
			if (event.eventName == null && event.content != null) {
				event.eventName = parseValue(this.endpointConfigResult.eventNameParsers, event.content);
			}
		}
	}

	private String parseValue(List<ExpressionParser> expressionParsers, String content) {
		if (content == null || expressionParsers == null) {
			return null;
		}
		for (ExpressionParser expressionParser : expressionParsers) {
			String value = expressionParser.evaluate(content);
			if (value != null) {
				return value;
			}
		}
		return null;
    }
}
