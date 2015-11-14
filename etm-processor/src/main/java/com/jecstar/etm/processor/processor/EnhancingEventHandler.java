package com.jecstar.etm.processor.processor;

import java.util.List;
import java.util.UUID;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.parsers.ExpressionParser;
import com.jecstar.etm.processor.EventCommand;
import com.jecstar.etm.processor.TelemetryEvent;
import com.jecstar.etm.processor.repository.EndpointConfigResult;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;
import com.lmax.disruptor.EventHandler;

public class EnhancingEventHandler implements EventHandler<TelemetryEvent> {

	
	private final long ordinal;
	private final long numberOfConsumers;
	
	private final TelemetryEventRepository telemetryEventRepository;
	private final EndpointConfigResult endpointConfigResult;
	private final Timer timer;
	
	
	public EnhancingEventHandler(final TelemetryEventRepository telemetryEventRepository, final long ordinal, final long numberOfConsumers, final Timer timer) {
		this.telemetryEventRepository = telemetryEventRepository;
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
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
		if (event.id == null) {
			event.id = UUID.randomUUID().toString();
		}
		if (event.creationTime.getTime() == 0) {
			event.creationTime.setTime(System.currentTimeMillis());
		}
		try {
			this.endpointConfigResult.initialize();
			this.telemetryEventRepository.findEndpointConfig(event.endpoint, this.endpointConfigResult);
			// First determine the application name.
			if (event.application == null) {
				if (event.application == null) {
					event.application = parseValue(this.endpointConfigResult.applicationParsers, event.content);
				}			
			}
			if (event.name == null || event.direction == null || event.transactionName == null) {
				if (event.name == null && event.content != null) {
					event.name = parseValue(this.endpointConfigResult.eventNameParsers, event.content);
				}
				if (event.direction == null) {
					event.direction = this.endpointConfigResult.eventDirection;
				}
				if (event.transactionName == null) {
					event.transactionName = parseValue(this.endpointConfigResult.transactionNameParsers, event.content);
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
	
//	private boolean needsCorrelation(final TelemetryEvent event) {
//		return event.correlationId != null;
//	}

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
