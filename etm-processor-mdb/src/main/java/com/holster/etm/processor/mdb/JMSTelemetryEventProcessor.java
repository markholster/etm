package com.holster.etm.processor.mdb;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import com.holster.etm.processor.TelemetryEvent;
import com.holster.etm.processor.TelemetryEventDirection;
import com.holster.etm.processor.TelemetryEventType;
import com.holster.etm.processor.logging.LogFactory;
import com.holster.etm.processor.logging.LogWrapper;
import com.holster.etm.processor.processor.TelemetryEventProcessor;

@MessageDriven(activationConfig = {
	    @ActivationConfigProperty(propertyName = "destinationLookup",
	            propertyValue = "jms/EtmQueue"),
	    @ActivationConfigProperty(propertyName = "destinationType",
	            propertyValue = "javax.jms.Queue")
	})
public class JMSTelemetryEventProcessor implements MessageListener {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(TelemetryEventProcessor.class);

	@Inject
	private TelemetryEventProcessor telemetryEventProcessor;

	private final TelemetryEvent telemetryEvent = new TelemetryEvent();

	@Override
	public void onMessage(Message message) {
		try {
			this.telemetryEvent.initialize();
			if (message instanceof javax.jms.TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				this.telemetryEvent.content = textMessage.getText();
			}
			this.telemetryEvent.sourceId = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_EVENT_SOURCE_ID);
			if (this.telemetryEvent.sourceId == null) {
				this.telemetryEvent.sourceId = message.getJMSMessageID();
			}
			this.telemetryEvent.sourceCorrelationId = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_EVENT_SOURCE_CORRELATION_ID);
			if (this.telemetryEvent.sourceCorrelationId == null) {
				this.telemetryEvent.sourceCorrelationId = message.getJMSCorrelationID();
			}
			this.telemetryEvent.endpoint = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_EVENT_ENDPOINT);
			this.telemetryEvent.application = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_EVENT_APPLICATION);
			this.telemetryEvent.name = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_EVENT_NAME);
			this.telemetryEvent.creationTime.setTime(message.getJMSDeliveryTime());
			this.telemetryEvent.expiryTime.setTime(message.getJMSExpiration());
			this.telemetryEvent.transactionName = message.getStringProperty(TelemetryEvent.JMS_PROPERTY_KEY_EVENT_TRANSACTION_NAME);
			determineEventType(this.telemetryEvent, message);
			determineDirectionType(this.telemetryEvent, message);
			this.telemetryEventProcessor.processTelemetryEvent(this.telemetryEvent);
		} catch (Throwable t) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage(t.getMessage(), t);
			}
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

}
