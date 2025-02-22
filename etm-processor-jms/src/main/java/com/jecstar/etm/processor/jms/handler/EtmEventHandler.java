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

import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.handler.HandlerResults;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import javax.jms.JMSException;
import javax.jms.Message;

public class EtmEventHandler extends AbstractJMSEventHandler {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(EtmEventHandler.class);

    private final TelemetryCommandProcessor telemetryCommandProcessor;

    public EtmEventHandler(TelemetryCommandProcessor telemetryCommandProcessor, String defaultImportProfile) {
        super(defaultImportProfile);
        this.telemetryCommandProcessor = telemetryCommandProcessor;
    }

    @Override
    protected TelemetryCommandProcessor getProcessor() {
        return this.telemetryCommandProcessor;
    }

    @SuppressWarnings("unchecked")
    public HandlerResults handleMessage(Message message) {
        HandlerResults results = new HandlerResults();
        try {
            results = handleData(getContent(message));
        } catch (JMSException e) {
            String messageId = "unknown";
            try {
                messageId = message.getJMSMessageID();
            } catch (JMSException e1) {
            }
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("Message with id '" + messageId + "' seems to have an invalid format", e);
            }
            results.addHandlerResult(com.jecstar.etm.processor.handler.HandlerResult.parserFailure(e));
        }
        return results;
    }

}
