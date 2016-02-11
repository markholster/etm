package com.jecstar.etm.processor.ibmmq.handlers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TimeZone;

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
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.processor.ibmmq.event.ApplicationData.ComplexContent;
import com.jecstar.etm.processor.ibmmq.event.EncodingType;
import com.jecstar.etm.processor.ibmmq.event.Event;

public class IIBEventHandler implements MessageHandler<byte[]> {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(IIBEventHandler.class);

	private final StringBuilder byteArrayBuilder = new StringBuilder();

	private final Unmarshaller unmarshaller;

	private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
	
	public IIBEventHandler() {
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
	public boolean handleMessage(TelemetryEvent telemetryEvent, byte[] message) {
		try (Reader reader = new InputStreamReader(new ByteArrayInputStream(message));) {
			Event event = ((JAXBElement<Event>) this.unmarshaller.unmarshal(reader)).getValue();
			return copyIbmEventToTelemetryEvent(event, telemetryEvent);
		} catch (JAXBException e) {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Unable to unmarshall event.", e);
			}
		} catch (IOException e) {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Unable to close array reader.", e);
			}
		}
		return false;
	}

	private boolean copyIbmEventToTelemetryEvent(Event event, TelemetryEvent telemetryEvent) {
		putNonNullDataInMap("IIB_Node", event.getEventPointData().getMessageFlowData().getBroker().getName(), telemetryEvent.metadata);
		putNonNullDataInMap("IIB_Server", event.getEventPointData().getMessageFlowData().getExecutionGroup().getName(), telemetryEvent.metadata);
		putNonNullDataInMap("IIB_MessageFlow", event.getEventPointData().getMessageFlowData().getMessageFlow().getName(), telemetryEvent.metadata);
		putNonNullDataInMap("IIB_MessageFlowNode", event.getEventPointData().getMessageFlowData().getNode().getNodeLabel(), telemetryEvent.metadata);
		putNonNullDataInMap("IIB_MessageFlowNodeTerminal", event.getEventPointData().getMessageFlowData().getNode().getTerminal(), telemetryEvent.metadata);
		putNonNullDataInMap("IIB_MessageFlowNodeType", event.getEventPointData().getMessageFlowData().getNode().getNodeType(), telemetryEvent.metadata);
		boolean mqBitstream = false;
		boolean httpBitstream = false;
		// See https://www-01.ibm.com/support/knowledgecenter/SSMKHH_9.0.0/com.ibm.etools.mft.doc/as36001_.htm for node types.
		if ("ComIbmMQInputNode".equals(event.getEventPointData().getMessageFlowData().getNode().getNodeType())) {
			mqBitstream = true;
			telemetryEvent.endpoint = event.getEventPointData().getMessageFlowData().getNode().getDetail();
		} else if ("ComIbmMQOutputNode".equals(event.getEventPointData().getMessageFlowData().getNode().getNodeType())) {
			mqBitstream = true;
			// TODO, filteren op input terminal? Events op de in terminal van de
			// MqOutputNode hebben nog geen msg id.
			for (ComplexContent complexContent : event.getApplicationData().getComplexContent()) {
				if (!("DestinationData".equals(complexContent.getElementName())
						|| "WrittenDestination".equals(complexContent.getElementName()))) {
					continue;
				}
				NodeList queueNames = complexContent.getAny().getElementsByTagName("queueName");
				if (queueNames.getLength() > 0) {
					telemetryEvent.endpoint = queueNames.item(0).getTextContent();
					break;
				}
			}
		} else if (event.getEventPointData().getMessageFlowData().getNode().getNodeType().startsWith("ComIbmWS") || 
				event.getEventPointData().getMessageFlowData().getNode().getNodeType().startsWith("ComIbmHTTP")) {
			httpBitstream = true;
		}
		if (event.getBitstreamData() != null && event.getBitstreamData().getBitstream() != null) {
			if (!EncodingType.BASE_64_BINARY.equals(event.getBitstreamData().getBitstream().getEncoding())) {
				if (log.isWarningLevelEnabled()) {
					log.logWarningMessage("Bitstream encoding type '" + event.getBitstreamData().getBitstream().getEncoding().hashCode()
									+ "' not supported. Use '" + EncodingType.BASE_64_BINARY.name() + "' instead.");
				}
				return false;
			}
			byte[] decoded = null;
			try {
				decoded = Base64.decode(event.getBitstreamData().getBitstream().getValue());
				if (mqBitstream) {
					parseBitstreamAsMqMessage(decoded, telemetryEvent);
				} else if (httpBitstream) {
					parseBitstreamAsHttpMessage(decoded, telemetryEvent);
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
					log.logWarningMessage("Failed to parse Ibm Event bitstream.", e);
				}
				return false;
			}
			return true;
		}
		return false;
	}

	private void parseBitstreamAsMqMessage(byte[] decodedBitstream, TelemetryEvent telemetryEvent)
			throws MQDataException, IOException, ParseException {
		try (DataInputStream inputData = new DataInputStream(new ByteArrayInputStream(decodedBitstream));) {

			MQMD mqmd = new MQMD(inputData);
			putNonNullDataInMap("MQMD_CharacterSet", "" + mqmd.getCodedCharSetId(), telemetryEvent.metadata);
			putNonNullDataInMap("MQMD_Format", mqmd.getFormat(), telemetryEvent.metadata);
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
			if (ibmMsgType == CMQC.MQMT_REQUEST) {
				telemetryEvent.type = TelemetryEventType.MESSAGE_REQUEST;
			} else if (ibmMsgType == CMQC.MQMT_REPLY) {
				telemetryEvent.type = TelemetryEventType.MESSAGE_RESPONSE;
			} else if (ibmMsgType == CMQC.MQMT_DATAGRAM) {
				telemetryEvent.type = TelemetryEventType.MESSAGE_DATAGRAM;
			}
		} 
	}

	private void parseBitstreamAsHttpMessage(byte[] decoded, TelemetryEvent telemetryEvent) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(decoded)))) {
			String line = reader.readLine();
			if (line != null) {
				// First line is always the method + url + protocol version;
				String[] split = line.split(" ");
				if (split.length >= 2) {
					telemetryEvent.endpoint = split[1];
				}
			}
			line = reader.readLine();
			boolean inHeaders = true;
			while (line != null) {
				if (line.trim().length() == 0 && inHeaders) {
					inHeaders = false;
					line = reader.readLine();
					continue;
				}
				if (inHeaders) {
					int ix = line.indexOf(":");
					if (ix != -1) {
						telemetryEvent.metadata.put(line.substring(0, ix).trim(), line.substring(ix + 1).trim());
					}
				} else {
					if (telemetryEvent.content != null) {
						telemetryEvent.content += "\r\n" + line;
					} else {
						telemetryEvent.content = line;
					}
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Unable to close reader.", e);
			}
		}
	}

	private void putNonNullDataInMap(String key, String value, Map<String, String> map) {
		if (value != null && value.trim().length() > 0) {
			map.put(key, value.trim());
		}
	}

	private void putNonNullDataInMap(String key, byte[] value, Map<String, String> map) {
		if (value != null && value.length > 0) {
			putNonNullDataInMap(key, byteArrayToString(value), map);
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
