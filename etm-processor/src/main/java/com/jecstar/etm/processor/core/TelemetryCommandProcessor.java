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

    void processTelemetryEvent(SqlTelemetryEventBuilder builder);

    void processTelemetryEvent(SqlTelemetryEvent event);

    void processTelemetryEvent(HttpTelemetryEventBuilder builder);

    void processTelemetryEvent(HttpTelemetryEvent event);

    void processTelemetryEvent(LogTelemetryEventBuilder builder);

    void processTelemetryEvent(LogTelemetryEvent event);

    void processTelemetryEvent(MessagingTelemetryEventBuilder builder);

    void processTelemetryEvent(MessagingTelemetryEvent event);

    void processTelemetryEvent(BusinessTelemetryEventBuilder builder);

    void processTelemetryEvent(BusinessTelemetryEvent event);

    MetricRegistry getMetricRegistry();

    boolean isReadyForProcessing();

    @Override
    void configurationChanged(ConfigurationChangedEvent event);
}
