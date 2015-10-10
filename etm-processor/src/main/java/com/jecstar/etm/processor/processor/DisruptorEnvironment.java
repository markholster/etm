package com.jecstar.etm.processor.processor;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.EndpointConfiguration;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class DisruptorEnvironment {

	private final Disruptor<TelemetryCommand> disruptor;
	private final TelemetryEventRepository telemetryEventRepository;
	private final PersistingEventHandler[] persistingEventHandlers;
	private final PersistenceEnvironment persistenceEnvironment;

	public DisruptorEnvironment(final EtmConfiguration etmConfiguration, final ExecutorService executorService, final PersistenceEnvironment persistenceEnvironment, final MetricRegistry metricRegistry) {
		this.disruptor = new Disruptor<TelemetryCommand>(TelemetryCommand::new, etmConfiguration.getEventBufferSize(), executorService, ProducerType.MULTI, new SleepingWaitStrategy());
		this.disruptor.handleExceptionsWith(new TelemetryCommandExceptionHandler());
		int enhancingHandlerCount = etmConfiguration.getEnhancingHandlerCount();
		final EnhancingEventHandler[] enhancingEventHandlers = new EnhancingEventHandler[enhancingHandlerCount];
		this.telemetryEventRepository = persistenceEnvironment.createTelemetryEventRepository(metricRegistry);
		for (int i = 0; i < enhancingHandlerCount; i++) {
			enhancingEventHandlers[i] = new EnhancingEventHandler(persistenceEnvironment.createTelemetryEventRepository(metricRegistry), i, enhancingHandlerCount, metricRegistry);
		}
		int persistingHandlerCount = etmConfiguration.getPersistingHandlerCount();
		this.persistingEventHandlers = new PersistingEventHandler[persistingHandlerCount]; 
		for (int i = 0; i < persistingHandlerCount; i++) {
			this.persistingEventHandlers[i] = new PersistingEventHandler(persistenceEnvironment.createTelemetryEventRepository(metricRegistry), i, persistingHandlerCount, metricRegistry);
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
		this.persistenceEnvironment = persistenceEnvironment;
	}
	
	public RingBuffer<TelemetryCommand> start() {
		this.persistenceEnvironment.createEnvironment();
		return this.disruptor.start();
	}

	public void shutdown() {
		this.disruptor.shutdown();
		for (PersistingEventHandler persistingEventHandler : this.persistingEventHandlers) {
			try {
	            persistingEventHandler.close();
            } catch (IOException e) {
            }
		}
		try {
	        this.telemetryEventRepository.close();
        } catch (IOException e) {
	        e.printStackTrace();
        }
    }

	public void findEndpointConfig(String endpoint, EndpointConfiguration result) {
	    this.telemetryEventRepository.findEndpointConfig(endpoint, result);
    }
}
