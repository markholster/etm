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
    public void processTelemetryEvent(SqlTelemetryEventBuilder builder) {
        this.sqlCount++;
    }

    @Override
    public void processTelemetryEvent(SqlTelemetryEvent event) {
        this.sqlCount++;
    }

    @Override
    public void processTelemetryEvent(HttpTelemetryEventBuilder builder) {
        this.httpCount++;
    }

    @Override
    public void processTelemetryEvent(HttpTelemetryEvent event) {
        this.httpCount++;
    }

    @Override
    public void processTelemetryEvent(LogTelemetryEventBuilder builder) {
        this.logCount++;
    }

    @Override
    public void processTelemetryEvent(LogTelemetryEvent event) {
        this.logCount++;
    }

    @Override
    public void processTelemetryEvent(MessagingTelemetryEventBuilder builder) {
        this.messagingCount++;
    }

    @Override
    public void processTelemetryEvent(MessagingTelemetryEvent event) {
        this.messagingCount++;
    }

    @Override
    public void processTelemetryEvent(BusinessTelemetryEventBuilder builder) {
        this.businessCount++;
    }

    @Override
    public void processTelemetryEvent(BusinessTelemetryEvent event) {
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
