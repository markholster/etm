package com.holster.etm.processor.processor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.apache.solr.client.solrj.SolrServer;

import com.datastax.driver.core.Session;
import com.holster.etm.core.configuration.EtmConfiguration;
import com.holster.etm.processor.TelemetryEvent;
import com.holster.etm.processor.repository.CorrelationBySourceIdResult;
import com.holster.etm.processor.repository.EndpointConfigResult;
import com.holster.etm.processor.repository.StatementExecutor;
import com.holster.etm.processor.repository.TelemetryEventRepositoryCassandraImpl;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class DisruptorEnvironment {

	private final Disruptor<TelemetryEvent> disruptor;
	private final TelemetryEventRepositoryCassandraImpl telemetryEventRepository;
	private final IndexingEventHandler[] indexingEventHandlers;

	public DisruptorEnvironment(final EtmConfiguration etmConfiguration, final ExecutorService executorService, final Session session, final SolrServer solrServer, final StatementExecutor statementExecutor, final Map<String, CorrelationBySourceIdResult> sourceCorrelations) {
		this.disruptor = new Disruptor<TelemetryEvent>(TelemetryEvent::new, etmConfiguration.getRingbufferSize(), executorService, ProducerType.MULTI, new SleepingWaitStrategy());
		this.disruptor.handleExceptionsWith(new TelemetryEventExceptionHandler(sourceCorrelations));
		int enhancingHandlerCount = etmConfiguration.getEnhancingHandlerCount();
		final EnhancingEventHandler[] enhancingEvntHandler = new EnhancingEventHandler[enhancingHandlerCount];
		this.telemetryEventRepository = new TelemetryEventRepositoryCassandraImpl(statementExecutor, sourceCorrelations);
		for (int i = 0; i < enhancingHandlerCount; i++) {
			enhancingEvntHandler[i] = new EnhancingEventHandler(new TelemetryEventRepositoryCassandraImpl(statementExecutor, sourceCorrelations), i, enhancingHandlerCount, etmConfiguration);
		}
		int indexingHandlerCount = etmConfiguration.getIndexingHandlerCount();
		this.indexingEventHandlers = new IndexingEventHandler[indexingHandlerCount]; 
		for (int i = 0; i < indexingHandlerCount; i++) {
			this.indexingEventHandlers[i] = new IndexingEventHandler(solrServer, i, indexingHandlerCount);
		}
		
		int persistingHandlerCount = etmConfiguration.getPersistingHandlerCount();
		final PersistingEventHandler[] persistingEventHandlers = new PersistingEventHandler[persistingHandlerCount]; 
		for (int i = 0; i < persistingHandlerCount; i++) {
			persistingEventHandlers[i] = new PersistingEventHandler(new TelemetryEventRepositoryCassandraImpl(statementExecutor, sourceCorrelations), i, persistingHandlerCount, etmConfiguration);
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
