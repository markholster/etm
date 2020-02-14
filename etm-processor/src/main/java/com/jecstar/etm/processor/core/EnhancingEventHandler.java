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
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.TelemetryCommand.CommandType;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.enhancers.DefaultTelemetryEventEnhancer;
import com.lmax.disruptor.EventHandler;

import java.time.ZonedDateTime;

class EnhancingEventHandler implements EventHandler<TelemetryCommand> {


    private final long ordinal;
    private final long numberOfConsumers;
    private final CommandResources commandResources;

    private final DefaultTelemetryEventEnhancer defaultTelemetryEventEnhancer = new DefaultTelemetryEventEnhancer();
    private final CustomAchmeaEnhancements achmeaEnhancements;
    private final Timer timer;

    EnhancingEventHandler(final long ordinal, final long numberOfConsumers, final EtmConfiguration etmConfiguration, final CommandResources commandResources, final MetricRegistry metricRegistry) {
        this.ordinal = ordinal;
        this.numberOfConsumers = numberOfConsumers;
        this.commandResources = commandResources;
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
            final var now = ZonedDateTime.now();
            final var importProfile = this.commandResources.loadImportProfile(importProfileName);
            if (importProfile.eventEnhancer != null) {
                importProfile.eventEnhancer.enhance(event, now);
            } else {
                this.defaultTelemetryEventEnhancer.enhance(event, now);
            }
        } finally {
            timerContext.stop();
        }
    }
}
