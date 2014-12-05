package com.holster.etm;

import java.nio.channels.IllegalSelectorException;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.jms.JMSException;
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
		
		final Map<String, PreparedStatement> statements = PersistingEventHandler.createPreparedStatements(session);
		final PersistingEventHandler[] persistingEventHandlers = new PersistingEventHandler[persistingHandlerCount];
		for (int i=0; i < persistingHandlerCount; i++) {
			persistingEventHandlers[i] = new PersistingEventHandler(session, statements, i, persistingHandlerCount);
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
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				telemetryEvent.content = textMessage.getText();
			}
			telemetryEvent.sourceId = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_SOURCE_ID);
			if (telemetryEvent.sourceId == null) {
				telemetryEvent.sourceId = message.getJMSMessageID();
			}
			telemetryEvent.sourceCorrelationId = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_SOURCE_CORRELATION_ID);
			if (telemetryEvent.sourceCorrelationId == null) { 
				telemetryEvent.sourceCorrelationId = message.getJMSCorrelationID();
			}
			telemetryEvent.endpoint = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_ENDPOINT);
			telemetryEvent.application = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_APPLICATION);
			telemetryEvent.eventName = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_EVENT_NAME);
			telemetryEvent.eventTime.setTime(message.getJMSTimestamp());
			telemetryEvent.transactionName = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_TRANSACTION_NAME);
			determineEventType(telemetryEvent, message);
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			this.ringBuffer.publish(sequence);
			
		}
	}
	
	private void determineEventType(TelemetryEvent telemetryEvent, Message message) throws JMSException {
		String messageType = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_MESSAGE_TYPE);
		if (messageType != null) {
			try {
				telemetryEvent.eventType = TelemetryEventType.valueOf(messageType);
				return;
			} catch (IllegalArgumentException e) {
			}
		}
		int ibmMsgType = message.getIntProperty("JMS_IBM_MsgType");
		if (ibmMsgType == 1) {
			telemetryEvent.eventType = TelemetryEventType.MESSAGE_REQUEST;
		} else if (ibmMsgType == 2) {
			telemetryEvent.eventType = TelemetryEventType.MESSAGE_RESPONSE;
		} else if (ibmMsgType == 8) {
			telemetryEvent.eventType = TelemetryEventType.MESSAGE_DATAGRAM;
		}
    }

	public void processTelemetryEvent(final TelemetryEvent telemetryEvent) {
		if (!this.started) {
			throw new IllegalSelectorException();
		}
		long sequence = this.ringBuffer.next();
		try {
			TelemetryEvent target = this.ringBuffer.get(sequence);
			target.initialize(telemetryEvent);
		} finally {
			this.ringBuffer.publish(sequence);
		}		
	}
}
