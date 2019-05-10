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
