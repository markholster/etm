package com.jecstar.etm.processor.processor;

import java.time.ZonedDateTime;
import java.util.List;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.LogTelemetryEvent;
import com.jecstar.etm.domain.MessagingTelemetryEvent;
import com.jecstar.etm.domain.SqlTelemetryEvent;
import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.server.core.domain.EndpointConfiguration;
import com.jecstar.etm.server.core.enhancers.DefaultTelemetryEventEnhancer;
import com.jecstar.etm.server.core.parsers.ExpressionParser;
import com.lmax.disruptor.EventHandler;

public class EnhancingEventHandler implements EventHandler<TelemetryCommand> {

	
	private final long ordinal;
	private final long numberOfConsumers;
	private final CommandResources commandResources;
	
	private final EndpointConfiguration endpointConfiguration;
	
	private final DefaultTelemetryEventEnhancer defaultTelemetryEventEnhancer = new DefaultTelemetryEventEnhancer();
	private final Timer timer;
	
	public EnhancingEventHandler(final long ordinal, final long numberOfConsumers, final CommandResources commandResources, final MetricRegistry metricRegistry) {
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
		this.commandResources = commandResources;
		this.endpointConfiguration = new EndpointConfiguration();
		this.timer = metricRegistry.timer("event-processor.enhancing");
	}

	@Override
	public void onEvent(final TelemetryCommand command, final long sequence, final boolean endOfBatch) throws Exception {
		if (sequence % this.numberOfConsumers != this.ordinal) {
			return;
		}
		switch (command.commandType) {
		case BUSINESS_EVENT:
			enhanceTelemetryEvent(command.businessTelemetryEvent);
			break;
		case HTTP_EVENT:
			enhanceTelemetryEvent(command.httpTelemetryEvent);
			break;
		case LOG_EVENT:
			enhanceTelemetryEvent(command.logTelemetryEvent);
			break;
		case MESSAGING_EVENT:
			enhanceTelemetryEvent(command.messagingTelemetryEvent);
			break;
		case SQL_EVENT:
			enhanceTelemetryEvent(command.sqlTelemetryEvent);
			break;
		default:
			throw new IllegalArgumentException("'" + command.commandType.name() + "' not implemented.");
		}
	}

	private void enhanceTelemetryEvent(TelemetryEvent<?> event) {
		final Context timerContext = this.timer.time();
		try {
			final ZonedDateTime now = ZonedDateTime.now();
			this.commandResources.loadEndpointConfig(event.endpoints, this.endpointConfiguration);
			if (this.endpointConfiguration.eventEnhancers != null && this.endpointConfiguration.eventEnhancers.size() > 0) {
				this.endpointConfiguration.eventEnhancers.forEach(c -> c.enhance(event, now));
			} else {
				this.defaultTelemetryEventEnhancer.enhance(event, now);
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
