package com.jecstar.etm.processor.mdb;

import java.io.StringReader;

import javax.annotation.PostConstruct;
import javax.ejb.Schedule;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.TelemetryEvent;
import com.jecstar.etm.core.TelemetryEventDirection;
import com.jecstar.etm.core.TelemetryEventType;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.jee.configurator.core.ProcessorConfiguration;
import com.jecstar.etm.processor.processor.TelemetryEventProcessor;

public class JMSTelemetryEventProcessor implements MessageListener {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(TelemetryEventProcessor.class);
	
	public static final String JMS_PROPERTY_KEY_EVENT_APPLICATION = "JMS_ETM_Application";
	public static final String JMS_PROPERTY_KEY_EVENT_DIRECTION = "JMS_ETM_Direction";
	public static final String JMS_PROPERTY_KEY_EVENT_ENDPOINT = "JMS_ETM_Endpoint";
	public static final String JMS_PROPERTY_KEY_EVENT_NAME = "JMS_ETM_Name";
	public static final String JMS_PROPERTY_KEY_EVENT_CORRELATION_ID = "JMS_ETM_CorrelationId";
	public static final String JMS_PROPERTY_KEY_EVENT_ID = "JMS_ETM_Id";
	public static final String JMS_PROPERTY_KEY_EVENT_TRANSACTION_NAME = "JMS_ETM_TransactionName";
	public static final String JMS_PROPERTY_KEY_EVENT_TYPE = "JMS_ETM_Type";
	
	public static final String JMS_PROPERTY_KEY_EVENT_NATIVE_FORMAT = "JMS_ETM_NativeFormat";

	@ProcessorConfiguration
	@Inject
	private EtmConfiguration configration;
	
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
	
	@Schedule(minute="*", hour="*", second="0,30", persistent=false)
	public void flushDocuments() {
		if (log.isDebugLevelEnabled()) {
			log.logDebugMessage("Requesting flush of documents");
		}
		if (this.telemetryEventProcessor != null) {
			this.telemetryEventProcessor.requestDocumentsFlush();
		}
	}

	@Override
	public void onMessage(Message message) {
		try {
			this.telemetryEvent.initialize();
			if (!handleNative(message)) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Message with msgid '" + message.getJMSMessageID() + "' could not be parsed nativly, trying to make the best of it.");
				}
				handleNonNative(message);
			}
			this.telemetryEventProcessor.processTelemetryEvent(this.telemetryEvent);
		} catch (Throwable t) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage(t.getMessage(), t);
			}
		}
	}

	private boolean handleNative(Message message) throws JMSException {
		if (message instanceof javax.jms.TextMessage) {
			TextMessage textMessage = (TextMessage) message;
			try (StringReader reader = new StringReader(textMessage.getText());) {
	            XmlTelemetryEvent xmlTelemetryEvent = (XmlTelemetryEvent) this.unmarshaller.unmarshal(reader);
	            xmlTelemetryEvent.copyToTelemetryEvent(this.telemetryEvent);
	            return true;
            } catch (JAXBException e) {
            	if (log.isDebugLevelEnabled()) {
            		log.logDebugMessage("Unable to unmarshall event.", e);
            	}
            }
		} else {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Message with msgid '" + message.getJMSMessageID() + "' is not a TextMessage, but a '" + message.getClass().getName() + "'.");
			}
		}
		return false;
    }


	private void handleNonNative(Message message) throws JMSException {
		if (message instanceof javax.jms.TextMessage) {
			TextMessage textMessage = (TextMessage) message;
			this.telemetryEvent.content = textMessage.getText();
		} else {
			if (log.isInfoLevelEnabled()) {
				log.logInfoMessage("Message with msgid '" + message.getJMSMessageID() + "' is not a TextMessage, but a '"
				        + message.getClass().getName() + "'. Unable to retrieve content");
			}
		}
		this.telemetryEvent.id = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_ID);
		if (this.telemetryEvent.id != null && this.telemetryEvent.id.startsWith("ID:")) {
			this.telemetryEvent.id = this.telemetryEvent.id.substring(3);
		}
		if (this.telemetryEvent.id == null) {
			this.telemetryEvent.id = message.getJMSMessageID();
		}
		this.telemetryEvent.correlationId = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_CORRELATION_ID);
		if (this.telemetryEvent.correlationId == null) {
			this.telemetryEvent.correlationId = message.getJMSCorrelationID();
			if (this.telemetryEvent.correlationId != null && this.telemetryEvent.correlationId.startsWith("ID:")) {
				this.telemetryEvent.correlationId = this.telemetryEvent.correlationId.substring(3);
			}
		}
		this.telemetryEvent.endpoint = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_ENDPOINT);
		this.telemetryEvent.application = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_APPLICATION);
		this.telemetryEvent.name = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_NAME);
		long time = message.getJMSDeliveryTime();
		if (time == 0) {
			time = message.getJMSTimestamp();
		}
		this.telemetryEvent.creationTime.setTime(time);
		this.telemetryEvent.expiryTime.setTime(message.getJMSExpiration());
		this.telemetryEvent.transactionName = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_TRANSACTION_NAME);
		determineEventType(this.telemetryEvent, message);
		determineDirectionType(this.telemetryEvent, message);
		customAchmea();
    }

	/**
	 * Achmea maatwerk -> Zolang er niet van WMB events gebruik gemaakt wordt.
	 */
	private void customAchmea() {
		String companyName = configration.getLicense().getOwner();
		if (companyName.startsWith("Achmea")) {
			if (TelemetryEventType.MESSAGE_DATAGRAM.equals(this.telemetryEvent.type)) {
				if (this.telemetryEvent.content != null) {
					if (this.telemetryEvent.content.indexOf("Request") != -1) {
						this.telemetryEvent.type = TelemetryEventType.MESSAGE_REQUEST;
					} else if (this.telemetryEvent.content.indexOf("Response") != -1) {
						this.telemetryEvent.type = TelemetryEventType.MESSAGE_RESPONSE;
					}
				}
			}
		}
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
