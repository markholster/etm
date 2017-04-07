package com.jecstar.etm.processor.core;

import java.util.concurrent.ThreadFactory;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class DisruptorEnvironment {

	private final Disruptor<TelemetryCommand> disruptor;
	private final PersistingEventHandler[] persistingEventHandlers;

	public DisruptorEnvironment(final EtmConfiguration etmConfiguration, final ThreadFactory threadFactory, final PersistenceEnvironment persistenceEnvironment, final MetricRegistry metricRegistry) {
		this.disruptor = new Disruptor<TelemetryCommand>(TelemetryCommand::new, etmConfiguration.getEventBufferSize(), threadFactory, ProducerType.MULTI, convertWaitStrategy(etmConfiguration.getWaitStrategy()));
		this.disruptor.setDefaultExceptionHandler(new TelemetryCommandExceptionHandler(metricRegistry));
		int enhancingHandlerCount = etmConfiguration.getEnhancingHandlerCount();
		final EnhancingEventHandler[] enhancingEventHandlers = new EnhancingEventHandler[enhancingHandlerCount];
		for (int i = 0; i < enhancingHandlerCount; i++) {
			enhancingEventHandlers[i] = new EnhancingEventHandler(i, enhancingHandlerCount, etmConfiguration, persistenceEnvironment.getCommandResources(metricRegistry), metricRegistry);
		}
		int persistingHandlerCount = etmConfiguration.getPersistingHandlerCount();
		this.persistingEventHandlers = new PersistingEventHandler[persistingHandlerCount]; 
		for (int i = 0; i < persistingHandlerCount; i++) {
			this.persistingEventHandlers[i] = new PersistingEventHandler(i, persistingHandlerCount, persistenceEnvironment.getCommandResources(metricRegistry), metricRegistry);
		}
		if (enhancingEventHandlers.length > 0) {
			this.disruptor.handleEventsWith(enhancingEventHandlers);
			if (this.persistingEventHandlers.length > 0) {
				this.disruptor.after(enhancingEventHandlers).handleEventsWith(this.persistingEventHandlers);
			}
		} else {
			if (this.persistingEventHandlers.length > 0) {
				this.disruptor.handleEventsWith(this.persistingEventHandlers);
			}
		}
	}
	
	private WaitStrategy convertWaitStrategy(com.jecstar.etm.server.core.configuration.WaitStrategy waitStrategy) {
		switch (waitStrategy) {
		case BLOCKING:
			return new BlockingWaitStrategy();
		case BUSY_SPIN:
			return new BusySpinWaitStrategy();
		case SLEEPING:
			return new SleepingWaitStrategy();
		case YIELDING:
			return new YieldingWaitStrategy();
		default:
			return null;
		}
		
	}

	public RingBuffer<TelemetryCommand> start() {
		return this.disruptor.start();
	}

	public void shutdown() {
		this.disruptor.shutdown();
		for (PersistingEventHandler persistingEventHandler : this.persistingEventHandlers) {
			persistingEventHandler.close();
		}
    }
}
