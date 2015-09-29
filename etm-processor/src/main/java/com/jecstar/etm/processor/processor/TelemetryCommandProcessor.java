package com.jecstar.etm.processor.processor;

import java.util.concurrent.ExecutorService;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.configuration.ConfigurationChangeListener;
import com.jecstar.etm.core.configuration.ConfigurationChangedEvent;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.processor.TelemetryCommand;
import com.lmax.disruptor.RingBuffer;

public class TelemetryCommandProcessor implements ConfigurationChangeListener {
	
	private RingBuffer<TelemetryCommand> ringBuffer;
	private boolean started = false;
	
	private ExecutorService executorService;
	private EtmConfiguration etmConfiguration;
	private DisruptorEnvironment disruptorEnvironment;
	private PersistenceEnvironment persistenceEnvironment;
	private MetricRegistry metricRegistry;
	private Timer offerTimer;
	

	public void start(final ExecutorService executorService, final PersistenceEnvironment persistenceEnvironment, final EtmConfiguration etmConfiguration, final MetricRegistry metricRegistry) {
		if (this.started) {
			throw new IllegalStateException();
		}
		this.started = true;
		this.executorService = executorService;
		this.persistenceEnvironment = persistenceEnvironment;
		this.etmConfiguration = etmConfiguration;
		this.metricRegistry = metricRegistry;
		this.offerTimer = this.metricRegistry.timer("event-processor-offering");
		this.disruptorEnvironment = new DisruptorEnvironment(etmConfiguration, executorService, this.persistenceEnvironment, this.metricRegistry);
		this.ringBuffer = this.disruptorEnvironment.start();
	}
	
	public void hotRestart() {
		if (!this.started) {
			throw new IllegalStateException();
		}
		DisruptorEnvironment newDisruptorEnvironment = new DisruptorEnvironment(this.etmConfiguration, this.executorService, this.persistenceEnvironment, this.metricRegistry);
		RingBuffer<TelemetryCommand> newRingBuffer = newDisruptorEnvironment.start();
		DisruptorEnvironment oldDisruptorEnvironment = this.disruptorEnvironment;
		
		this.ringBuffer = newRingBuffer;
		this.disruptorEnvironment = newDisruptorEnvironment;
		oldDisruptorEnvironment.shutdown();
	}
	
	public void stop() {
		if (!this.started) {
			throw new IllegalStateException();
		}
		this.disruptorEnvironment.shutdown();
	}
	
	public void stopAll() {
		if (!this.started) {
			throw new IllegalStateException();
		}		
		this.executorService.shutdown();
		this.disruptorEnvironment.shutdown();
		this.persistenceEnvironment.close();
	}


	public void processTelemetryCommand(final TelemetryCommand telemetryCommand) {
		if (!this.started) {
			throw new IllegalStateException();
		}
		// TODO check on license.
//		if (this.etmConfiguration.getLicenseExpriy().getTime() < System.currentTimeMillis()) {
//			throw new EtmException(EtmException.LICENSE_EXPIRED_EXCEPTION);
//		}
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
	
	
	public MetricRegistry getMetricRegistry() {
	    return this.metricRegistry;
    }
	
	private void preProcess(TelemetryCommand command) {
	}

	@Override
	public void configurationChanged(ConfigurationChangedEvent event) {
		if (this.started && event.isAnyChanged(EtmConfiguration.CONFIG_KEY_ENHANCING_HANDLER_COUNT,
				EtmConfiguration.CONFIG_KEY_PERSISTING_HANDLER_COUNT,
				EtmConfiguration.CONFIG_KEY_PERSISTING_BULK_SIZE)) {
			// Configuration changed in such a way that the DisruptorEnvironment needs to be recreated/restarted.
			try {
				hotRestart();
			} catch (IllegalStateException e) {
				
			}
		}
	}
}
