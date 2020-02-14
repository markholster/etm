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

package com.jecstar.etm.processor.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jecstar.etm.domain.*;
import com.jecstar.etm.domain.writer.TelemetryEventTags;
import com.jecstar.etm.domain.writer.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.server.core.domain.converter.json.*;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractJsonHandler {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    protected final LogWrapper log = LogFactory.getLogger(getClass());

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TelemetryEventTags tags = new TelemetryEventTagsJsonImpl();

    private final BusinessTelemetryEventConverterJsonImpl businessConverter = new BusinessTelemetryEventConverterJsonImpl();
    private final HttpTelemetryEventConverterJsonImpl httpConverter = new HttpTelemetryEventConverterJsonImpl();
    private final LogTelemetryEventConverterJsonImpl logConverter = new LogTelemetryEventConverterJsonImpl();
    private final MessagingTelemetryEventConverterJsonImpl messagingConverter = new MessagingTelemetryEventConverterJsonImpl();
    private final SqlTelemetryEventConverterJsonImpl sqlConverter = new SqlTelemetryEventConverterJsonImpl();

    private final BusinessTelemetryEvent businessTelemetryEvent = new BusinessTelemetryEvent();
    private final HttpTelemetryEvent httpTelemetryEvent = new HttpTelemetryEvent();
    private final LogTelemetryEvent logTelemetryEvent = new LogTelemetryEvent();
    private final MessagingTelemetryEvent messagingTelemetryEvent = new MessagingTelemetryEvent();
    private final SqlTelemetryEvent sqlTelemetryEvent = new SqlTelemetryEvent();
    private final String defaultImportProfile;

    protected abstract TelemetryCommandProcessor getProcessor();

    protected AbstractJsonHandler(String defaultImportProfile) {
        this.defaultImportProfile = defaultImportProfile;
    }

    @SuppressWarnings("unchecked")
    protected HandlerResults handleData(String jsonData) {
        HandlerResults results = new HandlerResults();
        if (jsonData == null) {
            return results;
        }
        jsonData = jsonData.trim();
        if (jsonData.startsWith("[")) {
            try {
                ArrayList<Map<String, Object>> events = this.objectMapper.readValue(jsonData, ArrayList.class);
                for (Map<String, Object> event : events) {
                    handleEvent(event, results);
                }
            } catch (Exception e) {
                results.addHandlerResult(HandlerResult.parserFailure(e));
            }
        } else {
            try {
                Map<String, Object> event = this.objectMapper.readValue(jsonData, HashMap.class);
                handleEvent(event, results);
            } catch (Exception e) {
                results.addHandlerResult(HandlerResult.parserFailure(e));
            }
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    protected HandlerResults handleSingleEvent(InputStream inputStream) {
        HandlerResults results = new HandlerResults();
        try {
            Map<String, Object> event = this.objectMapper.readValue(inputStream, HashMap.class);
            handleEvent(event, results);
        } catch (Exception e) {
            results.addHandlerResult(HandlerResult.parserFailure(e));
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    protected HandlerResults handleBulkEvents(InputStream inputStream) {
        HandlerResults results = new HandlerResults();
        try {
            ArrayList<Map<String, Object>> events = this.objectMapper.readValue(inputStream, ArrayList.class);
            for (Map<String, Object> event : events) {
                handleEvent(event, results);
            }
        } catch (Exception e) {
            results.addHandlerResult(HandlerResult.parserFailure(e));
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private void handleEvent(Map<String, Object> event, HandlerResults results) {
        TelemetryCommand.CommandType commandType = determineCommandType(event);
        if (commandType == null) {
            results.addHandlerResult(HandlerResult.failed("Unable to determine command type"));
        } else {
            Map<String, Object> eventData = (Map<String, Object>) event.get("data");
            String importProfile = (String) event.get("import_profile");
            if (importProfile == null) {
                importProfile = this.defaultImportProfile;
            }
            process(commandType, eventData, importProfile);
            results.addHandlerResult(HandlerResult.processed());
        }
    }

    private TelemetryCommand.CommandType determineCommandType(Map<String, Object> event) {
        String eventType = (String) event.get("type");
        return TelemetryCommand.CommandType.valueOfStringType(eventType);
    }

    private void process(TelemetryCommand.CommandType commandType, Map<String, Object> eventData, String importProfile) {
        String id = null;
        if (eventData.containsKey(this.tags.getIdTag())) {
            id = eventData.get(this.tags.getIdTag()).toString();
        }
        switch (commandType) {
            // Initializing is done in the converters.
            case BUSINESS_EVENT:
                this.businessConverter.read(eventData, this.businessTelemetryEvent, id);
                getProcessor().processTelemetryEvent(this.businessTelemetryEvent, importProfile);
                break;
            case HTTP_EVENT:
                this.httpConverter.read(eventData, this.httpTelemetryEvent, id);
                getProcessor().processTelemetryEvent(this.httpTelemetryEvent, importProfile);
                break;
            case LOG_EVENT:
                this.logConverter.read(eventData, this.logTelemetryEvent, id);
                getProcessor().processTelemetryEvent(this.logTelemetryEvent, importProfile);
                break;
            case MESSAGING_EVENT:
                this.messagingConverter.read(eventData, this.messagingTelemetryEvent, id);
                getProcessor().processTelemetryEvent(this.messagingTelemetryEvent, importProfile);
                break;
            case SQL_EVENT:
                this.sqlConverter.read(eventData, this.sqlTelemetryEvent, id);
                getProcessor().processTelemetryEvent(this.sqlTelemetryEvent, importProfile);
                break;
            case NOOP:
                break;
        }
    }

    protected String getDefaultImportProfile() {
        return this.defaultImportProfile;
    }
}
