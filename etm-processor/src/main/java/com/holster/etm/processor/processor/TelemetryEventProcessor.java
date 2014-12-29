package com.holster.etm.processor.processor;

import java.nio.channels.IllegalSelectorException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.apache.solr.client.solrj.SolrServer;

import com.datastax.driver.core.Session;
import com.holster.etm.processor.TelemetryEvent;
import com.holster.etm.processor.repository.CorrelationBySourceIdResult;
import com.holster.etm.processor.repository.StatementExecutor;
import com.holster.etm.processor.repository.TelemetryEventRepositoryCassandraImpl;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class TelemetryEventProcessor {
	
	private Disruptor<TelemetryEvent> disruptor;
	private RingBuffer<TelemetryEvent> ringBuffer;
	private boolean started = false;
	
	//TODO proberen dit niet in een synchronised map te plaatsen, maar bijvoorbeeld in een ConcurrentMap
	private final Map<String, CorrelationBySourceIdResult> sourceCorrelations = Collections.synchronizedMap(new HashMap<String, CorrelationBySourceIdResult>());
	
	private ExecutorService executorService;
	private Session cassandraSession;
	private SolrServer solrServer; 

	public void start(final ExecutorService executorService, final Session session, final SolrServer solrServer, final int ringbufferSize, final int enhancingHandlerCount,
	        final int indexingHandlerCount, final int persistingHandlerCount) {
		if (this.started) {
			throw new IllegalStateException();
		}
		this.started = true;
		this.executorService = executorService;
		this.cassandraSession = session;
		this.solrServer = solrServer;
		
		this.disruptor = new Disruptor<TelemetryEvent>(TelemetryEvent::new, ringbufferSize, this.executorService, ProducerType.MULTI, new SleepingWaitStrategy());
		this.disruptor.handleExceptionsWith(new TelemetryEventExceptionHandler());
		final StatementExecutor statementExecutor = new StatementExecutor(this.cassandraSession, "etm");
		final EnhancingEventHandler[] enhancingEvntHandler = new EnhancingEventHandler[enhancingHandlerCount];
		for (int i = 0; i < enhancingHandlerCount; i++) {
			enhancingEvntHandler[i] = new EnhancingEventHandler(new TelemetryEventRepositoryCassandraImpl(statementExecutor, this.sourceCorrelations), i, enhancingHandlerCount);
		}

		final IndexingEventHandler[] indexingEventHandlers = new IndexingEventHandler[indexingHandlerCount]; 
		for (int i = 0; i < indexingHandlerCount; i++) {
			indexingEventHandlers[i] = new IndexingEventHandler(this.solrServer, i, indexingHandlerCount);
		}
		
		final PersistingEventHandler[] persistingEventHandlers = new PersistingEventHandler[persistingHandlerCount]; 
		for (int i = 0; i < persistingHandlerCount; i++) {
			persistingEventHandlers[i] = new PersistingEventHandler(new TelemetryEventRepositoryCassandraImpl(statementExecutor, this.sourceCorrelations), i, persistingHandlerCount);
		}
		this.disruptor.handleEventsWith(enhancingEvntHandler);
		this.disruptor.after(enhancingEvntHandler).handleEventsWith(persistingEventHandlers);
		this.disruptor.after(enhancingEvntHandler).handleEventsWith(indexingEventHandlers);
		this.ringBuffer = this.disruptor.start();
	}
	
	public void stop() {
		if (!this.started) {
			throw new IllegalSelectorException();
		}
		this.disruptor.shutdown();
	}
	
	public void stopAll() {
		if (!this.started) {
			throw new IllegalSelectorException();
		}		
		this.executorService.shutdown();
		this.disruptor.shutdown();
		this.cassandraSession.close();
		this.solrServer.shutdown();
	}


	public void processTelemetryEvent(final TelemetryEvent telemetryEvent) {
		if (!this.started) {
			throw new IllegalSelectorException();
		}
		long sequence = this.ringBuffer.next();
		try {
			TelemetryEvent target = this.ringBuffer.get(sequence);
			target.initialize(telemetryEvent);
			preProcess(target);
		} finally {
			this.ringBuffer.publish(sequence);
		}
	}
	
	private void preProcess(TelemetryEvent event) {
		if (event.creationTime.getTime() == 0) {
			event.creationTime.setTime(System.currentTimeMillis());
		}
		if (event.transactionName != null) {
			event.transactionId = event.id;
		}
		if (event.sourceId != null) {
			this.sourceCorrelations.put(event.sourceId, new CorrelationBySourceIdResult(event.id, event.transactionId, event.transactionName, event.creationTime.getTime()));
		}
		
	}
}
