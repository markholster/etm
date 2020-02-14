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

package com.jecstar.etm.processor.core;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.domain.*;
import com.jecstar.etm.domain.builder.*;
import com.jecstar.etm.server.core.domain.configuration.ConfigurationChangeListener;
import com.jecstar.etm.server.core.domain.configuration.ConfigurationChangedEvent;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;

import java.util.concurrent.ThreadFactory;

public interface TelemetryCommandProcessor extends ConfigurationChangeListener {

    void start(ThreadFactory threadFactory, PersistenceEnvironment persistenceEnvironment, EtmConfiguration etmConfiguration);

    void stop();

    void stopAll();

    void processTelemetryEvent(SqlTelemetryEventBuilder builder, String importProfile);

    void processTelemetryEvent(SqlTelemetryEvent event, String importProfile);

    void processTelemetryEvent(HttpTelemetryEventBuilder builder, String importProfile);

    void processTelemetryEvent(HttpTelemetryEvent event, String importProfile);

    void processTelemetryEvent(LogTelemetryEventBuilder builder, String importProfile);

    void processTelemetryEvent(LogTelemetryEvent event, String importProfile);

    void processTelemetryEvent(MessagingTelemetryEventBuilder builder, String importProfile);

    void processTelemetryEvent(MessagingTelemetryEvent event, String importProfile);

    void processTelemetryEvent(BusinessTelemetryEventBuilder builder, String importProfile);

    void processTelemetryEvent(BusinessTelemetryEvent event, String importProfile);

    MetricRegistry getMetricRegistry();

    boolean isReadyForProcessing();

    long getCurrentCapacity();

    @Override
    void configurationChanged(ConfigurationChangedEvent event);
}
