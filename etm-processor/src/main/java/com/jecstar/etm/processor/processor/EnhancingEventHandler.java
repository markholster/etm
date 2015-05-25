package com.jecstar.etm.processor.processor;

import java.util.List;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.TelemetryEventType;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.parsers.ExpressionParser;
import com.jecstar.etm.processor.EventCommand;
import com.jecstar.etm.processor.TelemetryEvent;
import com.jecstar.etm.processor.repository.EndpointConfigResult;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;
import com.lmax.disruptor.EventHandler;

public class EnhancingEventHandler implements EventHandler<TelemetryEvent> {

	
	private final long ordinal;
	private final long numberOfConsumers;
	private final EtmConfiguration etmConfiguration;
	
	private final TelemetryEventRepository telemetryEventRepository;
	private final EndpointConfigResult endpointConfigResult;
	private final Timer timer;
	
	
	public EnhancingEventHandler(final TelemetryEventRepository telemetryEventRepository, final long ordinal, final long numberOfConsumers, final EtmConfiguration etmConfiguration, final Timer timer) {
		this.telemetryEventRepository = telemetryEventRepository;
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
		this.etmConfiguration = etmConfiguration;
		this.endpointConfigResult = new EndpointConfigResult();
		this.timer = timer;
		
	}

	@Override
	public void onEvent(final TelemetryEvent event, final long sequence, final boolean endOfBatch) throws Exception {
		if (event.ignore) {
			return;
		}
		if (!EventCommand.PROCESS.equals(event.eventCommand) || (sequence % this.numberOfConsumers) != this.ordinal) {
			return;
		}
		final Context timerContext = this.timer.time();
		try {
			this.endpointConfigResult.initialize();
			this.telemetryEventRepository.findEndpointConfig(event.endpoint, this.endpointConfigResult, this.etmConfiguration.getEndpointCacheExpiryTime());
			// First determine the application name.
			if (event.application == null) {
				if (event.application == null) {
					event.application = parseValue(this.endpointConfigResult.applicationParsers, event.content);
				}			
			}
			// TODO point-to-point messages which are added twice -> they have the same sourceId, but one is the parent of the other.
			if (needsCorrelation(event)) {
				// Find the correlation event.
				TelemetryEvent parent = this.telemetryEventRepository.findParent(event.sourceCorrelationId, event.application);
				if (parent == null) {
					System.out.println("Could not find " + event.sourceCorrelationId + "_" + event.application);
				}
				
				if (event.correlationId == null) {
					event.correlationId = parent.id;
				}
				// TODO the parent could be something other than the request in case of an point to point message. Find the event with specific type of request.
				if (TelemetryEventType.MESSAGE_RESPONSE.equals(event.type)) {
					// if this is a response, set the correlating data from the request on the response.
					if (event.transactionId == null) {
						event.transactionId = parent.transactionId;
					}
					if (event.transactionName == null) {
						event.transactionName = parent.transactionName;
					}
					if (event.correlationCreationTime.getTime() == 0) {
						event.correlationCreationTime.setTime(parent.creationTime.getTime());
					}
					if (event.correlationExpiryTime.getTime() == 0) {
						event.correlationExpiryTime.setTime(parent.expiryTime.getTime());
					}
					if (event.correlationName == null) {
						event.correlationName = parent.name;
					}
					if (event.slaRule == null) {
						event.slaRule = parent.slaRule;
					}
				}
			}
			if (event.name == null || event.direction == null || event.transactionName == null || event.slaRule == null) {
				if (event.name == null && event.content != null) {
					event.name = parseValue(this.endpointConfigResult.eventNameParsers, event.content);
				}
				if (event.direction == null) {
					event.direction = this.endpointConfigResult.eventDirection;
				}
				if (event.transactionName == null) {
					event.transactionName = parseValue(this.endpointConfigResult.transactionNameParsers, event.content);
					if (event.transactionName != null) {
						event.transactionId = event.id;
					}
				}
				if (event.slaRule == null && event.transactionName != null) {
					event.slaRule = this.endpointConfigResult.slaRules.get(event.transactionName);
				}
	 		}
			if (!this.endpointConfigResult.correlationDataParsers.isEmpty()) {
				this.endpointConfigResult.correlationDataParsers.forEach((k,v) -> {
					String parsedValue = parseValue(v, event.content);
					if (parsedValue != null) {
						event.correlationData.put(k, parsedValue);
					}
				});
			}
		} finally {
			timerContext.stop();
		}
	}
	
	private boolean needsCorrelation(final TelemetryEvent event) {
		if (event.sourceCorrelationId != null && event.correlationId == null) {
			return true;
		} else if (event.sourceCorrelationId != null
		        && ((event.transactionId == null || event.transactionName == null || event.correlationCreationTime.getTime() == 0 || event.correlationExpiryTime
		                .getTime() == 0) && TelemetryEventType.MESSAGE_RESPONSE.equals(event.type))) {
			return true;
		}
		return false;
	}

	private String parseValue(List<ExpressionParser> expressionParsers, String content) {
		if (content == null || expressionParsers == null) {
			return null;
		}
		for (ExpressionParser expressionParser : expressionParsers) {
			String value = parseValue(expressionParser, content);
			if (value != null) {
				return value;
			}
		}
		return null;
    }
	
	private String parseValue(ExpressionParser expressionParser, String content) {
		if (expressionParser == null || content == null) {
			return null;
		}
		String value = expressionParser.evaluate(content);
		if (value != null && value.trim().length() > 0) {
			return value;
		}
		return null;
	}
}
