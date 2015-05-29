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
import com.jecstar.etm.core.TelemetryCommand;
import com.jecstar.etm.core.TelemetryEventDirection;
import com.jecstar.etm.core.TelemetryMessageEventType;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.jee.configurator.core.ProcessorConfiguration;
import com.jecstar.etm.processor.TelemetryEvent;
import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;

public class JMSTelemetryEventProcessor implements MessageListener {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(TelemetryCommandProcessor.class);
	
	public static final String JMS_PROPERTY_KEY_EVENT_APPLICATION = "JMS_ETM_Application";
	public static final String JMS_PROPERTY_KEY_EVENT_DIRECTION = "JMS_ETM_Direction";
	public static final String JMS_PROPERTY_KEY_EVENT_ENDPOINT = "JMS_ETM_Endpoint";
	public static final String JMS_PROPERTY_KEY_EVENT_NAME = "JMS_ETM_Name";
	public static final String JMS_PROPERTY_KEY_EVENT_SOURCE_CORRELATION_ID = "JMS_ETM_SourceCorrelationID";
	public static final String JMS_PROPERTY_KEY_EVENT_SOURCE_ID = "JMS_ETM_SourceID";
	public static final String JMS_PROPERTY_KEY_EVENT_TRANSACTION_NAME = "JMS_ETM_TransactionName";
	public static final String JMS_PROPERTY_KEY_EVENT_TYPE = "JMS_ETM_Type";
	
	public static final String JMS_PROPERTY_KEY_EVENT_NATIVE_FORMAT = "JMS_ETM_NativeFormat";

	@ProcessorConfiguration
	@Inject
	private EtmConfiguration configration;
	
	@Inject
	@ProcessorConfiguration
	private TelemetryCommandProcessor telemetryCommandProcessor;

	private final TelemetryCommand telemetryCommand = new TelemetryCommand();
	
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
			log.logDebugMessage("Requesting flush of Solr documents");
		}
		if (this.telemetryCommandProcessor != null) {
			this.telemetryCommandProcessor.requestDocumentsFlush();
		}
	}

	@Override
	public void onMessage(Message message) {
		try {
			this.telemetryCommand.initialize();
			if (!handleNative(message)) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Message with msgid '" + message.getJMSMessageID() + "' could not be parsed nativly, trying to make the best of it.");
				}
				handleNonNative(message);
			}
			this.telemetryCommandProcessor.processTelemetryEvent(this.telemetryCommand);
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
	            xmlTelemetryEvent.copyToTelemetryEvent(this.telemetryCommand);
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
			this.telemetryCommand.content = textMessage.getText();
		} else {
			if (log.isInfoLevelEnabled()) {
				log.logInfoMessage("Message with msgid '" + message.getJMSMessageID() + "' is not a TextMessage, but a '"
				        + message.getClass().getName() + "'. Unable to retrieve content");
			}
		}
		this.telemetryCommand.sourceId = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_SOURCE_ID);
		if (this.telemetryCommand.sourceId == null) {
			this.telemetryCommand.sourceId = message.getJMSMessageID();
		}
		this.telemetryCommand.sourceCorrelationId = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_SOURCE_CORRELATION_ID);
		if (this.telemetryCommand.sourceCorrelationId == null) {
			this.telemetryCommand.sourceCorrelationId = message.getJMSCorrelationID();
		}
		this.telemetryCommand.endpoint = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_ENDPOINT);
		this.telemetryCommand.application = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_APPLICATION);
		this.telemetryCommand.name = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_NAME);
		long time = message.getJMSDeliveryTime();
		if (time == 0) {
			time = message.getJMSTimestamp();
		}
		this.telemetryCommand.creationTime.setTime(time);
		this.telemetryCommand.expiryTime.setTime(message.getJMSExpiration());
		this.telemetryCommand.transactionName = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_TRANSACTION_NAME);
		determineEventType(this.telemetryCommand, message);
		determineDirectionType(this.telemetryCommand, message);
		customAchmea();
    }

	/**
	 * Achmea maatwerk -> Zolang er niet van WMB events gebruik gemaakt wordt.
	 */
	private void customAchmea() {
		String companyName = configration.getCompanyName();
		if (companyName.startsWith("Achmea")) {
			if (TelemetryMessageEventType.MESSAGE_DATAGRAM.equals(this.telemetryCommand.type)) {
				if (this.telemetryCommand.content != null) {
					if (this.telemetryCommand.content.indexOf("Request") != -1) {
						this.telemetryCommand.type = TelemetryMessageEventType.MESSAGE_REQUEST;
					} else if (this.telemetryCommand.content.indexOf("Response") != -1) {
						this.telemetryCommand.type = TelemetryMessageEventType.MESSAGE_RESPONSE;
					}
				}
			}
		}
    }


	private void determineEventType(TelemetryEvent telemetryEvent, Message message) throws JMSException {
		String messageType = message.getStringProperty(JMS_PROPERTY_KEY_EVENT_TYPE);
		if (messageType != null) {
			try {
				telemetryEvent.type = TelemetryMessageEventType.valueOf(messageType);
				return;
			} catch (IllegalArgumentException e) {
			}
		}
		int ibmMsgType = message.getIntProperty("JMS_IBM_MsgType");
		if (ibmMsgType == 1) {
			telemetryEvent.type = TelemetryMessageEventType.MESSAGE_REQUEST;
		} else if (ibmMsgType == 2) {
			telemetryEvent.type = TelemetryMessageEventType.MESSAGE_RESPONSE;
		} else if (ibmMsgType == 8) {
			telemetryEvent.type = TelemetryMessageEventType.MESSAGE_DATAGRAM;
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
