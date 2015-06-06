package com.jecstar.etm.processor.processor;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.apache.solr.client.solrj.SolrClient;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.core.TelemetryCommand;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.processor.repository.EndpointConfigResult;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class DisruptorEnvironment {

	private final Disruptor<TelemetryCommand> disruptor;
	private final TelemetryEventRepository telemetryEventRepository;
	private final IndexingEventHandler[] indexingEventHandlers;
	private final PersistingEventHandler[] persistingEventHandlers;

	public DisruptorEnvironment(final EtmConfiguration etmConfiguration, final ExecutorService executorService, final SolrClient solrClient, final PersistenceEnvironment persistenceEnvironment, final MetricRegistry metricRegistry) {
		this.disruptor = new Disruptor<TelemetryCommand>(TelemetryCommand::new, etmConfiguration.getRingbufferSize(), executorService, ProducerType.MULTI, new SleepingWaitStrategy());
		this.disruptor.handleExceptionsWith(new TelemetryCommandExceptionHandler());
		int enhancingHandlerCount = etmConfiguration.getEnhancingHandlerCount();
		final EnhancingEventHandler[] enhancingEvntHandler = new EnhancingEventHandler[enhancingHandlerCount];
		this.telemetryEventRepository = persistenceEnvironment.createTelemetryEventRepository();
		for (int i = 0; i < enhancingHandlerCount; i++) {
			enhancingEvntHandler[i] = new EnhancingEventHandler(persistenceEnvironment.createTelemetryEventRepository(), i, enhancingHandlerCount, etmConfiguration, metricRegistry.timer("event-enhancing"));
		}
		int indexingHandlerCount = etmConfiguration.getIndexingHandlerCount();
		this.indexingEventHandlers = new IndexingEventHandler[indexingHandlerCount]; 
		for (int i = 0; i < indexingHandlerCount; i++) {
			this.indexingEventHandlers[i] = new IndexingEventHandler(solrClient, i, indexingHandlerCount, metricRegistry.timer("event-indexing"));
		}
		
		int persistingHandlerCount = etmConfiguration.getPersistingHandlerCount();
		this.persistingEventHandlers = new PersistingEventHandler[persistingHandlerCount]; 
		for (int i = 0; i < persistingHandlerCount; i++) {
			this.persistingEventHandlers[i] = new PersistingEventHandler(persistenceEnvironment.createTelemetryEventRepository(), i, persistingHandlerCount, etmConfiguration, metricRegistry.timer("event-persisting"));
		}
		this.disruptor.handleEventsWith(enhancingEvntHandler);
		if (this.persistingEventHandlers.length > 0) {
			this.disruptor.after(enhancingEvntHandler).handleEventsWith(this.persistingEventHandlers);
		}
		if (this.indexingEventHandlers.length > 0) {
			this.disruptor.after(enhancingEvntHandler).handleEventsWith(this.indexingEventHandlers);
		}
	}
	
	public RingBuffer<TelemetryCommand> start() {
		return this.disruptor.start();
	}

	public void shutdown() {
		this.disruptor.shutdown();
		for (IndexingEventHandler indexingEventHandler : this.indexingEventHandlers) {
			try {
	            indexingEventHandler.close();
            } catch (IOException e) {
            }
		}
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

	public void findEndpointConfig(String endpoint, EndpointConfigResult result, long endpointCacheExpiryTime) {
	    this.telemetryEventRepository.findEndpointConfig(endpoint, result, endpointCacheExpiryTime);
    }
}
