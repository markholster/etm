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

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.TelemetryCommand.CommandType;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.lmax.disruptor.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

class TelemetryCommandExceptionHandler implements ExceptionHandler<TelemetryCommand> {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(TelemetryCommandExceptionHandler.class);
    private final Map<CommandType, Counter> counters = new HashMap<>();
    private final Counter totalCounter;

    TelemetryCommandExceptionHandler(final MetricRegistry metricRegistry) {
        for (CommandType commandType : CommandType.values()) {
            this.counters.put(commandType, metricRegistry.counter("event-processor.failures." + commandType.toStringType() + "_events"));
        }
        this.totalCounter = metricRegistry.counter("event-processor.failures.total");
    }

    @Override
    public void handleEventException(Throwable t, long sequence, TelemetryCommand command) {
        this.counters.get(command.commandType).inc();
        this.totalCounter.inc();
        switch (command.commandType) {
            case SQL_EVENT:
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage("Unable to process sql event '" + command.sqlTelemetryEvent.id + "'.", t);
                }
                break;
            case HTTP_EVENT:
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage("Unable to process http event '" + command.httpTelemetryEvent.id + "'.", t);
                }
                break;
            case LOG_EVENT:
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage("Unable to process log event '" + command.logTelemetryEvent.id + "'.", t);
                }
                break;
            case MESSAGING_EVENT:
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage("Unable to process log event '" + command.messagingTelemetryEvent.id + "'.", t);
                }
                break;
            default:
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage("Unable to process event '" + command.commandType.name() + "'.", t);
                }
        }
    }

    @Override
    public void handleOnStartException(Throwable t) {
        if (log.isFatalLevelEnabled()) {
            log.logFatalMessage("Unable to start disruptor. No events will be processed", t);
        }
    }

    @Override
    public void handleOnShutdownException(Throwable t) {
        if (log.isWarningLevelEnabled()) {
            log.logWarningMessage("Unable to stop disruptor.", t);
        }
    }

}
