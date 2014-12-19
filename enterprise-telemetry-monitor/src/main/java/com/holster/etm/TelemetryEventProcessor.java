package com.holster.etm;

import java.nio.channels.IllegalSelectorException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.solr.client.solrj.SolrServer;

import com.datastax.driver.core.Session;
import com.holster.etm.logging.LogFactory;
import com.holster.etm.logging.LogWrapper;
import com.holster.etm.repository.CorrelationBySourceIdResult;
import com.holster.etm.repository.TelemetryEventRepository;
import com.holster.etm.repository.TelemetryEventRepositoryCassandraImpl;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class TelemetryEventProcessor {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(TelemetryEventProcessor.class);

	private Disruptor<TelemetryEvent> disruptor;
	private RingBuffer<TelemetryEvent> ringBuffer;
	private boolean started = false;
	
	//TODO proberen dit niet in een synchronised map te plaatsen.
	private final Map<String, CorrelationBySourceIdResult> sourceCorrelations = Collections.synchronizedMap(new HashMap<String, CorrelationBySourceIdResult>()); 

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

		final IndexingEventHandler[] indexingEventHandlers = new IndexingEventHandler[indexingHandlerCount]; 
		for (int i = 0; i < indexingHandlerCount; i++) {
			indexingEventHandlers[i] = new IndexingEventHandler(server, i, indexingHandlerCount);
		}
		
		final PersistingEventHandler[] persistingEventHandlers = new PersistingEventHandler[persistingHandlerCount]; 
		for (int i = 0; i < persistingHandlerCount; i++) {
			persistingEventHandlers[i] = new PersistingEventHandler(telemetryEventRepository, i, persistingHandlerCount);
		}
		this.disruptor.handleEventsWith(correlatingEventHandlers);
		this.disruptor.after(correlatingEventHandlers).handleEventsWith(persistingEventHandlers);
		this.disruptor.after(correlatingEventHandlers).handleEventsWith(indexingEventHandlers);
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
			telemetryEvent.sourceId = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_EVENT_SOURCE_ID);
			if (telemetryEvent.sourceId == null) {
				telemetryEvent.sourceId = message.getJMSMessageID();
			}
			telemetryEvent.sourceCorrelationId = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_EVENT_SOURCE_CORRELATION_ID);
			if (telemetryEvent.sourceCorrelationId == null) {
				telemetryEvent.sourceCorrelationId = message.getJMSCorrelationID();
			}
			telemetryEvent.endpoint = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_EVENT_ENDPOINT);
			telemetryEvent.application = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_EVENT_APPLICATION);
			telemetryEvent.name = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_EVENT_NAME);
			telemetryEvent.creationTime.setTime(message.getJMSDeliveryTime());
			telemetryEvent.expiryTime.setTime(message.getJMSExpiration());
			telemetryEvent.transactionName = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_EVENT_TRANSACTION_NAME);
			determineEventType(telemetryEvent, message);
			determineDirectionType(telemetryEvent, message);
			preProcess(telemetryEvent);
		} catch (Throwable t) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage(t.getMessage(), t);
			}
		} finally {
			this.ringBuffer.publish(sequence);

		}
	}

	private void determineEventType(TelemetryEvent telemetryEvent, Message message) throws JMSException {
		String messageType = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_EVENT_TYPE);
		if (messageType != null) {
			try {
				telemetryEvent.type = TelemetryEventType.valueOf(messageType);
				return;
			} catch (IllegalArgumentException e) {
			}
		}
		int ibmMsgType = message.getIntProperty("JMS_IBM_MsgType");
		if (ibmMsgType == 1) {
			telemetryEvent.type = TelemetryEventType.MESSAGE_REQUEST;
		} else if (ibmMsgType == 2) {
			telemetryEvent.type = TelemetryEventType.MESSAGE_RESPONSE;
		} else if (ibmMsgType == 8) {
			telemetryEvent.type = TelemetryEventType.MESSAGE_DATAGRAM;
		}
	}
	
	private void determineDirectionType(TelemetryEvent telemetryEvent, Message message) throws JMSException {
		String direction = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_EVENT_DIRECTION);
		if (direction != null) {
			try {
				telemetryEvent.direction = TelemetryEventDirection.valueOf(direction);
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
