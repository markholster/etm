package com.holster.etm;

import java.nio.channels.IllegalSelectorException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.solr.client.solrj.SolrServer;

import com.datastax.driver.core.Session;
import com.holster.etm.repository.CorrelationBySourceIdResult;
import com.holster.etm.repository.TelemetryEventRepository;
import com.holster.etm.repository.TelemetryEventRepositoryCassandraImpl;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class TelemetryEventProcessor {

	private Disruptor<TelemetryEvent> disruptor;
	private RingBuffer<TelemetryEvent> ringBuffer;
	private boolean started = false;
	
	//TODO proberen dit niet in een synchronised map te plaatsen.
	private final Map<String, CorrelationBySourceIdResult> sourceCorrelations = Collections.synchronizedMap(new HashMap<String, CorrelationBySourceIdResult>()); 

	@SuppressWarnings("unchecked")
	public void start(final Executor executor, final Session session, final SolrServer server, final int enhancingHandlerCount,
	        final int indexingHandlerCount, final int persistingHandlerCount) {
		if (this.started) {
			throw new IllegalStateException();
		}
		this.started = true;

		this.disruptor = new Disruptor<TelemetryEvent>(TelemetryEvent::new, 4096, executor, ProducerType.MULTI, new SleepingWaitStrategy());
		this.disruptor.handleExceptionsWith(new TelemetryEventExceptionHandler());

		final TelemetryEventRepository telemetryEventRepository = new TelemetryEventRepositoryCassandraImpl(session, this.sourceCorrelations);
		final EnhancingEventHandler[] correlatingEventHandlers = new EnhancingEventHandler[enhancingHandlerCount];
		for (int i = 0; i < enhancingHandlerCount; i++) {
			correlatingEventHandlers[i] = new EnhancingEventHandler(telemetryEventRepository, i, enhancingHandlerCount);
		}

		final List<EventHandler<TelemetryEvent>> step2handlers = new ArrayList<EventHandler<TelemetryEvent>>();
		for (int i = 0; i < indexingHandlerCount; i++) {
			step2handlers.add(new IndexingEventHandler(server, i, indexingHandlerCount));
		}

		for (int i = 0; i < persistingHandlerCount; i++) {
			step2handlers.add(new PersistingEventHandler(telemetryEventRepository, i, persistingHandlerCount));
		}

		EventHandler<TelemetryEvent>[] eventHandlers = new EventHandler[indexingHandlerCount + persistingHandlerCount];
		this.disruptor.handleEventsWith(correlatingEventHandlers).then(step2handlers.toArray(eventHandlers));
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
			telemetryEvent.transactionId = (UUID) message.getObjectProperty(TelemetryEvent.JMS_PROPERTY_KEY_TRANSACTION_ID);
			determineEventType(telemetryEvent, message);
			determineDirectionType(telemetryEvent, message);
			preProcess(telemetryEvent);
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
	
	private void determineDirectionType(TelemetryEvent telemetryEvent, Message message) throws JMSException {
		String direction = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_DIRECTION);
		if (direction != null) {
			try {
				telemetryEvent.eventDirection = TelemetryEventDirection.valueOf(direction);
				return;
			} catch (IllegalArgumentException e) {
			}
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
			preProcess(target);
		} finally {
			this.ringBuffer.publish(sequence);
		}
	}
	
	private void preProcess(TelemetryEvent event) {
		if (event.eventTime.getTime() == 0) {
			event.eventTime.setTime(System.currentTimeMillis());
		}
		if (event.transactionName != null && event.transactionId == null) {
			event.transactionId = event.id;
		}
		if (event.sourceId != null) {
			this.sourceCorrelations.put(event.sourceId, new CorrelationBySourceIdResult(event.id, event.transactionId, event.transactionName, event.eventTime.getTime()));
		}
		
	}
}
