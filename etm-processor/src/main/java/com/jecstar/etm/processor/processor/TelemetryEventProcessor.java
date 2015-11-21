package com.jecstar.etm.processor.processor;

import java.nio.channels.IllegalSelectorException;
import java.util.concurrent.ExecutorService;

import org.elasticsearch.client.Client;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.EventCommand;
import com.jecstar.etm.core.TelemetryEvent;
import com.jecstar.etm.core.configuration.EtmConfiguration;
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
	}
	
}
