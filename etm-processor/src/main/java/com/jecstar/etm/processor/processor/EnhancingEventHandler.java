package com.jecstar.etm.processor.processor;

import java.time.ZonedDateTime;
import java.util.List;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.domain.EndpointConfiguration;
import com.jecstar.etm.core.domain.HttpTelemetryEvent;
import com.jecstar.etm.core.domain.LogTelemetryEvent;
import com.jecstar.etm.core.domain.MessagingTelemetryEvent;
import com.jecstar.etm.core.domain.SqlTelemetryEvent;
import com.jecstar.etm.core.enhancers.DefaultHttpTelemetryEventEnhancer;
import com.jecstar.etm.core.enhancers.DefaultLogTelemetryEventEnhancer;
import com.jecstar.etm.core.enhancers.DefaultMessagingTelemetryEventEnhancer;
import com.jecstar.etm.core.enhancers.DefaultSqlTelemetryEventEnhancer;
import com.jecstar.etm.core.enhancers.HttpTelemetryEventEnhancer;
import com.jecstar.etm.core.enhancers.LogTelemetryEventEnhancer;
import com.jecstar.etm.core.enhancers.MessagingTelemetryEventEnhancer;
import com.jecstar.etm.core.enhancers.SqlTelemetryEventEnhancer;
import com.jecstar.etm.core.parsers.ExpressionParser;
import com.jecstar.etm.processor.TelemetryCommand;
import com.lmax.disruptor.EventHandler;

public class EnhancingEventHandler implements EventHandler<TelemetryCommand> {

	
	private final long ordinal;
	private final long numberOfConsumers;
	private final CommandResources commandResources;
	
	private final EndpointConfiguration endpointConfiguration;
	private final LogTelemetryEventEnhancer defaultLogTelemetryEventEnhancer = new DefaultLogTelemetryEventEnhancer();
	private final HttpTelemetryEventEnhancer defaultHttpTelemetryEventEnhancer = new DefaultHttpTelemetryEventEnhancer();
	private final MessagingTelemetryEventEnhancer defaultMessagingTelemetryEventEnhancer = new DefaultMessagingTelemetryEventEnhancer();
	private final SqlTelemetryEventEnhancer defaultSqlTelemetryEventEnhancer = new DefaultSqlTelemetryEventEnhancer();
	private final Timer timer;
	
	public EnhancingEventHandler(final long ordinal, final long numberOfConsumers, final CommandResources commandResources, final MetricRegistry metricRegistry) {
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
		this.commandResources = commandResources;
		this.endpointConfiguration = new EndpointConfiguration();
		this.timer = metricRegistry.timer("event-processor-enhancing");
	}

	@Override
	public void onEvent(final TelemetryCommand command, final long sequence, final boolean endOfBatch) throws Exception {
		if (sequence % this.numberOfConsumers != this.ordinal) {
			return;
		}
		switch (command.commandType) {
		case HTTP_EVENT:
			enhanceHttpTelemetryEvent(command.httpTelemetryEvent);
			break;
		case LOG_EVENT:
			enhanceLogTelemetryEvent(command.logTelemetryEvent);
			break;
		case MESSAGING_EVENT:
			enhanceMessagingTelemetryEvent(command.messagingTelemetryEvent);
			break;
		case SQL_EVENT:
			enhanceSqlTelemetryEvent(command.sqlTelemetryEvent);
			break;
		default:
			throw new IllegalArgumentException("'" + command.commandType.name() + "' not implemented.");
		}
	}

	private void enhanceHttpTelemetryEvent(HttpTelemetryEvent event) {
		final Context timerContext = this.timer.time();
		try {
			final ZonedDateTime now = ZonedDateTime.now();
			this.commandResources.loadEndpointConfig(event.endpoint, this.endpointConfiguration);
			if (this.endpointConfiguration.httpEventEnhancers != null) {
				this.endpointConfiguration.httpEventEnhancers.forEach(c -> c.enhance(event, now));
			} else {
				this.defaultHttpTelemetryEventEnhancer.enhance(event, now);
			}
		} finally {
			timerContext.stop();
		}	    
	}
	
	private void enhanceLogTelemetryEvent(LogTelemetryEvent event) {
		final Context timerContext = this.timer.time();
		try {
			final ZonedDateTime now = ZonedDateTime.now();
			this.commandResources.loadEndpointConfig(event.endpoint, this.endpointConfiguration);
			if (this.endpointConfiguration.logEventEnhancers != null) {
				this.endpointConfiguration.logEventEnhancers.forEach(c -> c.enhance(event, now));
			} else {
				this.defaultLogTelemetryEventEnhancer.enhance(event, now);
			}
		} finally {
			timerContext.stop();
		}	    
	}

	private void enhanceMessagingTelemetryEvent(MessagingTelemetryEvent event) {
		final Context timerContext = this.timer.time();
		try {
			final ZonedDateTime now = ZonedDateTime.now();
			this.commandResources.loadEndpointConfig(event.endpoint, this.endpointConfiguration);
			if (this.endpointConfiguration.messagingEventEnhancers != null) {
				this.endpointConfiguration.messagingEventEnhancers.forEach(c -> c.enhance(event, now));
			} else {
				this.defaultMessagingTelemetryEventEnhancer.enhance(event, now);
			}
		} finally {
			timerContext.stop();
		}	    
    }

	private void enhanceSqlTelemetryEvent(SqlTelemetryEvent event) {
		final Context timerContext = this.timer.time();
		try {
			final ZonedDateTime now = ZonedDateTime.now();
			this.commandResources.loadEndpointConfig(event.endpoint, this.endpointConfiguration);
			if (this.endpointConfiguration.sqlEventEnhancers != null) {
				this.endpointConfiguration.sqlEventEnhancers.forEach(c -> c.enhance(event, now));
			} else {
				this.defaultSqlTelemetryEventEnhancer.enhance(event, now);
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
