package com.jecstar.etm.processor.processor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.apache.solr.client.solrj.SolrClient;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.processor.TelemetryEvent;
import com.jecstar.etm.processor.repository.CorrelationBySourceIdResult;
import com.jecstar.etm.processor.repository.EndpointConfigResult;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class DisruptorEnvironment {

	private final Disruptor<TelemetryEvent> disruptor;
	private final TelemetryEventRepository telemetryEventRepository;
	private final IndexingEventHandler[] indexingEventHandlers;

	public DisruptorEnvironment(final EtmConfiguration etmConfiguration, final ExecutorService executorService, final SolrClient solrClient, final PersistenceEnvironment persistenceEnvironment, final Map<String, CorrelationBySourceIdResult> sourceCorrelations) {
		this.disruptor = new Disruptor<TelemetryEvent>(TelemetryEvent::new, etmConfiguration.getRingbufferSize(), executorService, ProducerType.MULTI, new SleepingWaitStrategy());
		this.disruptor.handleExceptionsWith(new TelemetryEventExceptionHandler(sourceCorrelations));
		int enhancingHandlerCount = etmConfiguration.getEnhancingHandlerCount();
		final EnhancingEventHandler[] enhancingEvntHandler = new EnhancingEventHandler[enhancingHandlerCount];
		this.telemetryEventRepository = persistenceEnvironment.createTelemetryEventRepository();
		for (int i = 0; i < enhancingHandlerCount; i++) {
			enhancingEvntHandler[i] = new EnhancingEventHandler(persistenceEnvironment.createTelemetryEventRepository(), i, enhancingHandlerCount, etmConfiguration);
		}
		int indexingHandlerCount = etmConfiguration.getIndexingHandlerCount();
		this.indexingEventHandlers = new IndexingEventHandler[indexingHandlerCount]; 
		for (int i = 0; i < indexingHandlerCount; i++) {
			this.indexingEventHandlers[i] = new IndexingEventHandler(solrClient, i, indexingHandlerCount);
		}
		
		int persistingHandlerCount = etmConfiguration.getPersistingHandlerCount();
		final PersistingEventHandler[] persistingEventHandlers = new PersistingEventHandler[persistingHandlerCount]; 
		for (int i = 0; i < persistingHandlerCount; i++) {
			persistingEventHandlers[i] = new PersistingEventHandler(persistenceEnvironment.createTelemetryEventRepository(), i, persistingHandlerCount, etmConfiguration);
		}
		this.disruptor.handleEventsWith(enhancingEvntHandler);
		if (persistingEventHandlers.length > 0) {
			this.disruptor.after(enhancingEvntHandler).handleEventsWith(persistingEventHandlers);
		}
		if (this.indexingEventHandlers.length > 0) {
			this.disruptor.after(enhancingEvntHandler).handleEventsWith(this.indexingEventHandlers);
		}
	}
	
	public RingBuffer<TelemetryEvent> start() {
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
    }

	public void findEndpointConfig(String endpoint, EndpointConfigResult result, long endpointCacheExpiryTime) {
	    this.telemetryEventRepository.findEndpointConfig(endpoint, result, endpointCacheExpiryTime);
    }
}
