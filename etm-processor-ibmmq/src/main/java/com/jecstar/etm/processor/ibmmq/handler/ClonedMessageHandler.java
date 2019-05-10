package com.jecstar.etm.processor.ibmmq.handler;

import com.ibm.mq.MQMessage;
import com.ibm.mq.constants.CMQC;
import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.builder.EndpointBuilder;
import com.jecstar.etm.domain.builder.EndpointHandlerBuilder;
import com.jecstar.etm.domain.builder.MessagingTelemetryEventBuilder;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.handler.HandlerResult;
import com.jecstar.etm.processor.handler.HandlerResults;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

public class ClonedMessageHandler extends AbstractMQEventHandler {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(ClonedMessageHandler.class);


    private final StringBuilder byteArrayBuilder = new StringBuilder();
    private final TelemetryCommandProcessor telemetryCommandProcessor;
    private final MessagingTelemetryEventBuilder messagingTelemetryEventBuilder = new MessagingTelemetryEventBuilder();


    public ClonedMessageHandler(TelemetryCommandProcessor telemetryCommandProcessor, String defaultImportProfile) {
        super(defaultImportProfile);
        this.telemetryCommandProcessor = telemetryCommandProcessor;
    }

    @Override
    protected TelemetryCommandProcessor getProcessor() {
        return this.telemetryCommandProcessor;
    }

    public HandlerResults handleMessage(MQMessage message) {
        HandlerResults results = new HandlerResults();
        this.messagingTelemetryEventBuilder.initialize();
        try {
            parseMessage(message);
            this.telemetryCommandProcessor.processTelemetryEvent(this.messagingTelemetryEventBuilder, getDefaultImportProfile());
            results.addHandlerResult(HandlerResult.processed());
        } catch (IOException e) {
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("Unable to process content.", e);
            }
            results.addHandlerResult(HandlerResult.parserFailure(e));
        }
        return results;
    }


    private void parseMessage(MQMessage message) throws IOException {
        EndpointHandlerBuilder endpointHandlerBuilder = new EndpointHandlerBuilder()
                .setHandlingTime(Instant.ofEpochMilli(message.putDateTime.getTimeInMillis()));

        putNonNullDataInMap("MQMD_CharacterSet", "" + message.characterSet, endpointHandlerBuilder.getMetadata());
        putNonNullDataInMap("MQMD_Format", message.format, endpointHandlerBuilder.getMetadata());
        putNonNullDataInMap("MQMD_Encoding", "" + message.encoding, endpointHandlerBuilder.getMetadata());
        putNonNullDataInMap("MQMD_AccountingToken", message.accountingToken, endpointHandlerBuilder.getMetadata());
        putNonNullDataInMap("MQMD_Persistence", "" + message.persistence, endpointHandlerBuilder.getMetadata());
        putNonNullDataInMap("MQMD_Priority", "" + message.priority, endpointHandlerBuilder.getMetadata());
        putNonNullDataInMap("MQMD_ReplyToQueueManager", message.replyToQueueManagerName, endpointHandlerBuilder.getMetadata());
        putNonNullDataInMap("MQMD_ReplyToQueue", message.replyToQueueName, endpointHandlerBuilder.getMetadata());
        putNonNullDataInMap("MQMD_BackoutCount", "" + message.backoutCount, endpointHandlerBuilder.getMetadata());

        this.messagingTelemetryEventBuilder.setPayload(getContent(message));


        this.messagingTelemetryEventBuilder
                .setId(byteArrayToString(message.messageId))
                .setCorrelationId(byteArrayToString(message.correlationId))
                .addOrMergeEndpoint(new EndpointBuilder()
                        .addEndpointHandler(endpointHandlerBuilder.setType(EndpointHandler.EndpointHandlerType.WRITER)));
        if (message.expiry != CMQC.MQEI_UNLIMITED) {
            this.messagingTelemetryEventBuilder.setExpiry(Instant.ofEpochMilli(message.putDateTime.getTimeInMillis() + (message.expiry * 100)));
        }
        if (message.messageType == CMQC.MQMT_REQUEST) {
            this.messagingTelemetryEventBuilder.setMessagingEventType(MessagingEventType.REQUEST);
        } else if (message.messageType == CMQC.MQMT_REPLY) {
            this.messagingTelemetryEventBuilder.setMessagingEventType(MessagingEventType.RESPONSE);
        } else if (message.messageType == CMQC.MQMT_DATAGRAM) {
            this.messagingTelemetryEventBuilder.setMessagingEventType(MessagingEventType.FIRE_FORGET);
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
        if (bytes == null) {
            return null;
        }
        this.byteArrayBuilder.setLength(0);
        boolean allZero = true;
        for (byte aByte : bytes) {
            this.byteArrayBuilder.append(String.format("%02x", aByte));
            if (aByte != 0) {
                allZero = false;
            }
        }
        return allZero ? null : this.byteArrayBuilder.toString();
    }
}
