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

package com.jecstar.etm.processor.ibmmq.handler;

import com.ibm.mq.MQMessage;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.handler.HandlerResult;
import com.jecstar.etm.processor.handler.HandlerResults;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.io.IOException;

public class EtmEventHandler extends AbstractMQEventHandler {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(EtmEventHandler.class);

    private final TelemetryCommandProcessor telemetryCommandProcessor;
    private final StringBuilder byteArrayBuilder = new StringBuilder();

    public EtmEventHandler(TelemetryCommandProcessor telemetryCommandProcessor, String defaultImportProfile) {
        super(defaultImportProfile);
        this.telemetryCommandProcessor = telemetryCommandProcessor;
    }

    public HandlerResults handleMessage(MQMessage message) {
        HandlerResults results = new HandlerResults();
        try {
            results = handleData(getContent(message));
        } catch (IOException e) {
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("Message with id '" + byteArrayToString(message.messageId) + "' seems to have an invalid format", e);
            }
            results.addHandlerResult(HandlerResult.parserFailure(e));
        }
        return results;
    }

    private String byteArrayToString(byte[] bytes) {
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

    @Override
    protected TelemetryCommandProcessor getProcessor() {
        return this.telemetryCommandProcessor;
    }
}
