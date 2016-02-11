package com.jecstar.etm.processor.mdb.ibm;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jms.BytesMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.elasticsearch.common.Base64;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.CCSID;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.MQMD;
import com.ibm.mq.headers.MQRFH2;
import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.TelemetryEvent;
import com.jecstar.etm.core.TelemetryEventType;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.jee.configurator.core.ProcessorConfiguration;
import com.jecstar.etm.processor.processor.TelemetryEventProcessor;

public class IIBEventProcessor implements MessageListener {

	/**
	 * 
	 * The <code>LogWrapper</code> for this class.
	 * 
	 */
	private static final LogWrapper log = LogFactory.getLogger(IIBEventProcessor.class);

	@ProcessorConfiguration
	@Inject
	private EtmConfiguration configration;

	@Inject
	@ProcessorConfiguration
	private TelemetryEventProcessor telemetryEventProcessor;

	private final TelemetryEvent telemetryEvent = new TelemetryEvent();

	private Unmarshaller unmarshaller;

	private final StringBuilder byteArrayBuilder = new StringBuilder();
	
	private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

	@PostConstruct
	private void initialize() {
		this.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Event.class);
			this.unmarshaller = jaxbContext.createUnmarshaller();
		} catch (JAXBException e) {
			throw new EtmException(EtmException.UNMARSHALLER_CREATE_EXCEPTION, e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onMessage(Message message) {
		try {
			this.telemetryEvent.initialize();
			if (message instanceof javax.jms.TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				try (StringReader reader = new StringReader(textMessage.getText());) {
					JAXBElement<Event> jaxbEvent = (JAXBElement<Event>) this.unmarshaller.unmarshal(reader);
					Event event = jaxbEvent.getValue();
					if (!copyIbmEventToTelemetryEvent(event, telemetryEvent)) {
						return;
					}
				} catch (JAXBException e) {
					if (log.isDebugLevelEnabled()) {
						log.logDebugMessage("Unable to unmarshall event.", e);
					}
					return;
				}
				this.telemetryEventProcessor.processTelemetryEvent(this.telemetryEvent);
			} else if (message instanceof javax.jms.BytesMessage) {
				BytesMessage byteMessage = (BytesMessage) message;
				long bodyLength = byteMessage.getBodyLength();
				if (bodyLength > Integer.MAX_VALUE) {
					if (log.isDebugLevelEnabled()) {
						log.logDebugMessage("Messag to large to handle");
					}					
					return;
				}
				byte[] bodyBytes = new byte[(int) bodyLength];
				byteMessage.readBytes(bodyBytes);
				try (StringReader reader = new StringReader(new String(bodyBytes));) {
					JAXBElement<Event> jaxbEvent = (JAXBElement<Event>) this.unmarshaller.unmarshal(reader);
					Event event = jaxbEvent.getValue();
					if (!copyIbmEventToTelemetryEvent(event, telemetryEvent)) {
						return;
					}
				} catch (JAXBException e) {
					if (log.isDebugLevelEnabled()) {
						log.logDebugMessage("Unable to unmarshall event.", e);
					}
					return;
				}
				this.telemetryEventProcessor.processTelemetryEvent(this.telemetryEvent);
			} else {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Message with msgid '" + message.getJMSMessageID()
							+ "' is not a TextMessage, but a '" + message.getClass().getName() + "'.");
				}
			}
		} catch (Throwable t) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage(t.getMessage(), t);
			}
		}
	}

	private boolean copyIbmEventToTelemetryEvent(Event event, TelemetryEvent telemetryEvent) {
		telemetryEvent.endpoint = event.eventPointData.messageFlowData.node.detail;
		if ("ESB.MSG.ESL".equals(telemetryEvent.endpoint) || "ESB.DTG.STA".equals(telemetryEvent.endpoint)) {
			// TODO achmea maatwerk. Configuratie hier uitlezen.
			return false;
		}
		putNonNullDataInMap("IIB_Node", event.eventPointData.messageFlowData.broker.name, telemetryEvent.metadata);
		putNonNullDataInMap("IIB_Server", event.eventPointData.messageFlowData.executionGroup.name, telemetryEvent.metadata);
		putNonNullDataInMap("IIB_MessageFlow", event.eventPointData.messageFlowData.messageFlow.name, telemetryEvent.metadata);
		putNonNullDataInMap("IIB_MessageFlowNode", event.eventPointData.messageFlowData.node.nodeLabel, telemetryEvent.metadata);
		putNonNullDataInMap("IIB_MessageFlowNodeTerminal", event.eventPointData.messageFlowData.node.terminal, telemetryEvent.metadata);
		putNonNullDataInMap("IIB_MessageFlowNodeType", event.eventPointData.messageFlowData.node.nodeType, telemetryEvent.metadata);
		if (event.bitstreamData != null && event.bitstreamData.bitstream != null 
				&& EncodingType.BASE_64_BINARY.equals(event.bitstreamData.bitstream.getEncoding())) {
			byte[] decoded = null;
			try {
				decoded = Base64.decode(event.bitstreamData.bitstream.value);
				DataInputStream inputData = new DataInputStream(new ByteArrayInputStream(decoded));
				MQMD mqmd = new MQMD(inputData);
				putNonNullDataInMap("MQMD_CharacterSet", "" + mqmd.getCodedCharSetId(), telemetryEvent.metadata);
				putNonNullDataInMap("MQMD_Format", mqmd.getFormat() != null ? mqmd.getFormat().trim() : null, telemetryEvent.metadata);
				putNonNullDataInMap("MQMD_Encoding", "" + mqmd.getEncoding(), telemetryEvent.metadata);
				putNonNullDataInMap("MQMD_AccountingToken", mqmd.getAccountingToken(), telemetryEvent.metadata);
				putNonNullDataInMap("MQMD_Persistence", "" + mqmd.getPersistence(), telemetryEvent.metadata);
				putNonNullDataInMap("MQMD_Priority", "" + mqmd.getPriority(), telemetryEvent.metadata);
				putNonNullDataInMap("MQMD_ReplyToQueueManager", mqmd.getReplyToQMgr() != null ? mqmd.getReplyToQMgr().trim() : null, telemetryEvent.metadata);
				putNonNullDataInMap("MQMD_ReplyToQueue", mqmd.getReplyToQ() != null ? mqmd.getReplyToQ().trim() : null, telemetryEvent.metadata);
				putNonNullDataInMap("MQMD_BackoutCount", "" + mqmd.getBackoutCount(), telemetryEvent.metadata);
				if (mqmd.getFormat().equals(MQConstants.MQFMT_RF_HEADER_2)) {
					new MQRFH2(inputData, mqmd.getEncoding(), mqmd.getCodedCharSetId());
					// TODO Do something with RFH2 header?
				}
				String codepage = CCSID.getCodepage(mqmd.getCodedCharSetId());
				byte[] remaining = new byte[inputData.available()];
				inputData.readFully(remaining);
				telemetryEvent.content = new String(remaining, codepage);
				telemetryEvent.id = byteArrayToString(mqmd.getMsgId());
				if (mqmd.getCorrelId() != null && mqmd.getCorrelId().length > 0) {
					telemetryEvent.correlationId = byteArrayToString(mqmd.getCorrelId());
				}
				telemetryEvent.creationTime.setTime(this.dateFormat.parse(mqmd.getPutDate() + mqmd.getPutTime()).getTime());
				if (CMQC.MQEI_UNLIMITED != mqmd.getExpiry()) {
					telemetryEvent.expiryTime.setTime(telemetryEvent.creationTime.getTime() + (mqmd.getExpiry() * 100));
				}
				int ibmMsgType = mqmd.getMsgType();
				if (ibmMsgType == 1) {
					telemetryEvent.type = TelemetryEventType.MESSAGE_REQUEST;
				} else if (ibmMsgType == 2) {
					telemetryEvent.type = TelemetryEventType.MESSAGE_RESPONSE;
				} else if (ibmMsgType == 8) {
					telemetryEvent.type = TelemetryEventType.MESSAGE_DATAGRAM;
				}
			} catch (IOException e) {
				if (log.isWarningLevelEnabled()) {
					log.logWarningMessage("Failed to decode Ibm Event bitstream.", e);
				}
				return false;
			} catch (MQDataException e) {
				if (log.isWarningLevelEnabled()) {
					log.logWarningMessage("Failed to parse Ibm Event bitstream.", e);
				}
				return false;
			} catch (ParseException e) {
				if (log.isWarningLevelEnabled()) {
					log.logWarningMessage("Failed to decode Ibm Event bitstream.", e);
				}
				return false;
			}
			return true;
		}
		return false;
	}

	private void putNonNullDataInMap(String key, String value, Map<String, String> map) {
		if (value != null && value.trim().length() > 0) {
			map.put(key, value);
		}
	}
	
	private void putNonNullDataInMap(String key, byte[] value, Map<String, String> map) {
		if (value != null && value.length > 0) {
			map.put(key, byteArrayToString(value));
		}
	}

	private String byteArrayToString(byte[] bytes) {
		this.byteArrayBuilder.setLength(0);
		boolean allZero = true;
		for (int i = 0; i < bytes.length; i++) {
			this.byteArrayBuilder.append(String.format("%02x", bytes[i]));
			if (bytes[i] != 0) {
				allZero = false;
			}
		}
		return allZero ? null : this.byteArrayBuilder.toString();
	}

}
