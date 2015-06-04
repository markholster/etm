package com.jecstar.etm.processor.processor;

import java.time.LocalDateTime;
import java.util.List;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.utils.UUIDs;
import com.jecstar.etm.core.EndpointHandler;
import com.jecstar.etm.core.TelemetryCommand;
import com.jecstar.etm.core.TelemetryMessageEvent;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.parsers.ExpressionParser;
import com.jecstar.etm.processor.repository.EndpointConfigResult;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;
import com.lmax.disruptor.EventHandler;

public class EnhancingEventHandler implements EventHandler<TelemetryCommand> {

	
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
	public void onEvent(final TelemetryCommand command, final long sequence, final boolean endOfBatch) throws Exception {
		if (sequence % this.numberOfConsumers != this.ordinal) {
			return;
		}
		switch (command.commandType) {
		case MESSAGE_EVENT:
			enhanceTelemetryMessageEvent(command.messageEvent);
			break;
		default:
			break;
		}
	}
	

	private void enhanceTelemetryMessageEvent(TelemetryMessageEvent event) {
		final Context timerContext = this.timer.time();
		try {
			final LocalDateTime now = LocalDateTime.now();
			if (event.id == null) {
				event.id = UUIDs.timeBased().toString();
			}
			this.endpointConfigResult.initialize();
			this.telemetryEventRepository.findEndpointConfig(event.endpoint, this.endpointConfigResult, this.etmConfiguration.getEndpointCacheExpiryTime());
			if (event.name == null) {
				event.name = parseValue(this.endpointConfigResult.eventNameParsers, event.content);
			}
			if (event.writingEndpointHandler.applicationName == null) {
				event.writingEndpointHandler.applicationName = parseValue(this.endpointConfigResult.writingApplicationParsers, event.content);
			}
			if (event.writingEndpointHandler.applicationName != null && event.writingEndpointHandler.handlingTime == null) {
				event.writingEndpointHandler.handlingTime = now;
			}
			
			if (event.readingEndpointHandlers.size() == 0) {
				String readingApplication = parseValue(this.endpointConfigResult.readingApplicationParsers, event.content);
				if (readingApplication != null) {
					EndpointHandler endpointHandler = new EndpointHandler();
					endpointHandler.applicationName = readingApplication;
					event.readingEndpointHandlers.add(endpointHandler);
				}
			}
			// If no reading endpointhandlers, and no writing endpointhandler,
			// at least set the time of the writingEndpointHandler to the
			// current system time. Otherwise no event time can be determined.
			if (event.writingEndpointHandler.applicationName == null && event.readingEndpointHandlers.size() == 0 && event.writingEndpointHandler.handlingTime == null) {
				event.writingEndpointHandler.handlingTime = now;
			}
			for (EndpointHandler endpointHandler : event.readingEndpointHandlers) {
				if (endpointHandler.handlingTime == null) {
					endpointHandler.handlingTime = event.writingEndpointHandler.handlingTime != null ? event.writingEndpointHandler.handlingTime : now;
				}
			}
			if (event.transactionName == null) {
				event.transactionName = parseValue(this.endpointConfigResult.transactionNameParsers, event.content);
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
