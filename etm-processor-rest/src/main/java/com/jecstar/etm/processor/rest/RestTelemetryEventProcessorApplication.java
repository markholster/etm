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

package com.jecstar.etm.processor.rest;

import com.jecstar.etm.processor.core.TelemetryCommandProcessor;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

public class RestTelemetryEventProcessorApplication extends Application {

    public RestTelemetryEventProcessorApplication(TelemetryCommandProcessor processor) {
        RestTelemetryEventProcessor.setProcessor(processor);
    }

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<>();
        classes.add(RestTelemetryEventProcessor.class);
        return classes;
    }
}
