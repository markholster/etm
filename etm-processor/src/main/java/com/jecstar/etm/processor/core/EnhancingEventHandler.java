package com.jecstar.etm.processor.core;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.TelemetryCommand.CommandType;
import com.jecstar.etm.server.core.domain.ImportProfile;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.enhancers.DefaultTelemetryEventEnhancer;
import com.lmax.disruptor.EventHandler;

import java.time.ZonedDateTime;

class EnhancingEventHandler implements EventHandler<TelemetryCommand> {


    private final long ordinal;
    private final long numberOfConsumers;
    private final CommandResources commandResources;

    private final ImportProfile importProfile;

    private final DefaultTelemetryEventEnhancer defaultTelemetryEventEnhancer = new DefaultTelemetryEventEnhancer();
    private final CustomAchmeaEnhancements achmeaEnhancements;
    private final Timer timer;

    EnhancingEventHandler(final long ordinal, final long numberOfConsumers, final EtmConfiguration etmConfiguration, final CommandResources commandResources, final MetricRegistry metricRegistry) {
        this.ordinal = ordinal;
        this.numberOfConsumers = numberOfConsumers;
        this.commandResources = commandResources;
        this.importProfile = new ImportProfile();
        this.achmeaEnhancements = new CustomAchmeaEnhancements(etmConfiguration);
        this.timer = metricRegistry.timer("event-processor.enhancing");
    }

    @Override
    public void onEvent(final TelemetryCommand command, final long sequence, final boolean endOfBatch) {
        if (sequence % this.numberOfConsumers != this.ordinal || CommandType.NOOP.equals(command.commandType)) {
            return;
        }
        switch (command.commandType) {
            case BUSINESS_EVENT:
                enhanceTelemetryEvent(command.businessTelemetryEvent, command.importProfile);
                break;
            case HTTP_EVENT:
                enhanceTelemetryEvent(command.httpTelemetryEvent, command.importProfile);
                break;
            case LOG_EVENT:
                enhanceTelemetryEvent(command.logTelemetryEvent, command.importProfile);
                break;
            case MESSAGING_EVENT:
                this.achmeaEnhancements.enhanceMessagingEvent(command.messagingTelemetryEvent);
                enhanceTelemetryEvent(command.messagingTelemetryEvent, command.importProfile);
                break;
            case SQL_EVENT:
                enhanceTelemetryEvent(command.sqlTelemetryEvent, command.importProfile);
                break;
            default:
                throw new IllegalArgumentException("'" + command.commandType.name() + "' not implemented.");
        }
    }

    private void enhanceTelemetryEvent(TelemetryEvent<?> event, String importProfileName) {
        final Context timerContext = this.timer.time();
        try {
            final ZonedDateTime now = ZonedDateTime.now();
            this.commandResources.loadImportProfile(importProfileName, this.importProfile);
            if (this.importProfile.eventEnhancer != null) {
                this.importProfile.eventEnhancer.enhance(event, now);
            } else {
                this.defaultTelemetryEventEnhancer.enhance(event, now);
            }
        } finally {
            timerContext.stop();
        }
    }
}
