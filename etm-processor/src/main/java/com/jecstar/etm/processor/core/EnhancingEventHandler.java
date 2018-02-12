package com.jecstar.etm.processor.core;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.TelemetryCommand.CommandType;
import com.jecstar.etm.server.core.domain.EndpointConfiguration;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.enhancers.DefaultTelemetryEventEnhancer;
import com.lmax.disruptor.EventHandler;

import java.time.ZonedDateTime;

class EnhancingEventHandler implements EventHandler<TelemetryCommand> {


    private final long ordinal;
    private final long numberOfConsumers;
    private final CommandResources commandResources;

    private final EndpointConfiguration endpointConfiguration;

    private final DefaultTelemetryEventEnhancer defaultTelemetryEventEnhancer = new DefaultTelemetryEventEnhancer();
    private final CustomAchmeaEnhancements achmeaEnhancements;
    private final Timer timer;

    EnhancingEventHandler(final long ordinal, final long numberOfConsumers, final EtmConfiguration etmConfiguration, final CommandResources commandResources, final MetricRegistry metricRegistry) {
        this.ordinal = ordinal;
        this.numberOfConsumers = numberOfConsumers;
        this.commandResources = commandResources;
        this.endpointConfiguration = new EndpointConfiguration();
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
                enhanceTelemetryEvent(command.businessTelemetryEvent);
                break;
            case HTTP_EVENT:
                enhanceTelemetryEvent(command.httpTelemetryEvent);
                break;
            case LOG_EVENT:
                enhanceTelemetryEvent(command.logTelemetryEvent);
                break;
            case MESSAGING_EVENT:
                this.achmeaEnhancements.enhanceMessagingEvent(command.messagingTelemetryEvent);
                enhanceTelemetryEvent(command.messagingTelemetryEvent);
                break;
            case SQL_EVENT:
                enhanceTelemetryEvent(command.sqlTelemetryEvent);
                break;
            default:
                throw new IllegalArgumentException("'" + command.commandType.name() + "' not implemented.");
        }
    }

    private void enhanceTelemetryEvent(TelemetryEvent<?> event) {
        final Context timerContext = this.timer.time();
        try {
            final ZonedDateTime now = ZonedDateTime.now();
            this.commandResources.loadEndpointConfig(event.endpoints, this.endpointConfiguration);
            if (this.endpointConfiguration.eventEnhancer != null) {
                this.endpointConfiguration.eventEnhancer.enhance(event, now);
            } else {
                this.defaultTelemetryEventEnhancer.enhance(event, now);
            }
        } finally {
            timerContext.stop();
        }
    }
}
