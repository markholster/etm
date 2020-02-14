/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.processor.jms.handler;

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

import javax.jms.JMSException;
import javax.jms.Message;
import java.time.Instant;
import java.util.Map;

public class ClonedMessageHandler extends AbstractJMSEventHandler {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(ClonedMessageHandler.class);


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

    public HandlerResults handleMessage(Message message) {
        HandlerResults results = new HandlerResults();
        this.messagingTelemetryEventBuilder.initialize();
        try {
            parseMessage(message);
            this.telemetryCommandProcessor.processTelemetryEvent(this.messagingTelemetryEventBuilder, getDefaultImportProfile());
            results.addHandlerResult(HandlerResult.processed());
        } catch (JMSException e) {
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("Unable to process content.", e);
            }
            results.addHandlerResult(HandlerResult.parserFailure(e));
        }
        return results;
    }


    private void parseMessage(Message message) throws JMSException {
        putNonNullDataInMap("JMS_Type", message.getJMSType(), this.messagingTelemetryEventBuilder.getMetadata());
        putNonNullDataInMap("JMS_DeliveryMode", "" + message.getJMSDeliveryMode(), this.messagingTelemetryEventBuilder.getMetadata());
        putNonNullDataInMap("JMS_Priority", "" + message.getJMSPriority(), this.messagingTelemetryEventBuilder.getMetadata());
        putNonNullDataInMap("JMS_Redelivered", "" + message.getJMSRedelivered(), this.messagingTelemetryEventBuilder.getMetadata());

        this.messagingTelemetryEventBuilder.setPayload(getContent(message));

        this.messagingTelemetryEventBuilder
                .setId(parseId(message.getJMSMessageID()))
                .setCorrelationId(parseId(message.getJMSCorrelationID()))
                .setMessagingEventType(MessagingEventType.FIRE_FORGET)
                .addOrMergeEndpoint(new EndpointBuilder()
                        .addEndpointHandler(new EndpointHandlerBuilder()
                                .setType(EndpointHandler.EndpointHandlerType.WRITER)
                                .setHandlingTime(Instant.ofEpochMilli(message.getJMSTimestamp() != 0 ? message.getJMSTimestamp() : System.currentTimeMillis()))));
        if (message.getJMSExpiration() != 0) {
            this.messagingTelemetryEventBuilder.setExpiry(Instant.ofEpochMilli(message.getJMSExpiration()));
        }
    }

    private void putNonNullDataInMap(String key, String value, Map<String, Object> map) {
        if (value != null && value.trim().length() > 0) {
            map.put(key, value.trim());
        }
    }

    private String parseId(String id) {
        if (id == null || id.length() == 0) {
            return null;
        }
        if (id.startsWith("ID:")) {
            // By spec all id's should start with "ID:" but we don't trust all providers so we build in this check.
            return id.substring(3);
        }
        return id;
    }
}
