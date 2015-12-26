package com.jecstar.etm.processor.ibmmq.event;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.elasticsearch.common.Base64;
import org.w3c.dom.NodeList;

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

public class IIBEventProcessor {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(IIBEventProcessor.class);

	private EtmConfiguration configration;

	private TelemetryEventProcessor telemetryEventProcessor;

	private final TelemetryEvent telemetryEvent = new TelemetryEvent();
	private final StringBuilder byteArrayBuilder = new StringBuilder();

	private Unmarshaller unmarshaller;

//	@PostConstruct
//	void initialize() {
//		try {
//			JAXBContext jaxbContext = JAXBContext.newInstance(Event.class);
//			this.unmarshaller = jaxbContext.createUnmarshaller();
//		} catch (JAXBException e) {
//			throw new EtmException(EtmException.UNMARSHALLER_CREATE_EXCEPTION, e);
//		}
//	}

//	@Override
//	public void onMessage(Message message) {
//		try {
//			this.telemetryEvent.initialize();
//			if (message instanceof javax.jms.TextMessage) {
//				handleTextMessage((TextMessage) message);
//			} else if (message instanceof javax.jms.BytesMessage) {
//				handleByteMessage((BytesMessage) message);
//			} else {
//				if (log.isDebugLevelEnabled()) {
//					log.logDebugMessage("Message with msgid '" + message.getJMSMessageID()
//							+ "' is not a TextMessage or BytesMessage, but a '" + message.getClass().getName() + "'.");
//				}
//			}
//		} catch (Throwable t) {
//			if (log.isErrorLevelEnabled()) {
//				log.logErrorMessage(t.getMessage(), t);
//			}
//		}
//	}

//	@SuppressWarnings("unchecked")
//	private void handleByteMessage(BytesMessage byteMessage) throws JMSException {
//		if (byteMessage.getBodyLength() > Integer.MAX_VALUE - 1) {
//			if (log.isWarningLevelEnabled()) {
//				log.logWarningMessage("Message with id '" + byteMessage.getJMSMessageID() + "' is to large. Max size = "
//						+ (Integer.MAX_VALUE - 1) + ", message size = " + byteMessage.getBodyLength());
//			}
//			return;
//		}
//		byte[] body = new byte[(int) byteMessage.getBodyLength()];
//		byteMessage.readBytes(body);
//		try (Reader reader = new InputStreamReader(new ByteArrayInputStream(body));) {
//			Event event = ((JAXBElement<Event>) this.unmarshaller.unmarshal(reader)).getValue();
//			if (copyIbmEventToTelemetryEvent(event, telemetryEvent)) {
//				this.telemetryEventProcessor.processTelemetryEvent(this.telemetryEvent);
//				return;
//			}
//		} catch (JAXBException e) {
//			if (log.isDebugLevelEnabled()) {
//				log.logDebugMessage("Unable to unmarshall event.", e);
//			}
//		} catch (IOException e) {
//			if (log.isDebugLevelEnabled()) {
//				log.logDebugMessage("Unable to close array reader.", e);
//			}
//		}
//	}

//	@SuppressWarnings("unchecked")
//	private void handleTextMessage(TextMessage message) throws JMSException {
//		try (StringReader reader = new StringReader(message.getText());) {
//			Event event = ((JAXBElement<Event>) this.unmarshaller.unmarshal(reader)).getValue();
//			if (copyIbmEventToTelemetryEvent(event, telemetryEvent)) {
//				this.telemetryEventProcessor.processTelemetryEvent(this.telemetryEvent);
//				return;
//			}
//		} catch (JAXBException e) {
//			if (log.isDebugLevelEnabled()) {
//				log.logDebugMessage("Unable to unmarshall event.", e);
//			}
//		}
//	}

//	private boolean copyIbmEventToTelemetryEvent(Event event, TelemetryEvent telemetryEvent) {
//		putNonNullDataInMap("IIB_Node", event.eventPointData.messageFlowData.broker.name, telemetryEvent.metadata);
//		putNonNullDataInMap("IIB_Server", event.eventPointData.messageFlowData.executionGroup.name,
//				telemetryEvent.metadata);
//		putNonNullDataInMap("IIB_MessageFlow", event.eventPointData.messageFlowData.messageFlow.name,
//				telemetryEvent.metadata);
//		putNonNullDataInMap("IIB_MessageFlowNode", event.eventPointData.messageFlowData.node.nodeLabel,
//				telemetryEvent.metadata);
//		putNonNullDataInMap("IIB_MessageFlowNodeTerminal", event.eventPointData.messageFlowData.node.terminal,
//				telemetryEvent.metadata);
//		putNonNullDataInMap("IIB_MessageFlowNodeType", event.eventPointData.messageFlowData.node.nodeType,
//				telemetryEvent.metadata);
//		boolean mqBitstream = false;
//		boolean httpBitstream = false;
//		// See https://www-01.ibm.com/support/knowledgecenter/SSMKHH_9.0.0/com.ibm.etools.mft.doc/as36001_.htm for node types.
//		if ("ComIbmMQInputNode".equals(event.eventPointData.messageFlowData.node.nodeType)) {
//			mqBitstream = true;
//			telemetryEvent.endpoint = event.eventPointData.messageFlowData.node.detail;
//		} else if ("ComIbmMQOutputNode".equals(event.eventPointData.messageFlowData.node.nodeType)) {
//			mqBitstream = true;
//			// TODO, filteren op input terminal? Events op de in terminal van de
//			// MqOutputNode hebben nog geen msg id.
//			for (ComplexContent complexContent : event.applicationData.complexContent) {
//				if (!("DestinationData".equals(complexContent.elementName)
//						|| "WrittenDestination".equals(complexContent.elementName))) {
//					continue;
//				}
//				NodeList queueNames = complexContent.any.getElementsByTagName("queueName");
//				if (queueNames.getLength() > 0) {
//					telemetryEvent.endpoint = queueNames.item(0).getTextContent();
//					break;
//				}
//			}
//		} else if (event.eventPointData.messageFlowData.node.nodeType.startsWith("ComIbmWS") || 
//				event.eventPointData.messageFlowData.node.nodeType.startsWith("ComIbmHTTP")) {
//			httpBitstream = true;
//		}
//		if (event.bitstreamData != null && event.bitstreamData.bitstream != null) {
//			if (!EncodingType.BASE_64_BINARY.equals(event.bitstreamData.bitstream.getEncoding())) {
//				if (log.isWarningLevelEnabled()) {
//					log.logWarningMessage(
//							"Bitstream encoding type '" + event.bitstreamData.bitstream.getEncoding().hashCode()
//									+ "' not supported. Use '" + EncodingType.BASE_64_BINARY.name() + "' instead.");
//				}
//				return false;
//			}
//			byte[] decoded = null;
//			try {
//				decoded = Base64.decode(event.bitstreamData.bitstream.value);
//				if (mqBitstream) {
//					parseBitstreamAsMqMessage(decoded, telemetryEvent);
//				} else if (httpBitstream) {
//					parseBitstreamAsHttpMessage(decoded, telemetryEvent);
//				}
//			} catch (IOException e) {
//				if (log.isWarningLevelEnabled()) {
//					log.logWarningMessage("Failed to decode Ibm Event bitstream.", e);
//				}
//				return false;
//			} catch (MQDataException e) {
//				if (log.isWarningLevelEnabled()) {
//					log.logWarningMessage("Failed to parse Ibm Event bitstream.", e);
//				}
//				return false;
//			}
//			return true;
//		}
//		return false;
//	}

//	private void parseBitstreamAsMqMessage(byte[] decodedBitstream, TelemetryEvent telemetryEvent)
//			throws MQDataException, IOException {
//		try (DataInputStream inputData = new DataInputStream(new ByteArrayInputStream(decodedBitstream));) {
//
//			MQMD mqmd = new MQMD(inputData);
//			putNonNullDataInMap("MQMD_CharacterSet", "" + mqmd.getCodedCharSetId(), telemetryEvent.metadata);
//			putNonNullDataInMap("MQMD_Format", mqmd.getFormat(), telemetryEvent.metadata);
//			putNonNullDataInMap("MQMD_Encoding", "" + mqmd.getEncoding(), telemetryEvent.metadata);
//			putNonNullDataInMap("MQMD_AccountingToken", mqmd.getAccountingToken(), telemetryEvent.metadata);
//			putNonNullDataInMap("MQMD_Persistence", "" + mqmd.getPersistence(), telemetryEvent.metadata);
//			putNonNullDataInMap("MQMD_Priority", "" + mqmd.getPriority(), telemetryEvent.metadata);
//			putNonNullDataInMap("MQMD_ReplyToQueueManager", mqmd.getReplyToQMgr(), telemetryEvent.metadata);
//			putNonNullDataInMap("MQMD_ReplyToQueue", mqmd.getReplyToQ(), telemetryEvent.metadata);
//			putNonNullDataInMap("MQMD_BackoutCount", "" + mqmd.getBackoutCount(), telemetryEvent.metadata);
//			if (mqmd.getFormat().equals(MQConstants.MQFMT_RF_HEADER_2)) {
//				new MQRFH2(inputData, mqmd.getEncoding(), mqmd.getCodedCharSetId());
//				// TODO Do something with RFH2 header?
//			}
//			String codepage = CCSID.getCodepage(mqmd.getCodedCharSetId());
//			byte[] remaining = new byte[inputData.available()];
//			inputData.readFully(remaining);
//
//			telemetryEvent.content = new String(remaining, codepage);
//			telemetryEvent.id = byteArrayToString(mqmd.getMsgId());
//			if (mqmd.getCorrelId() != null && mqmd.getCorrelId().length > 0) {
//				telemetryEvent.correlationId = byteArrayToString(mqmd.getCorrelId());
//			}
//			telemetryEvent.creationTime.setTime(mqmd.getPutDateTime());
//			if (CMQC.MQEI_UNLIMITED != mqmd.getExpiry()) {
//				telemetryEvent.expiryTime.setTime(mqmd.getPutDateTime() + (mqmd.getExpiry() * 100));
//			}
//			int ibmMsgType = mqmd.getMsgType();
//			if (ibmMsgType == 1) {
//				telemetryEvent.type = TelemetryEventType.MESSAGE_REQUEST;
//			} else if (ibmMsgType == 2) {
//				telemetryEvent.type = TelemetryEventType.MESSAGE_RESPONSE;
//			} else if (ibmMsgType == 8) {
//				telemetryEvent.type = TelemetryEventType.MESSAGE_DATAGRAM;
//			}
//		} 
//	}
//
//	private void parseBitstreamAsHttpMessage(byte[] decoded, TelemetryEvent telemetryEvent) {
//		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(decoded)))) {
//			String line = reader.readLine();
//			if (line != null) {
//				// First line is always the method + url + protocol version;
//				String[] split = line.split(" ");
//				if (split.length >= 2) {
//					telemetryEvent.endpoint = split[1];
//				}
//			}
//			line = reader.readLine();
//			boolean inHeaders = true;
//			while (line != null) {
//				if (line.trim().length() == 0 && inHeaders) {
//					inHeaders = false;
//					line = reader.readLine();
//					continue;
//				}
//				if (inHeaders) {
//					int ix = line.indexOf(":");
//					if (ix != -1) {
//						telemetryEvent.metadata.put(line.substring(0, ix).trim(), line.substring(ix + 1).trim());
//					}
//				} else {
//					if (telemetryEvent.content != null) {
//						telemetryEvent.content += "\r\n" + line;
//					} else {
//						telemetryEvent.content = line;
//					}
//				}
//				line = reader.readLine();
//			}
//		} catch (IOException e) {
//			if (log.isDebugLevelEnabled()) {
//				log.logDebugMessage("Unable to close reader.", e);
//			}
//		}
//	}

//	private void putNonNullDataInMap(String key, String value, Map<String, String> map) {
//		if (value != null && value.trim().length() > 0) {
//			map.put(key, value.trim());
//		}
//	}
//
//	private void putNonNullDataInMap(String key, byte[] value, Map<String, String> map) {
//		if (value != null && value.length > 0) {
//			putNonNullDataInMap(key, byteArrayToString(value), map);
//		}
//	}
//
//	private String byteArrayToString(byte[] bytes) {
//		this.byteArrayBuilder.setLength(0);
//		boolean allZero = true;
//		for (int i = 0; i < bytes.length; i++) {
//			this.byteArrayBuilder.append(String.format("%02x", bytes[i]));
//			if (bytes[i] != 0) {
//				allZero = false;
//			}
//		}
//		return allZero ? null : this.byteArrayBuilder.toString();
//	}

}
