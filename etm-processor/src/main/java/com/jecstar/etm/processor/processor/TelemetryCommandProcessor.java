package com.jecstar.etm.processor.processor;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.configuration.ConfigurationChangeListener;
import com.jecstar.etm.core.configuration.ConfigurationChangedEvent;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.DbQueryTelemetryEventBuilder;
import com.jecstar.etm.core.domain.HttpTelemetryEventBuilder;
import com.jecstar.etm.core.domain.LogTelemetryEventBuilder;
import com.jecstar.etm.core.domain.MessagingTelemetryEventBuilder;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.processor.TelemetryCommand;
import com.lmax.disruptor.RingBuffer;

public class TelemetryCommandProcessor implements ConfigurationChangeListener {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(TelemetryCommandProcessor.class);

	
	private RingBuffer<TelemetryCommand> ringBuffer;
	private boolean started = false;
	
	private ExecutorService executorService;
	private EtmConfiguration etmConfiguration;
	private DisruptorEnvironment disruptorEnvironment;
	private PersistenceEnvironment persistenceEnvironment;
	private MetricRegistry metricRegistry;
	private Timer offerTimer;
	private ScheduledReporter metricReporter;
	
	public TelemetryCommandProcessor() {
		this.metricRegistry = new MetricRegistry();
	}

	public void start(final ExecutorService executorService, final PersistenceEnvironment persistenceEnvironment, final EtmConfiguration etmConfiguration) {
		if (this.started) {
			throw new IllegalStateException();
		}
		this.started = true;
		this.executorService = executorService;
		this.persistenceEnvironment = persistenceEnvironment;
		this.etmConfiguration = etmConfiguration;
		this.metricReporter = this.persistenceEnvironment.createMetricReporter(etmConfiguration.getNodeName(), this.metricRegistry);
		this.metricReporter.start(1, TimeUnit.MINUTES);
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
		this.metricReporter.stop();
	}
	
	public void stopAll() {
		if (!this.started) {
			throw new IllegalStateException();
		}		
		this.executorService.shutdown();
		this.disruptorEnvironment.shutdown();
		this.metricReporter.stop();
		try {
			this.persistenceEnvironment.close();
		} catch (IOException e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Failed to close PersistenceEnvironment", e);
			}
		}
	}

	public void processDbQueryTelemetryEvent(final DbQueryTelemetryEventBuilder builder) {
		preProcess();
		final Context timerContext = this.offerTimer.time();
		TelemetryCommand target = null;
		long sequence = this.ringBuffer.next();
		try {
			target = this.ringBuffer.get(sequence);
			target.initialize(builder);
		} finally {
			this.ringBuffer.publish(sequence);
			timerContext.stop();
		}
	}
	
	public void processHttpTelemetryEvent(final HttpTelemetryEventBuilder builder) {
		preProcess();
		final Context timerContext = this.offerTimer.time();
		TelemetryCommand target = null;
		long sequence = this.ringBuffer.next();
		try {
			target = this.ringBuffer.get(sequence);
			target.initialize(builder);
		} finally {
			this.ringBuffer.publish(sequence);
			timerContext.stop();
		}
	}
	
	public void processLogTelemetryEvent(final LogTelemetryEventBuilder builder) {
		preProcess();
		final Context timerContext = this.offerTimer.time();
		TelemetryCommand target = null;
		long sequence = this.ringBuffer.next();
		try {
			target = this.ringBuffer.get(sequence);
			target.initialize(builder);
		} finally {
			this.ringBuffer.publish(sequence);
			timerContext.stop();
		}
	}
	
	public void processMessagingTelemetryEvent(final MessagingTelemetryEventBuilder builder) {
		preProcess();
		final Context timerContext = this.offerTimer.time();
		TelemetryCommand target = null;
		long sequence = this.ringBuffer.next();
		try {
			target = this.ringBuffer.get(sequence);
			target.initialize(builder);
		} finally {
			this.ringBuffer.publish(sequence);
			timerContext.stop();
		}
	}
	
	public MetricRegistry getMetricRegistry() {
	    return this.metricRegistry;
    }
	
	private void preProcess() {
		if (!this.started) {
			throw new IllegalStateException();
		}
		// TODO check on license.
//		if (this.etmConfiguration.getLicenseExpriy().getTime() < System.currentTimeMillis()) {
//			throw new EtmException(EtmException.LICENSE_EXPIRED_EXCEPTION);
//		}
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
