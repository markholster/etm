package com.jecstar.etm.processor.processor;

import java.io.IOException;
import java.util.concurrent.ThreadFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.LogTelemetryEvent;
import com.jecstar.etm.domain.MessagingTelemetryEvent;
import com.jecstar.etm.domain.SqlTelemetryEvent;
import com.jecstar.etm.domain.builders.BusinessTelemetryEventBuilder;
import com.jecstar.etm.domain.builders.HttpTelemetryEventBuilder;
import com.jecstar.etm.domain.builders.LogTelemetryEventBuilder;
import com.jecstar.etm.domain.builders.MessagingTelemetryEventBuilder;
import com.jecstar.etm.domain.builders.SqlTelemetryEventBuilder;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.metrics.GarbageCollectorMetricSet;
import com.jecstar.etm.processor.metrics.MemoryUsageMetricSet;
import com.jecstar.etm.processor.metrics.OperatingSystemMetricSet;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.configuration.ConfigurationChangeListener;
import com.jecstar.etm.server.core.configuration.ConfigurationChangedEvent;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.lmax.disruptor.RingBuffer;

public class TelemetryCommandProcessor implements ConfigurationChangeListener {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(TelemetryCommandProcessor.class);
	
	private RingBuffer<TelemetryCommand> ringBuffer;
	private boolean started = false;
	
	private ThreadFactory threadFactory;
	private EtmConfiguration etmConfiguration;
	private DisruptorEnvironment disruptorEnvironment;
	private PersistenceEnvironment persistenceEnvironment;
	private MetricRegistry metricRegistry;
	private Timer offerTimer;
	
	public TelemetryCommandProcessor(MetricRegistry metricRegistry) {
		this.metricRegistry = metricRegistry;
	}

	public void start(final ThreadFactory threadFactory, final PersistenceEnvironment persistenceEnvironment, final EtmConfiguration etmConfiguration) {
		if (this.started) {
			throw new IllegalStateException();
		}
		this.started = true;
		this.threadFactory = threadFactory;
		this.persistenceEnvironment = persistenceEnvironment;
		this.etmConfiguration = etmConfiguration;
		this.offerTimer = this.metricRegistry.timer("event-processor.offering");
		this.disruptorEnvironment = new DisruptorEnvironment(etmConfiguration, this.threadFactory, this.persistenceEnvironment, this.metricRegistry);
		this.ringBuffer = this.disruptorEnvironment.start();
		this.metricRegistry.register("event-processor.ringbuffer-capacity", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return TelemetryCommandProcessor.this.ringBuffer.remainingCapacity();
			}});
		this.metricRegistry.registerAll(new GarbageCollectorMetricSet());
		this.metricRegistry.registerAll(new MemoryUsageMetricSet());
		this.metricRegistry.registerAll(new OperatingSystemMetricSet());
	}
	
	public void hotRestart() {
		if (!this.started) {
			throw new IllegalStateException();
		}
		if (log.isDebugLevelEnabled()) {
			log.logDebugMessage("Executing hot restart of TelemetryCommandProcessor.");
		}
		DisruptorEnvironment newDisruptorEnvironment = new DisruptorEnvironment(this.etmConfiguration, this.threadFactory, this.persistenceEnvironment, this.metricRegistry);
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
		this.disruptorEnvironment.shutdown();
		try {
			this.persistenceEnvironment.close();
		} catch (IOException e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Failed to close PersistenceEnvironment", e);
			}
		}
	}

	public void processTelemetryEvent(final SqlTelemetryEventBuilder builder) {
		processTelemetryEvent(builder.build());
	}
	
	public void processTelemetryEvent(final SqlTelemetryEvent event) {
		preProcess();
		final Context timerContext = this.offerTimer.time();
		TelemetryCommand target = null;
		long sequence = this.ringBuffer.next();
		try {
			target = this.ringBuffer.get(sequence);
			target.initialize(event);
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Processing sql event with id '" + event.id + "'.");
			}
		} catch (Exception e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Failed to initialize sql event with id '" + event.id + "'.", e);
			}		
			target.initializeToNoop();
		} finally {
			this.ringBuffer.publish(sequence);
			timerContext.stop();
		}
	}
	
	public void processTelemetryEvent(final HttpTelemetryEventBuilder builder) {
		processTelemetryEvent(builder.build());
	}
	
	public void processTelemetryEvent(final HttpTelemetryEvent event) {
		preProcess();
		final Context timerContext = this.offerTimer.time();
		TelemetryCommand target = null;
		long sequence = this.ringBuffer.next();
		try {
			target = this.ringBuffer.get(sequence);
			target.initialize(event);
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Processing http event with id '" + event.id + "'.");
			}
		} catch (Exception e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Failed to initialize http event with id '" + event.id + "'.", e);
			}			
			target.initializeToNoop();
		} finally {
			this.ringBuffer.publish(sequence);
			timerContext.stop();
		}
	}
	
	public void processTelemetryEvent(final LogTelemetryEventBuilder builder) {
		processTelemetryEvent(builder.build());
	}
	
	public void processTelemetryEvent(final LogTelemetryEvent event) {
		preProcess();
		final Context timerContext = this.offerTimer.time();
		TelemetryCommand target = null;
		long sequence = this.ringBuffer.next();
		try {
			target = this.ringBuffer.get(sequence);
			target.initialize(event);
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Processing log event with id '" + event.id + "'.");
			}
		} catch (Exception e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Failed to initialize log event with id '" + event.id + "'.", e);
			}		
			target.initializeToNoop();
		} finally {
			this.ringBuffer.publish(sequence);
			timerContext.stop();
		}
	}
	
	public void processTelemetryEvent(final MessagingTelemetryEventBuilder builder) {
		processTelemetryEvent(builder.build());
	}
	
	public void processTelemetryEvent(final MessagingTelemetryEvent event) {
		preProcess();
		final Context timerContext = this.offerTimer.time();
		TelemetryCommand target = null;
		long sequence = this.ringBuffer.next();
		try {
			target = this.ringBuffer.get(sequence);
			target.initialize(event);
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Processing messaging event with id '" + event.id + "'.");
			}		
		} catch (Exception e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Failed to initialize messaging event with id '" + event.id + "'.", e);
			}	
			target.initializeToNoop();
		} finally {
			this.ringBuffer.publish(sequence);
			timerContext.stop();
		}
	}
	
	public void processTelemetryEvent(final BusinessTelemetryEventBuilder builder) {
		processTelemetryEvent(builder.build());
	}
	
	public void processTelemetryEvent(final BusinessTelemetryEvent event) {
		preProcess();
		final Context timerContext = this.offerTimer.time();
		TelemetryCommand target = null;
		long sequence = this.ringBuffer.next();
		try {
			target = this.ringBuffer.get(sequence);
			target.initialize(event);
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Processing business event with id '" + event.id + "'.");
			}			
		} catch (Exception e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Failed to initialize business event with id '" + event.id + "'.", e);
			}
			target.initializeToNoop();
		} finally {
			this.ringBuffer.publish(sequence);
			timerContext.stop();
		}
	}
	
	public MetricRegistry getMetricRegistry() {
	    return this.metricRegistry;
    }
	
	public boolean isReadyForProcessing() {
		if (!this.started) {
			return false;
		}
		if (this.etmConfiguration.isLicenseExpired()) {
			return false;
		}
		return true;
	}
	
	private void preProcess() {
		if (!this.started) {
			throw new IllegalStateException();
		}
		if (this.etmConfiguration.isLicenseExpired()) {
			throw new EtmException(EtmException.LICENSE_EXPIRED_EXCEPTION);
		}
	}

	@Override
	public void configurationChanged(ConfigurationChangedEvent event) {
		if (this.started && event.isAnyChanged(
				EtmConfiguration.CONFIG_KEY_ENHANCING_HANDLER_COUNT,
				EtmConfiguration.CONFIG_KEY_PERSISTING_HANDLER_COUNT)) {
			// TODO check for more options that need a restart, like changing the buffer size.
			// Configuration changed in such a way that the DisruptorEnvironment needs to be recreated/restarted.
			try {
				hotRestart();
			} catch (IllegalStateException e) {
				
			}
		}
	}
}