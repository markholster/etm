package com.holster.etm;

import java.nio.channels.IllegalSelectorException;
import java.util.concurrent.Executor;

import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.solr.client.solrj.SolrServer;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class TelemetryEventProcessor {

	private Disruptor<TelemetryEvent> disruptor;
	private RingBuffer<TelemetryEvent> ringBuffer;
	private boolean started = false;

    public void start(final Executor executor, final Session session, final SolrServer server, final int indexingHandlerCount, final int persistingHandlerCount) {
		if (this.started) {
			throw new IllegalStateException();
		}
		this.started = true;
		
		this.disruptor = new Disruptor<TelemetryEvent>(TelemetryEvent::new, 4096, executor, ProducerType.MULTI, new SleepingWaitStrategy());
		this.disruptor.handleExceptionsWith(new TelemetryEventExceptionHandler());
		
		final IndexingEventHandler[] indexingEventHandlers = new IndexingEventHandler[indexingHandlerCount];
		for (int i=0; i < indexingHandlerCount; i++) {
			indexingEventHandlers[i] = new IndexingEventHandler(server, i, indexingHandlerCount);
		}
		
		final PreparedStatement insertStatement = PersistingEventHandler.createInsertStatement(session);
		final PersistingEventHandler[] persistingEventHandlers = new PersistingEventHandler[persistingHandlerCount];
		for (int i=0; i < persistingHandlerCount; i++) {
			persistingEventHandlers[i] = new PersistingEventHandler(session, insertStatement, i, persistingHandlerCount);
		}
		
		this.disruptor.handleEventsWith(indexingEventHandlers);
		this.disruptor.handleEventsWith(persistingEventHandlers);
		this.ringBuffer = this.disruptor.start();
		
		
	}

	public void stop() {
		if (!this.started) {
			throw new IllegalSelectorException();
		}
		this.disruptor.shutdown();
	}

	public void processJmsMessage(final Message message) {
		if (!this.started) {
			throw new IllegalSelectorException();
		}
		long sequence = this.ringBuffer.next();
		try {
			TelemetryEvent telemetryEvent = this.ringBuffer.get(sequence);
			telemetryEvent.initialize();
			telemetryEvent.source = message;
			telemetryEvent.sourceId = message.getStringProperty("ETM_SourceID");
			if (telemetryEvent.sourceId == null) {
				telemetryEvent.sourceId = message.getJMSMessageID();
			}
			telemetryEvent.sourceCorrelationId = message.getStringProperty("ETM_SourceCorrelationID");
			if (telemetryEvent.sourceCorrelationId == null) { 
				telemetryEvent.sourceCorrelationId = message.getJMSCorrelationID();
			}
			telemetryEvent.endpoint = message.getStringProperty("ETM_Endpoint");
			telemetryEvent.application = message.getStringProperty("ETM_Application");
			telemetryEvent.eventName = message.getStringProperty("ETM_EventName");
			telemetryEvent.eventTime.setTime(message.getJMSTimestamp());
			telemetryEvent.transactionName = message.getStringProperty("ETM_TransactionName");
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				telemetryEvent.content = textMessage.getText();
			}
		} catch (Throwable t) {
			// TODO logging.
		} finally {
			this.ringBuffer.publish(sequence);
			
		}
	}
	
	public void processTelemetryEvent(TelemetryEvent telemetryEvent) {
		if (!this.started) {
			throw new IllegalSelectorException();
		}
		long sequence = this.ringBuffer.next();
		try {
			TelemetryEvent target = this.ringBuffer.get(sequence);
			target.initialize(telemetryEvent);
			target.source = telemetryEvent;
		} finally {
			this.ringBuffer.publish(sequence);
		}		
	}
}
