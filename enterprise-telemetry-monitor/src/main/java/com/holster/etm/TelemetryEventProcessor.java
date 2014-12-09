package com.holster.etm;

import java.nio.channels.IllegalSelectorException;
import java.util.ArrayList;
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
import com.holster.etm.repository.TelemetryEventRepository;
import com.holster.etm.repository.TelemetryEventRepositoryCassandraImpl;
import com.lmax.disruptor.AlertException;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class TelemetryEventProcessor {

	private Disruptor<TelemetryEvent> disruptor;
	private RingBuffer<TelemetryEvent> ringBuffer;
	private boolean started = false;
	private SequenceBarrier sequenceBarrier;
	
	private final Map<String, Long> sourceCorrelations = new HashMap<String, Long>(); 

	@SuppressWarnings("unchecked")
	public void start(final Executor executor, final Session session, final SolrServer server, final int correlatingHandlerCount,
	        final int indexingHandlerCount, final int persistingHandlerCount) {
		if (this.started) {
			throw new IllegalStateException();
		}
		this.started = true;

		this.disruptor = new Disruptor<TelemetryEvent>(TelemetryEvent::new, 2048, executor, ProducerType.MULTI, new SleepingWaitStrategy());
		this.disruptor.handleExceptionsWith(new TelemetryEventExceptionHandler());

		final TelemetryEventRepository telemetryEventRepository = new TelemetryEventRepositoryCassandraImpl(session);
		final EnhancingEventHandler[] correlatingEventHandlers = new EnhancingEventHandler[correlatingHandlerCount];
		for (int i = 0; i < correlatingHandlerCount; i++) {
			correlatingEventHandlers[i] = new EnhancingEventHandler(telemetryEventRepository, i, correlatingHandlerCount);
		}

		final List<EventHandler<TelemetryEvent>> step2handlers = new ArrayList<EventHandler<TelemetryEvent>>();
		for (int i = 0; i < indexingHandlerCount; i++) {
			step2handlers.add(new IndexingEventHandler(server, i, indexingHandlerCount));
		}

		for (int i = 0; i < persistingHandlerCount; i++) {
			step2handlers.add(new PersistingEventHandler(telemetryEventRepository, this.sourceCorrelations, i, persistingHandlerCount));
		}

		EventHandler<TelemetryEvent>[] eventHandlers = new EventHandler[indexingHandlerCount + persistingHandlerCount];
		this.sequenceBarrier = this.disruptor.handleEventsWith(correlatingEventHandlers).then(step2handlers.toArray(eventHandlers))
		        .asSequenceBarrier();
		this.ringBuffer = this.disruptor.start();
	}

	public void stop() {
		if (!this.started) {
			throw new IllegalSelectorException();
		}
		System.out.println(this.sourceCorrelations.size());
		this.disruptor.shutdown();
	}

	public void processJmsMessage(final Message message) {
		if (!this.started) {
			throw new IllegalSelectorException();
		}
		String sourceCorrelationId = determineSourceCorrelationId(message);
		
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
			telemetryEvent.sourceCorrelationId = sourceCorrelationId;
			telemetryEvent.endpoint = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_ENDPOINT);
			telemetryEvent.application = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_APPLICATION);
			telemetryEvent.eventName = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_EVENT_NAME);
			telemetryEvent.eventTime.setTime(message.getJMSTimestamp());
			telemetryEvent.transactionName = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_TRANSACTION_NAME);
			telemetryEvent.transactionId = (UUID) message.getObjectProperty(TelemetryEvent.JMS_PROPERTY_KEY_TRANSACTION_ID);
			determineEventType(telemetryEvent, message);
			checkAndWaitForCorrelation(telemetryEvent.sourceCorrelationId);
			sourceCorrelations.put(telemetryEvent.sourceId, sequence);
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			this.ringBuffer.publish(sequence);

		}
	}

	private String determineSourceCorrelationId(Message message) {
		try {
			String sourceCorrelationId = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_SOURCE_CORRELATION_ID);
			if (sourceCorrelationId == null) {
				sourceCorrelationId = message.getJMSCorrelationID();
			}
			return sourceCorrelationId;
		} catch (JMSException e) {
			e.printStackTrace();
		}
		return null;
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
			if (target.sourceId != null) {
				sourceCorrelations.put(telemetryEvent.sourceId, sequence);
			}
			checkAndWaitForCorrelation(target.sourceCorrelationId);
		} finally {
			this.ringBuffer.publish(sequence);
		}
	}

	private void checkAndWaitForCorrelation(String sourceId) {
		Long sequence = this.sourceCorrelations.get(sourceId);
		if (sequence != null) {
			try {
	            this.sequenceBarrier.waitFor(sequence);
            } catch (AlertException e) {
	            e.printStackTrace();
            } catch (InterruptedException e) {
	            e.printStackTrace();
            } catch (TimeoutException e) {
	            e.printStackTrace();
            }
			this.sourceCorrelations.remove(sourceId);
		}
    }
}
