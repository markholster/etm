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
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.ThreadFactory;

class DisruptorEnvironment {

    private final Disruptor<TelemetryCommand> disruptor;
    private final PersistingEventHandler[] persistingEventHandlers;

    DisruptorEnvironment(final EtmConfiguration etmConfiguration, final ThreadFactory threadFactory, final PersistenceEnvironment persistenceEnvironment, final MetricRegistry metricRegistry) {
        this.disruptor = new Disruptor<>(TelemetryCommand::new, etmConfiguration.getEventBufferSize(), threadFactory, ProducerType.MULTI, convertWaitStrategy(etmConfiguration.getWaitStrategy()));
        this.disruptor.setDefaultExceptionHandler(new TelemetryCommandExceptionHandler(metricRegistry));
        int enhancingHandlerCount = etmConfiguration.getEnhancingHandlerCount();
        final EnhancingEventHandler[] enhancingEventHandlers = new EnhancingEventHandler[enhancingHandlerCount];
        for (int i = 0; i < enhancingHandlerCount; i++) {
            enhancingEventHandlers[i] = new EnhancingEventHandler(i, enhancingHandlerCount, etmConfiguration, persistenceEnvironment.getCommandResources(metricRegistry), metricRegistry);
        }
        int persistingHandlerCount = etmConfiguration.getPersistingHandlerCount();
        this.persistingEventHandlers = new PersistingEventHandler[persistingHandlerCount];
        for (int i = 0; i < persistingHandlerCount; i++) {
            this.persistingEventHandlers[i] = new PersistingEventHandler(i, persistingHandlerCount, persistenceEnvironment.getCommandResources(metricRegistry), metricRegistry);
        }
        if (enhancingEventHandlers.length > 0) {
            this.disruptor.handleEventsWith(enhancingEventHandlers);
            if (this.persistingEventHandlers.length > 0) {
                this.disruptor.after(enhancingEventHandlers).handleEventsWith(this.persistingEventHandlers);
            }
        } else {
            if (this.persistingEventHandlers.length > 0) {
                this.disruptor.handleEventsWith(this.persistingEventHandlers);
            }
        }
    }

    private WaitStrategy convertWaitStrategy(com.jecstar.etm.server.core.domain.configuration.WaitStrategy waitStrategy) {
        switch (waitStrategy) {
            case BLOCKING:
                return new BlockingWaitStrategy();
            case BUSY_SPIN:
                return new BusySpinWaitStrategy();
            case SLEEPING:
                return new SleepingWaitStrategy();
            case YIELDING:
                return new YieldingWaitStrategy();
            default:
                return null;
        }

    }

    public RingBuffer<TelemetryCommand> start() {
        return this.disruptor.start();
    }

    public void shutdown() {
        this.disruptor.shutdown();
        for (PersistingEventHandler persistingEventHandler : this.persistingEventHandlers) {
            persistingEventHandler.close();
        }
    }
}
