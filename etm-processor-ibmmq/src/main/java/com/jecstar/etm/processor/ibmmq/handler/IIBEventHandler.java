package com.jecstar.etm.processor.ibmmq.handler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.w3c.dom.NodeList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.CCSID;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.MQMD;
import com.ibm.mq.headers.MQRFH2;
import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.domain.builders.ApplicationBuilder;
import com.jecstar.etm.domain.builders.BusinessTelemetryEventBuilder;
import com.jecstar.etm.domain.builders.EndpointBuilder;
import com.jecstar.etm.domain.builders.EndpointHandlerBuilder;
import com.jecstar.etm.domain.builders.HttpTelemetryEventBuilder;
import com.jecstar.etm.domain.builders.LogTelemetryEventBuilder;
import com.jecstar.etm.domain.builders.MessagingTelemetryEventBuilder;
import com.jecstar.etm.domain.builders.SqlTelemetryEventBuilder;
import com.jecstar.etm.domain.builders.TelemetryEventBuilder;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.TelemetryCommand.CommandType;
import com.jecstar.etm.processor.ibmmq.event.ApplicationData.ComplexContent;
import com.jecstar.etm.processor.ibmmq.event.ApplicationData.SimpleContent;
import com.jecstar.etm.processor.ibmmq.event.EncodingType;
import com.jecstar.etm.processor.ibmmq.event.Event;
import com.jecstar.etm.processor.ibmmq.event.SimpleContentDataType;
import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

public class IIBEventHandler {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(IIBEventHandler.class);

	private final TelemetryCommandProcessor telemetryCommandProcessor;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final JsonConverter jsonConverter = new JsonConverter();
	
	private final StringBuilder byteArrayBuilder = new StringBuilder();
	private final Unmarshaller unmarshaller;
	private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
	
	private final BusinessTelemetryEventBuilder businessTelemetryEventBuilder = new BusinessTelemetryEventBuilder(); 
	private final HttpTelemetryEventBuilder httpTelemetryEventBuilder = new HttpTelemetryEventBuilder(); 
	private final LogTelemetryEventBuilder logTelemetryEventBuilder = new LogTelemetryEventBuilder(); 
	private final MessagingTelemetryEventBuilder messagingTelemetryEventBuilder = new MessagingTelemetryEventBuilder(); 
	
	public IIBEventHandler(TelemetryCommandProcessor telemetryCommandProcessor) {
		this.telemetryCommandProcessor = telemetryCommandProcessor;
		this.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Event.class);
			this.unmarshaller = jaxbContext.createUnmarshaller();
		} catch (JAXBException e) {
			throw new EtmException(EtmException.UNMARSHALLER_CREATE_EXCEPTION, e);
		}
	}

	@SuppressWarnings("unchecked")
	public HandlerResult handleMessage(byte[] messageId, byte[] message) {
		try (Reader reader = new InputStreamReader(new ByteArrayInputStream(message));) {
			Event event = ((JAXBElement<Event>) this.unmarshaller.unmarshal(reader)).getValue();
			return process(event);
		} catch (JAXBException | IOException e) {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Unable to unmarshall event.", e);
			}
			return HandlerResult.PARSE_FAILURE;
		}
	}

	@SuppressWarnings("unchecked")
	private HandlerResult process(Event event) {
		// We use the event name field as it is the only field that can be set with hard values. All other fields can only be set with xpath values from the message payload. 
		String nodeType = event.getEventPointData().getMessageFlowData().getNode().getNodeType();
		if (nodeType == null) {
			return HandlerResult.FAILED;
		}
		// See https://www.ibm.com/support/knowledgecenter/SSMKHH_10.0.0/com.ibm.etools.mft.doc/as36001_.htm for node types.
		if (nodeType.startsWith("ComIbmMQ")) {
			return processAsMessagingEvent(event);
		} else if (nodeType.startsWith("ComIbmHTTP") ||
				nodeType.startsWith("ComIbmWS") ||
				(nodeType.startsWith("ComIbmSOAP") && !nodeType.equals("ComIbmSOAPWrapperNode") && !nodeType.equals("ComIbmSOAPExtractNode"))) {
			return processAsHttpEvent(event);
		} else {
			return HandlerResult.FAILED;
		}
		
		
		telemetryEvent.creationTime.setTime(event.getEventPointData().getEventData().getEventSequence().getCreationTime().toGregorianCalendar().getTimeInMillis());
		putNonNullDataInMap("IIB_LocalTransactionId", event.getEventPointData().getEventData().getEventCorrelation().getLocalTransactionId(), telemetryEvent.correlationData);
		putNonNullDataInMap("IIB_ParentTransactionId", event.getEventPointData().getEventData().getEventCorrelation().getParentTransactionId(), telemetryEvent.correlationData);
		putNonNullDataInMap("IIB_GlobalTransactionId", event.getEventPointData().getEventData().getEventCorrelation().getGlobalTransactionId(), telemetryEvent.correlationData);
		
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
			// TODO, filteren op output terminal? Events op de in terminal van de
			// MqOutputNode hebben nog geen msg id.
			if (event.getApplicationData() != null && event.getApplicationData().getComplexContent() != null) {
				for (ComplexContent complexContent : event.getApplicationData().getComplexContent()) {
					if (!("DestinationData".equals(complexContent.getElementName())
							|| "WrittenDestination".equals(complexContent.getElementName()))) {
						continue;
					}
					NodeList nodeList = complexContent.getAny().getElementsByTagName("queueName");
					if (nodeList.getLength() > 0) {
						telemetryEvent.endpoint = nodeList.item(0).getTextContent() != null ? nodeList.item(0).getTextContent().trim() : null;
					}
					nodeList = complexContent.getAny().getElementsByTagName("msgId");
					if (nodeList.getLength() > 0) {
						telemetryEvent.id = nodeList.item(0).getTextContent();
					}
					nodeList = complexContent.getAny().getElementsByTagName("correlId");
					if (nodeList.getLength() > 0) {
						String correlId = nodeList.item(0).getTextContent();
						if (correlId.replaceAll("0", "").trim().length() != 0) {
							telemetryEvent.correlationId = correlId;
						}
					}
				}
			}
		} else if (event.getEventPointData().getMessageFlowData().getNode().getNodeType().startsWith("ComIbmWS") || 
				event.getEventPointData().getMessageFlowData().getNode().getNodeType().startsWith("ComIbmHTTP")) {
			httpBitstream = true;
		}
		if (customAchmeaFiltering(telemetryEvent)) {
			return false;
		}
		NodeType nodeType = NodeType.nullSafeValueOf(event.getEventPointData().getMessageFlowData().getNode().getNodeType());
		if (nodeType != null && nodeType.getEventType() != null && telemetryEvent.type == null) {
			telemetryEvent.type = nodeType.getEventType();
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
				decoded = Base64.getDecoder().decode(event.getBitstreamData().getBitstream().getValue());
				if (mqBitstream) {
					parseBitstreamAsMqMessage(decoded, telemetryEvent, encoding, ccsid);
				} else if (httpBitstream) {
					parseBitstreamAsHttpMessage(decoded, telemetryEvent, encoding, ccsid);
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

	@SuppressWarnings("unchecked")
	private HandlerResult processAsMessagingEvent(Event event) {
		this.messagingTelemetryEventBuilder.initialize();
		int encoding = -1;
		int ccsid = -1;
		// Determine the encoding & ccsid based on values from the event.
		if (event.getApplicationData() != null && event.getApplicationData().getSimpleContent() != null) {
			for (SimpleContent simpleContent : event.getApplicationData().getSimpleContent()) {
				if ("Encoding".equals(simpleContent.getName()) && SimpleContentDataType.INTEGER.equals(simpleContent.getDataType())) {
					encoding = Integer.valueOf(simpleContent.getValue());
				} else if ("CodedCharSetId".equals(simpleContent.getName()) && SimpleContentDataType.INTEGER.equals(simpleContent.getDataType())) {
					ccsid = Integer.valueOf(simpleContent.getValue());
				} 
			}
		}	
	
		EndpointBuilder endpointBuilder = new EndpointBuilder();
		// TODO, filteren op output terminal? Events op de in terminal van de MqOutputNode hebben nog geen msg id.
		if (event.getApplicationData() != null && event.getApplicationData().getComplexContent() != null) {
			for (ComplexContent complexContent : event.getApplicationData().getComplexContent()) {
				if (!("DestinationData".equals(complexContent.getElementName())
						|| "WrittenDestination".equals(complexContent.getElementName()))) {
					continue;
				}
				NodeList nodeList = complexContent.getAny().getElementsByTagName("queueName");
				if (nodeList.getLength() > 0) {
					endpointBuilder.setName(nodeList.item(0).getTextContent() != null ? nodeList.item(0).getTextContent().trim() : null);
				}
				nodeList = complexContent.getAny().getElementsByTagName("msgId");
				if (nodeList.getLength() > 0) {
					this.messagingTelemetryEventBuilder.setId(nodeList.item(0).getTextContent());
				}
				nodeList = complexContent.getAny().getElementsByTagName("correlId");
				if (nodeList.getLength() > 0) {
					String correlId = nodeList.item(0).getTextContent();
					if (correlId.replaceAll("0", "").trim().length() != 0) {
						this.messagingTelemetryEventBuilder.setCorrelationId(correlId);
					}
				}
			}
		}
		// Add some flow information
		addSourceInformation(event, this.messagingTelemetryEventBuilder);
		EndpointHandlerBuilder endpointHandlerBuilder = createEndpointHandlerBuilder(event);
		String nodeType = event.getEventPointData().getMessageFlowData().getNode().getNodeType();
		if ("ComIbmMQInputNode".equals(nodeType) || "ComIbmMQGetNode".equals(nodeType)) {
			endpointBuilder.addReadingEndpointHandler(endpointHandlerBuilder);
		} else {
			endpointBuilder.setWritingEndpointHandler(endpointHandlerBuilder);
		}
		this.messagingTelemetryEventBuilder.addOrMergeEndpoint(endpointBuilder);
		if (event.getBitstreamData() == null|| event.getBitstreamData().getBitstream() == null) {
			return HandlerResult.FAILED;
		}
		if (!EncodingType.BASE_64_BINARY.equals(event.getBitstreamData().getBitstream().getEncoding())) {
			if (log.isWarningLevelEnabled()) {
				log.logWarningMessage("Bitstream encoding type '" + event.getBitstreamData().getBitstream().getEncoding().hashCode()
								+ "' not supported. Use '" + EncodingType.BASE_64_BINARY.name() + "' instead.");
			}
			return HandlerResult.FAILED;
		}
		byte[] decoded = Base64.getDecoder().decode(event.getBitstreamData().getBitstream().getValue());
		try {
			parseBitstreamAsMqMessage(decoded, this.messagingTelemetryEventBuilder, encoding, ccsid);
		} catch (MQDataException | IOException e) {
			return HandlerResult.FAILED;
		}	
		this.telemetryCommandProcessor.processTelemetryEvent(this.messagingTelemetryEventBuilder);
		return HandlerResult.PROCESSED;
	}
	
	private HandlerResult processAsHttpEvent(Event event) {
		this.httpTelemetryEventBuilder.initialize();
		return HandlerResult.PROCESSED;
	}
	
	private void addSourceInformation(Event event, TelemetryEventBuilder<?, ?> builder) {
		putNonNullDataInMap("IIB_LocalTransactionId", event.getEventPointData().getEventData().getEventCorrelation().getLocalTransactionId(), builder.getCorrelationData());
		putNonNullDataInMap("IIB_ParentTransactionId", event.getEventPointData().getEventData().getEventCorrelation().getParentTransactionId(), builder.getCorrelationData());
		putNonNullDataInMap("IIB_GlobalTransactionId", event.getEventPointData().getEventData().getEventCorrelation().getGlobalTransactionId(), builder.getCorrelationData());
		putNonNullDataInMap("IIB_Node", event.getEventPointData().getMessageFlowData().getBroker().getName(), builder.getMetadata());
		putNonNullDataInMap("IIB_Server", event.getEventPointData().getMessageFlowData().getExecutionGroup().getName(), builder.getMetadata());
		putNonNullDataInMap("IIB_MessageFlow", event.getEventPointData().getMessageFlowData().getMessageFlow().getName(), builder.getMetadata());
		putNonNullDataInMap("IIB_MessageFlowNode", event.getEventPointData().getMessageFlowData().getNode().getNodeLabel(), builder.getMetadata());
		putNonNullDataInMap("IIB_MessageFlowNodeTerminal", event.getEventPointData().getMessageFlowData().getNode().getTerminal(), builder.getMetadata());
		putNonNullDataInMap("IIB_MessageFlowNodeType", event.getEventPointData().getMessageFlowData().getNode().getNodeType(), builder.getMetadata());		
	}
	
	@SuppressWarnings("unchecked")
	private EndpointHandlerBuilder createEndpointHandlerBuilder(Event event) {
		// Try to parse some metadata that can be "hidden" in the event name.
		String eventData = event.getEventPointData().getEventData().getEventIdentity().getEventName();
		HashMap<String, Object> eventMetaData = new HashMap<>();
		try {
			eventMetaData = this.objectMapper.readValue(eventData, HashMap.class);
		} catch (IOException e) {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Event name does not contain ETM metadata.");
			}
		}
		EndpointHandlerBuilder builder = new EndpointHandlerBuilder();
		long epochMillis = event.getEventPointData().getEventData().getEventSequence().getCreationTime().toGregorianCalendar().getTimeInMillis();
		builder.setHandlingTime(ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC));
		String application = this.jsonConverter.getString("application", eventMetaData);
		String version = this.jsonConverter.getString("version", eventMetaData);
		if (application == null) {
			String productVersion = event.getEventPointData().getEventData().getProductVersion();
			if (productVersion.startsWith("6") || productVersion.startsWith("7") || productVersion.startsWith("8")) {
				application = "WMB";
			} else {
				application = "IIB";
			}
			version = productVersion;
		}
		builder.setApplication(new ApplicationBuilder().setName(application).setVersion(version));
		builder.setTransactionId(event.getEventPointData().getEventData().getEventCorrelation().getLocalTransactionId());
		return builder;
	}

	private void parseBitstreamAsMqMessage(byte[] decodedBitstream, MessagingTelemetryEventBuilder builder, int encoding, int ccsid)
			throws MQDataException, IOException {
		if (decodedBitstream[0] == 77 && decodedBitstream[1] == 68) {
			try (DataInputStream inputData = new DataInputStream(new ByteArrayInputStream(decodedBitstream));) {
				MQMD mqmd = new MQMD(inputData);
				putNonNullDataInMap("MQMD_CharacterSet", "" + mqmd.getCodedCharSetId(), builder.getMetadata());
				putNonNullDataInMap("MQMD_Format", mqmd.getFormat() != null ? mqmd.getFormat().trim() : null, builder.getMetadata());
				putNonNullDataInMap("MQMD_Encoding", "" + mqmd.getEncoding(), builder.getMetadata());
				putNonNullDataInMap("MQMD_AccountingToken", mqmd.getAccountingToken(), builder.getMetadata());
				putNonNullDataInMap("MQMD_Persistence", "" + mqmd.getPersistence(), builder.getMetadata());
				putNonNullDataInMap("MQMD_Priority", "" + mqmd.getPriority(), builder.getMetadata());
				putNonNullDataInMap("MQMD_ReplyToQueueManager", mqmd.getReplyToQMgr() != null ? mqmd.getReplyToQMgr().trim() : null, builder.getMetadata());
				putNonNullDataInMap("MQMD_ReplyToQueue", mqmd.getReplyToQ() != null ? mqmd.getReplyToQ().trim() : null, builder.getMetadata());
				putNonNullDataInMap("MQMD_BackoutCount", "" + mqmd.getBackoutCount(), builder.getMetadata());
				if (mqmd.getFormat().equals(MQConstants.MQFMT_RF_HEADER_2)) {
					new MQRFH2(inputData, mqmd.getEncoding(), mqmd.getCodedCharSetId());
					// TODO Do something with RFH2 header?
				}
				String codepage = CCSID.getCodepage(mqmd.getCodedCharSetId());
				byte[] remaining = new byte[inputData.available()];
				inputData.readFully(remaining);
	
				builder.setPayload(new String(remaining, codepage));
				if (builder.getId() == null) {
					// Event can be set earlier in case of ComIbmMQOutputNode, in that case we have to skip the set because it may fail.
					builder.setId(byteArrayToString(mqmd.getMsgId()));
				}
				if (builder.getCorrelationId() == null) {
					// Event can be set earlier in case of ComIbmMQOutputNode, in that case we have to skip the set because it may fail. s	
					if (mqmd.getCorrelId() != null && mqmd.getCorrelId().length > 0) {
						builder.setCorrelationId(byteArrayToString(mqmd.getCorrelId()));
					}
				}
				if (CMQC.MQEI_UNLIMITED != mqmd.getExpiry()) {
					try {
						long expiryTime = this.dateFormat.parse(mqmd.getPutDate() + mqmd.getPutTime()).getTime() + (mqmd.getExpiry() * 100);
						telemetryEvent.expiryTime.setTime(expiryTime);
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				int ibmMsgType = mqmd.getMsgType();
				if (ibmMsgType == CMQC.MQMT_REQUEST) {
					builder.setMessagingEventType(MessagingEventType.REQUEST);
				} else if (ibmMsgType == CMQC.MQMT_REPLY) {
					builder.setMessagingEventType(MessagingEventType.RESPONSE);
				} else if (ibmMsgType == CMQC.MQMT_DATAGRAM) {
					builder.setMessagingEventType(MessagingEventType.FIRE_FORGET);
				}
			}
		}
	}

	private void parseBitstreamAsHttpMessage(byte[] decoded, TelemetryEvent telemetryEvent, int encoding, int ccsid) throws UnsupportedEncodingException {
		String codepage = null;
		if (ccsid != -1) {
			codepage = CCSID.getCodepage(ccsid);
		}
		try (BufferedReader reader = new BufferedReader(codepage == null ? new InputStreamReader(new ByteArrayInputStream(decoded)) : new InputStreamReader(new ByteArrayInputStream(decoded), codepage))) {
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
						telemetryEvent.metadata.put("http_" + line.substring(0, ix).trim(), line.substring(ix + 1).trim());
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
	
	private void putNonNullDataInMap(String key, String value, Map<String, Object> map) {
		if (value != null && value.trim().length() > 0) {
			map.put(key, value.trim());
		}
	}

	private void putNonNullDataInMap(String key, byte[] value, Map<String, Object> map) {
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
