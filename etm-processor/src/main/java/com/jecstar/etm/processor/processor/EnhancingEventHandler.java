package com.jecstar.etm.processor.processor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.domain.EndpointHandler;
import com.jecstar.etm.core.domain.PayloadFormat;
import com.jecstar.etm.core.domain.TelemetryEvent;
import com.jecstar.etm.core.parsers.ExpressionParser;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.repository.EndpointConfigResult;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;
import com.lmax.disruptor.EventHandler;

public class EnhancingEventHandler implements EventHandler<TelemetryCommand> {

	
	private final long ordinal;
	private final long numberOfConsumers;
	
	private final TelemetryEventRepository telemetryEventRepository;
	private final EndpointConfigResult endpointConfigResult;
	private final Timer timer;
	
	public EnhancingEventHandler(final TelemetryEventRepository telemetryEventRepository, final long ordinal, final long numberOfConsumers, final MetricRegistry metricRegistry) {
		this.telemetryEventRepository = telemetryEventRepository;
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
		this.endpointConfigResult = new EndpointConfigResult();
		this.timer = metricRegistry.timer("event-processor-enhancing");
	}

	@Override
	public void onEvent(final TelemetryCommand command, final long sequence, final boolean endOfBatch) throws Exception {
		if (sequence % this.numberOfConsumers != this.ordinal) {
			return;
		}
		switch (command.commandType) {
		case EVENT:
			enhanceTelemetryEvent(command.event);
			break;
		default:
			break;
		}
	}
	

	/**
	 * TODO describe why it's a good practice to always set the writing application handling time.
	 * @param event
	 */
	private void enhanceTelemetryEvent(TelemetryEvent event) {
		final Context timerContext = this.timer.time();
		try {
			final ZonedDateTime now = ZonedDateTime.now();
			if (event.id == null) {
				// Assign a random id in case no id is present.
				event.id = UUID.randomUUID().toString();
			}
			if (event.payloadFormat == null) {
				// Always set the payload format, because it's a type in elastic.
				event.payloadFormat = detectPayloadFormat(event.payload);
			}
			this.endpointConfigResult.initialize();
			this.telemetryEventRepository.findEndpointConfig(event.endpoint, this.endpointConfigResult);
			if (event.name == null) {
				// Set the event name by the event name parsers.
				event.name = parseValue(this.endpointConfigResult.eventNameParsers, event.payload);
			}
			if (event.writingEndpointHandler.application.name == null) {
				// Set the writing application name by the writing application parsers.
				event.writingEndpointHandler.application.name = parseValue(this.endpointConfigResult.writingApplicationParsers, event.payload);
				if (event.writingEndpointHandler.application.name != null && event.writingEndpointHandler.handlingTime == null) {
					// If the name is set, and the handlingtime is not set, set it to the system date.
					event.writingEndpointHandler.handlingTime = now;
				}
			}
			if (event.readingEndpointHandlers.size() == 0) {
				// Check if we can find some reading applications with the reading application parsers.
				String readingApplicationName = parseValue(this.endpointConfigResult.readingApplicationParsers, event.payload);
				if (readingApplicationName != null) {
					EndpointHandler endpointHandler = new EndpointHandler();
					endpointHandler.application.name = readingApplicationName;
					event.readingEndpointHandlers.add(endpointHandler);
				}
			}
			for (EndpointHandler endpointHandler : event.readingEndpointHandlers) {
				// Add the handling time to all reading endpoint handlers without a handling time.
				if (endpointHandler.handlingTime == null) {
					endpointHandler.handlingTime = event.writingEndpointHandler.handlingTime != null ? event.writingEndpointHandler.handlingTime : now;
				}
			}
			// Check if dont't have any reading or writing application set. If
			// so, set the time one de writing application anyway, because
			// elastic needs at least 1 event time to determine the index.
			if (event.writingEndpointHandler.handlingTime == null && event.readingEndpointHandlers.size() == 0) {
				event.writingEndpointHandler.handlingTime = now;
			}
			if (!this.endpointConfigResult.correlationDataParsers.isEmpty()) {
				// Apply the correlation parsers.
				this.endpointConfigResult.correlationDataParsers.forEach((k,v) -> {
					String parsedValue = parseValue(v, event.payload);
					if (parsedValue != null) {
						event.correlationData.put(k, parsedValue);
					}
				});
			}
			if (!this.endpointConfigResult.extractionDataParsers.isEmpty()) {
				// Apply the data extraction parsers.
				this.endpointConfigResult.extractionDataParsers.forEach((k,v) -> {
					String parsedValue = parseValue(v, event.payload);
					if (parsedValue != null) {
						event.extractedData.put(k, parsedValue);
					}
				});
			}
		} finally {
			timerContext.stop();
		}	    
    }

	/**
	 * Super simple payload format detector. This isn't an enhanced detection
	 * algorithm because we won't be losing to much performance here. The end
	 * user should be able to tell the system which format it is anyway.
	 * 
	 * @param payload The payload.
	 * @return The detected <code>PayloadFormat</code>.
	 */
	private PayloadFormat detectPayloadFormat(String payload) {
		if (payload == null) {
			return PayloadFormat.TEXT;
		}
		String trimmed = payload.toLowerCase().trim();
		if (trimmed.indexOf("<soap:envelope ") != -1) {
			return PayloadFormat.SOAP;
		} else if (trimmed.indexOf("<!doctype html") != -1) {
			return PayloadFormat.HTML;
		} else if (trimmed.startsWith("<?xml ")) {
			return PayloadFormat.XML;
		} else if (trimmed.startsWith("{") && payload.endsWith("}")) {
			return PayloadFormat.JSON;
		} else if (trimmed.startsWith("select")
				   || trimmed.startsWith("insert")
				   || trimmed.startsWith("update")
				   || trimmed.startsWith("delete")
				   || trimmed.startsWith("drop")
				   || trimmed.startsWith("create")) {
			return PayloadFormat.SQL;
		}
		return PayloadFormat.TEXT;
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
