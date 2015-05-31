package com.jecstar.etm.processor.processor;

import java.nio.channels.IllegalSelectorException;
import java.util.concurrent.ExecutorService;

import org.apache.solr.client.solrj.SolrClient;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.utils.UUIDs;
import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.TelemetryCommand;
import com.jecstar.etm.core.TelemetryCommand.CommandType;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.lmax.disruptor.RingBuffer;

public class TelemetryCommandProcessor {
	
	private RingBuffer<TelemetryCommand> ringBuffer;
	private boolean started = false;
	
	private ExecutorService executorService;
	private SolrClient solrClient;
	private EtmConfiguration etmConfiguration;
	private DisruptorEnvironment disruptorEnvironment;
	private PersistenceEnvironment persistenceEnvironment;
	private MetricRegistry metricRegistry;
	private Timer offerTimer;
	

	public void start(final ExecutorService executorService, final PersistenceEnvironment persistenceEnvironment, final SolrClient solrClient, final EtmConfiguration etmConfiguration, final MetricRegistry metricRegistry) {
		if (this.started) {
			throw new IllegalStateException();
		}
		this.started = true;
		this.executorService = executorService;
		this.persistenceEnvironment = persistenceEnvironment;
		this.solrClient = solrClient;
		this.etmConfiguration = etmConfiguration;
		this.metricRegistry = metricRegistry;
		this.offerTimer = this.metricRegistry.timer("event-offering");
		this.disruptorEnvironment = new DisruptorEnvironment(etmConfiguration, executorService, solrClient, this.persistenceEnvironment, this.metricRegistry);
		this.ringBuffer = this.disruptorEnvironment.start();
	}
	
	public void hotRestart() {
		if (!this.started) {
			throw new IllegalStateException();
		}
		DisruptorEnvironment newDisruptorEnvironment = new DisruptorEnvironment(this.etmConfiguration, this.executorService, this.solrClient, this.persistenceEnvironment, this.metricRegistry);
		RingBuffer<TelemetryCommand> newRingBuffer = newDisruptorEnvironment.start();
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
		this.solrClient.shutdown();
	}


	public void processTelemetryEvent(final TelemetryCommand telemetryCommand) {
		if (!this.started) {
			throw new IllegalSelectorException();
		}
		if (this.etmConfiguration.getLicenseExpriy().getTime() < System.currentTimeMillis()) {
			throw new EtmException(EtmException.LICENSE_EXPIRED_EXCEPTION);
		}
		final Context timerContext = this.offerTimer.time();
		TelemetryCommand target = null;
		long sequence = this.ringBuffer.next();
		try {
			target = this.ringBuffer.get(sequence);
			target.initialize(telemetryCommand);
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
			TelemetryCommand target = this.ringBuffer.get(sequence);
			target.initialize();
			target.commandType = CommandType.FLUSH_DOCUMENTS;
			preProcess(target);
		} finally {
			this.ringBuffer.publish(sequence);
		}
	}
	
	public MetricRegistry getMetricRegistry() {
	    return this.metricRegistry;
    }
	
	private void preProcess(TelemetryCommand command) {
		if (CommandType.MESSAGE_EVENT.equals(command.commandType)) {
//			if (command.messageEvent.id == null) {
//				command.messageEvent.id = UUIDs.timeBased().toString();
//			}
//			this.persistenceEnvironment.getProcessingMap().addTelemetryEvent(command.messageEvent);
		}
	}
}
