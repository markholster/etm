package com.holster.etm.processor.processor;

import java.nio.channels.IllegalSelectorException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.apache.solr.client.solrj.SolrServer;

import com.datastax.driver.core.Session;
import com.holster.etm.processor.TelemetryEvent;
import com.holster.etm.processor.TelemetryEventType;
import com.holster.etm.processor.parsers.ExpressionParser;
import com.holster.etm.processor.repository.CorrelationBySourceIdResult;
import com.holster.etm.processor.repository.EndpointConfigResult;
import com.holster.etm.processor.repository.StatementExecutor;
import com.holster.etm.processor.repository.TelemetryEventRepository;
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
	
	private TelemetryEventRepository telemetryEventRepository;

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
		this.telemetryEventRepository = new TelemetryEventRepositoryCassandraImpl(statementExecutor, this.sourceCorrelations);
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
		this.telemetryEventRepository = null;
	}
	
	public void stopAll() {
		if (!this.started) {
			throw new IllegalSelectorException();
		}		
		this.executorService.shutdown();
		this.disruptor.shutdown();
		this.telemetryEventRepository = null;
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
		if ((event.transactionName == null || event.name == null)&& TelemetryEventType.MESSAGE_REQUEST.equals(event.type)) {
			// A little bit of enhancement before the event is processed by the
			// disruptor. We need to make sure the eventName & transactionName
			// is determined because the correlation of the transaction data on
			// the response will fail if the request and response are processed
			// at exactly the same time. By determining the event name &
			// transaction name at this point, the only requirement is that the
			// request is offered to ETM before the response, which is quite
			// logical.
			EndpointConfigResult result = new EndpointConfigResult();
			this.telemetryEventRepository.findEndpointConfig(event.endpoint, result);
			if (result.eventNameParsers != null && result.eventNameParsers.size() > 0) {
				event.name = parseValue(result.eventNameParsers, event.content);
			}
			if (result.transactionNameParsers != null && result.transactionNameParsers.size() > 0) {
				event.transactionName = parseValue(result.transactionNameParsers, event.content);
			}
		}
		if (event.transactionName != null) {
			event.transactionId = event.id;
		}
		if (event.sourceId != null) {
			this.sourceCorrelations.put(event.sourceId, new CorrelationBySourceIdResult(event.id, event.name, event.transactionId, event.transactionName, event.creationTime.getTime()));
		}
	}
	
	private String parseValue(List<ExpressionParser> expressionParsers, String content) {
		if (content == null || expressionParsers == null) {
			return null;
		}
		for (ExpressionParser expressionParser : expressionParsers) {
			String value = parseValue(expressionParser, content);
			if (value != null) {
				return value;
			}
		}
		return null;
    }
	
	private String parseValue(ExpressionParser expressionParser, String content) {
		if (expressionParser == null || content == null) {
			return null;
		}
		String value = expressionParser.evaluate(content);
		if (value != null && value.trim().length() > 0) {
			return value;
		}
		return null;
	}
}
