package com.jecstar.etm.processor.processor;

import java.nio.channels.IllegalSelectorException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.elasticsearch.client.Client;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.TelemetryEventType;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.parsers.ExpressionParser;
import com.jecstar.etm.processor.EventCommand;
import com.jecstar.etm.processor.TelemetryEvent;
import com.jecstar.etm.processor.repository.EndpointConfigResult;
import com.lmax.disruptor.RingBuffer;

public class TelemetryEventProcessor {
	
	private RingBuffer<TelemetryEvent> ringBuffer;
	private boolean started = false;
	
	private ExecutorService executorService;
	private Client elasticClient;
	private EtmConfiguration etmConfiguration;
	private DisruptorEnvironment disruptorEnvironment;
	private PersistenceEnvironment persistenceEnvironment;
	private MetricRegistry metricRegistry;
	private Timer offerTimer;
	

	public void start(final ExecutorService executorService, final PersistenceEnvironment persistenceEnvironment, final Client elasticClient, final EtmConfiguration etmConfiguration, final MetricRegistry metricRegistry) {
		if (this.started) {
			throw new IllegalStateException();
		}
		this.started = true;
		this.executorService = executorService;
		this.persistenceEnvironment = persistenceEnvironment;
		this.elasticClient = elasticClient;
		this.etmConfiguration = etmConfiguration;
		this.metricRegistry = metricRegistry;
		this.offerTimer = this.metricRegistry.timer("event-offer");
		this.disruptorEnvironment = new DisruptorEnvironment(etmConfiguration, executorService, elasticClient, this.persistenceEnvironment, this.metricRegistry);
		this.ringBuffer = this.disruptorEnvironment.start();
	}
	
	public void hotRestart() {
		if (!this.started) {
			throw new IllegalStateException();
		}
		DisruptorEnvironment newDisruptorEnvironment = new DisruptorEnvironment(this.etmConfiguration, this.executorService, this.elasticClient, this.persistenceEnvironment, this.metricRegistry);
		RingBuffer<TelemetryEvent> newRingBuffer = newDisruptorEnvironment.start();
		DisruptorEnvironment oldDisruptorEnvironment = this.disruptorEnvironment;
		
		this.ringBuffer = newRingBuffer;
		this.disruptorEnvironment = newDisruptorEnvironment;
		oldDisruptorEnvironment.shutdown();
	}
	
	public void stop() {
		if (!this.started) {
			throw new IllegalSelectorException();
		}
		this.disruptorEnvironment.shutdown();
	}
	
	public void stopAll() {
		if (!this.started) {
			throw new IllegalSelectorException();
		}		
		this.executorService.shutdown();
		this.disruptorEnvironment.shutdown();
		this.persistenceEnvironment.close();
	}


	public void processTelemetryEvent(final TelemetryEvent telemetryEvent) {
		if (!this.started) {
			throw new IllegalSelectorException();
		}
		if (this.etmConfiguration.getLicense() == null || this.etmConfiguration.getLicense().getExpiryDate().getTime() < System.currentTimeMillis()) {
			throw new EtmException(EtmException.LICENSE_EXPIRED_EXCEPTION);
		}
		final Context timerContext = this.offerTimer.time();
		TelemetryEvent target = null;
		long sequence = this.ringBuffer.next();
		try {
			target = this.ringBuffer.get(sequence);
			target.initialize(telemetryEvent);
			preProcess(target);
		} finally {
			this.ringBuffer.publish(sequence);
			timerContext.stop();
		}
	}
	
	/**
	 * Flush the pending documents to Solr.
	 */
	public void requestDocumentsFlush() {
		long sequence = this.ringBuffer.next();
		try {
			TelemetryEvent target = this.ringBuffer.get(sequence);
			target.initialize();
			target.eventCommand = EventCommand.FLUSH_DOCUMENTS;
		} finally {
			this.ringBuffer.publish(sequence);
		}
	}
	
	public MetricRegistry getMetricRegistry() {
	    return this.metricRegistry;
    }
	
	private void preProcess(TelemetryEvent event) {
		if (event.id == null) {
			event.id = UUID.randomUUID().toString();
		}
		if (event.creationTime.getTime() == 0) {
			event.creationTime.setTime(System.currentTimeMillis());
		}
		EndpointConfigResult endpointConfig = null;
		if (event.retention.getTime() == 0) {
			event.retention.setTime(System.currentTimeMillis() + this.etmConfiguration.getDataRetentionTime());
		}
		if ((event.transactionName == null || event.name == null)&& TelemetryEventType.MESSAGE_REQUEST.equals(event.type)) {
			// A little bit of enhancement before the event is processed by the
			// disruptor. We need to make sure the eventName & transactionName
			// is determined because the correlation of the transaction data on
			// the response will fail if the request and response are processed
			// at exactly the same time. By determining the event name &
			// transaction name at this point, the only requirement is that the
			// request is offered to ETM before the response, which is quite
			// logical.
			endpointConfig = new EndpointConfigResult();
			this.disruptorEnvironment.findEndpointConfig(event.endpoint, endpointConfig);
			if (endpointConfig.eventNameParsers != null && endpointConfig.eventNameParsers.size() > 0) {
				event.name = parseValue(endpointConfig.eventNameParsers, event.content);
			}
			if (endpointConfig.transactionNameParsers != null && endpointConfig.transactionNameParsers.size() > 0) {
				event.transactionName = parseValue(endpointConfig.transactionNameParsers, event.content);
			}
		}
		if (event.transactionName != null) {
			event.transactionId = event.id;
		}
		if (event.id != null) {
			if (event.application == null) {
				if (endpointConfig == null) {
					endpointConfig = new EndpointConfigResult();
					this.disruptorEnvironment.findEndpointConfig(event.endpoint, endpointConfig);
				}
				event.application = parseValue(endpointConfig.applicationParsers, event.content);
			}
			this.persistenceEnvironment.getProcessingMap().addTelemetryEvent(event);
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
