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
import com.codahale.metrics.Timer;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.handler.HandlerResults;
import com.jecstar.etm.processor.jms.configuration.Destination;
import com.jecstar.etm.processor.jms.handler.ClonedMessageHandler;
import com.jecstar.etm.processor.jms.handler.EtmEventHandler;
import com.jecstar.etm.processor.reader.DestinationReader;
import com.jecstar.etm.processor.reader.DestinationReaderInstantiationContext;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.util.Counter;

import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.Message;

class JmsDestinationReader implements DestinationReader {

    private static final LogWrapper log = LogFactory.getLogger(JmsDestinationReader.class);

    private final Timer jmsGetTimer;
    private final Destination destination;
    private final String userId;
    private final String password;
    private final EtmEventHandler etmEventHandler;
    private final ClonedMessageHandler clonedMessageEventHandler;
    private final ConnectionFactory connectionFactory;

    /**
     * The number of milliseconds waiting for a message on the destination.
     */
    private final int receiveMessageWaitInterval = 5_000;

    /**
     * The number of milliseconds between an increase or decrease check of the pool size.
     */
    private final int poolCheckInterval = 30_000;

    private final DestinationReaderInstantiationContext<JmsDestinationReader> instantiationContext;
    private final Counter counter;

    private boolean stop = false;
    private long lastPoolCheckTime;

    private JMSContext jmsContext;
    private JMSConsumer consumer;

    public JmsDestinationReader(
            final TelemetryCommandProcessor processor,
            final MetricRegistry metricRegistry,
            final ConnectionFactory connectionFactory,
            final Destination destination,
            final String userId,
            final String password,
            final DestinationReaderInstantiationContext<JmsDestinationReader> instantiationContext
    ) {
        this.destination = destination;
        this.userId = userId;
        this.password = password;
        this.connectionFactory = connectionFactory;
        this.etmEventHandler = new EtmEventHandler(processor, destination.getDefaultImportProfile());
        this.clonedMessageEventHandler = new ClonedMessageHandler(processor, destination.getDefaultImportProfile());
        this.jmsGetTimer = metricRegistry.timer("jms-processor.mqget." + destination.getName().replaceAll("\\.", "_"));
        this.instantiationContext = instantiationContext;
        this.counter = new Counter();
    }

    @Override
    public void run() {
        connect();
        this.lastPoolCheckTime = System.currentTimeMillis();
        while (!this.stop) {
            Message message = null;
            final Timer.Context jmsGetContext = this.jmsGetTimer.time();
            try {
                message = this.consumer.receive(this.receiveMessageWaitInterval);
            } finally {
                long elapsed = jmsGetContext.stop();
                this.counter.add(elapsed / 1_000_000);
            }
            if (message != null) {
                if ("etmevent".equalsIgnoreCase(this.destination.getMessagesType())) {
                    this.etmEventHandler.handleMessage(message);
                } else if ("clone".equalsIgnoreCase(this.destination.getMessagesType())) {
                    this.clonedMessageEventHandler.handleMessage(message);
                } else {
                    HandlerResults results = this.etmEventHandler.handleMessage(message);
                    if (results.hasParseFailures()) {
                        this.clonedMessageEventHandler.handleMessage(message);
                    }
                }
            }
            checkPoolSize();
            if (Thread.currentThread().isInterrupted()) {
                this.stop = true;
            }
        }
        disconnect();
    }

    @Override
    public void stop() {
        this.stop = true;
    }

    private void connect() {
        if (log.isDebugLevelEnabled()) {
            log.logDebugMessage("Connecting to destination '" + this.destination.getName() + "'");
        }
        if (this.jmsContext == null) {
            if (this.userId != null) {
                this.jmsContext = this.connectionFactory.createContext(this.userId, this.password, JMSContext.AUTO_ACKNOWLEDGE);
            } else {
                this.jmsContext = this.connectionFactory.createContext(JMSContext.AUTO_ACKNOWLEDGE);
            }
            this.jmsContext.start();
        }
        if (this.consumer == null) {
            if ("topic".equals(this.destination.getType())) {
                this.consumer = this.jmsContext.createConsumer(this.jmsContext.createTopic(this.destination.getName()));
            } else {
                this.consumer = this.jmsContext.createConsumer(this.jmsContext.createQueue(this.destination.getName()));
            }
        }
    }

    private void disconnect() {
        if (log.isDebugLevelEnabled()) {
            log.logDebugMessage("Disconnecting from destination '" + this.destination.getName() + "'");
        }
        if (this.consumer != null) {
            this.consumer.close();
            this.consumer = null;
        }
        if (this.jmsContext != null) {
            this.jmsContext.close();
            this.jmsContext = null;
        }
    }

    private void checkPoolSize() {
        if (this.instantiationContext.getIndexInPool() != 0) {
            // Only the first thread in the pool should check the pool size. It's worth nothing if every thread continuously
            // checks the pool size.
            return;
        }
        if ("topic".equals(this.destination.getType())) {
            return;
        }
        if (System.currentTimeMillis() - this.lastPoolCheckTime < this.poolCheckInterval) {
            return;
        }
        this.lastPoolCheckTime = System.currentTimeMillis();
        final long averageReceiveTime = this.counter.getAverage();
        if (averageReceiveTime < 10) {
            this.instantiationContext.getDestinationReaderPool().increaseIfPossible();
        } else if (averageReceiveTime > 250) {
            this.instantiationContext.getDestinationReaderPool().decreaseIfPossible();
        }
        this.counter.reset();
    }
}
