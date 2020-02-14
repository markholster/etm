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

package com.jecstar.etm.processor.jms;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.domain.*;
import com.jecstar.etm.domain.builder.*;
import com.jecstar.etm.processor.core.PersistenceEnvironment;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.server.core.domain.configuration.ConfigurationChangedEvent;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;

import java.util.concurrent.ThreadFactory;

public class DummyCommandProcessor implements TelemetryCommandProcessor {

    private int sqlCount;
    private int httpCount;
    private int logCount;
    private int messagingCount;
    private int businessCount;

    @Override
    public void start(ThreadFactory threadFactory, PersistenceEnvironment persistenceEnvironment, EtmConfiguration etmConfiguration) {

    }

    @Override
    public void stop() {
        this.sqlCount = 0;
        this.httpCount = 0;
        this.logCount = 0;
        this.messagingCount = 0;
        this.businessCount = 0;
    }

    @Override
    public void stopAll() {
        this.stop();
    }

    @Override
    public void processTelemetryEvent(SqlTelemetryEventBuilder builder, String defaultImportProfile) {
        this.sqlCount++;
    }

    @Override
    public void processTelemetryEvent(SqlTelemetryEvent event, String defaultImportProfile) {
        this.sqlCount++;
    }

    @Override
    public void processTelemetryEvent(HttpTelemetryEventBuilder builder, String defaultImportProfile) {
        this.httpCount++;
    }

    @Override
    public void processTelemetryEvent(HttpTelemetryEvent event, String defaultImportProfile) {
        this.httpCount++;
    }

    @Override
    public void processTelemetryEvent(LogTelemetryEventBuilder builder, String defaultImportProfile) {
        this.logCount++;
    }

    @Override
    public void processTelemetryEvent(LogTelemetryEvent event, String defaultImportProfile) {
        this.logCount++;
    }

    @Override
    public void processTelemetryEvent(MessagingTelemetryEventBuilder builder, String defaultImportProfile) {
        this.messagingCount++;
    }

    @Override
    public void processTelemetryEvent(MessagingTelemetryEvent event, String defaultImportProfile) {
        this.messagingCount++;
    }

    @Override
    public void processTelemetryEvent(BusinessTelemetryEventBuilder builder, String defaultImportProfile) {
        this.businessCount++;
    }

    @Override
    public void processTelemetryEvent(BusinessTelemetryEvent event, String defaultImportProfile) {
        this.businessCount++;
    }

    @Override
    public MetricRegistry getMetricRegistry() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReadyForProcessing() {
        return true;
    }

    @Override
    public long getCurrentCapacity() {
        return 1000;
    }

    @Override
    public void configurationChanged(ConfigurationChangedEvent event) {
    }

    public int getBusinessCount() {
        return this.businessCount;
    }

    public int getHttpCount() {
        return this.httpCount;
    }

    public int getLogCount() {
        return this.logCount;
    }

    public int getMessagingCount() {
        return this.messagingCount;
    }

    public int getSqlCount() {
        return this.sqlCount;
    }

    public int getTotalEventCount() {
        return getBusinessCount() + getHttpCount() + getLogCount() + getMessagingCount() + getSqlCount();
    }
}
