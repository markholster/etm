package com.holster.etm.processor.mdb;

import java.io.StringReader;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.holster.etm.core.EtmException;
import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;
import com.holster.etm.jee.configurator.core.ProcessorConfiguration;
import com.holster.etm.processor.TelemetryEvent;
import com.holster.etm.processor.TelemetryEventDirection;
import com.holster.etm.processor.TelemetryEventType;
import com.holster.etm.processor.processor.TelemetryEventProcessor;

@MessageDriven(activationConfig = {
	    @ActivationConfigProperty(propertyName = "destinationLookup",
	            propertyValue = "jms/queue/EtmNotification"),
	    @ActivationConfigProperty(propertyName = "destinationType",
	            propertyValue = "javax.jms.Queue")
	})
public class JMSTelemetryEventProcessor implements MessageListener {
	
	public static final String JMS_PROPERTY_KEY_EVENT_APPLICATION = "JMS_ETM_Application";
	public static final String JMS_PROPERTY_KEY_EVENT_DIRECTION = "JMS_ETM_Direction";
	public static final String JMS_PROPERTY_KEY_EVENT_ENDPOINT = "JMS_ETM_Endpoint";
	public static final String JMS_PROPERTY_KEY_EVENT_NAME = "JMS_ETM_Name";
	public static final String JMS_PROPERTY_KEY_EVENT_SOURCE_CORRELATION_ID = "JMS_ETM_SourceCorrelationID";
	public static final String JMS_PROPERTY_KEY_EVENT_SOURCE_ID = "JMS_ETM_SourceID";
	public static final String JMS_PROPERTY_KEY_EVENT_TRANSACTION_NAME = "JMS_ETM_TransactionName";
	public static final String JMS_PROPERTY_KEY_EVENT_TYPE = "JMS_ETM_Type";
	
	public static final String JMS_PROPERTY_KEY_EVENT_NATIVE_FORMAT = "JMS_ETM_NativeFormat";

	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(TelemetryEventProcessor.class);

	@Inject
	@ProcessorConfiguration
	private TelemetryEventProcessor telemetryEventProcessor;

	private final TelemetryEvent telemetryEvent = new TelemetryEvent();
	
	private Unmarshaller unmarshaller;
	
	@PostConstruct
	private void initialize() {
		try {
	        JAXBContext jaxbContext = JAXBContext.newInstance(XmlTelemetryEvent.class);
	        this.unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
        	throw new EtmException(EtmException.UNMARSHALLER_CREATE_EXCEPTION, e);
        }
	}
	

	@Override
	public void onMessage(Message message) {
		try {
			this.telemetryEvent.initialize();
			boolean nativeFormat = message.getBooleanProperty(JMS_PROPERTY_KEY_EVENT_NATIVE_FORMAT);
			if (nativeFormat) {
				handleNative(message);
			} else {
				handleNoneNative(message);
			}
			this.telemetryEventProcessor.processTelemetryEvent(this.telemetryEvent);
		} catch (Throwable t) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage(t.getMessage(), t);
			}
		}
	}

	private void handleNative(Message message) throws JMSException {
		if (message instanceof javax.jms.TextMessage) {
			TextMessage textMessage = (TextMessage) message;
			try (StringReader reader = new StringReader(textMessage.getText());) {
	            this.unmarshaller.unmarshal(reader);
            } catch (JAXBException e) {
            	if (log.isErrorLevelEnabled()) {
            		log.logErrorMessage("Unable to unmarshall event.", e);
            	}
            }
		} else {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Message with msgid '" + message.getJMSMessageID() + "' is not a TextMessage, but a '" + message.getClass().getName() + "'.");
			}
		}
    }


	private void handleNoneNative(Message message) throws JMSException {
		if (message instanceof javax.jms.TextMessage) {
			TextMessage textMessage = (TextMessage) message;
			this.telemetryEvent.content = textMessage.getText();
		}
		this.telemetryEvent.sourceId = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_SOURCE_ID);
		if (this.telemetryEvent.sourceId == null) {
			this.telemetryEvent.sourceId = message.getJMSMessageID();
		}
		this.telemetryEvent.sourceCorrelationId = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_SOURCE_CORRELATION_ID);
		if (this.telemetryEvent.sourceCorrelationId == null) {
			this.telemetryEvent.sourceCorrelationId = message.getJMSCorrelationID();
		}
		this.telemetryEvent.endpoint = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_ENDPOINT);
		this.telemetryEvent.application = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_APPLICATION);
		this.telemetryEvent.name = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_NAME);
		this.telemetryEvent.creationTime.setTime(message.getJMSDeliveryTime());
		this.telemetryEvent.expiryTime.setTime(message.getJMSExpiration());
		this.telemetryEvent.transactionName = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_TRANSACTION_NAME);
		determineEventType(this.telemetryEvent, message);
		determineDirectionType(this.telemetryEvent, message);
    }

	private void determineEventType(TelemetryEvent telemetryEvent, Message message) throws JMSException {
		String messageType = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_TYPE);
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
		String direction = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_DIRECTION);
		if (direction != null) {
			try {
				telemetryEvent.direction = TelemetryEventDirection.valueOf(direction);
				return;
			} catch (IllegalArgumentException e) {
			}
		}
	}

}
