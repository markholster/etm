package com.jecstar.etm.processor.jms;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.jms.configuration.Destination;
import com.jecstar.etm.processor.jms.handler.ClonedMessageHandler;
import com.jecstar.etm.processor.jms.handler.EtmEventHandler;
import com.jecstar.etm.processor.jms.handler.HandlerResult;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import javax.jms.*;

public class DestinationReader implements Runnable {

    private static final LogWrapper log = LogFactory.getLogger(DestinationReader.class);

    private final Timer jmsGetTimer;
    private final Destination destination;
    private final String userId;
    private final String password;
    private final EtmEventHandler etmEventHandler;
    private final ClonedMessageHandler clonedMessageEventHandler;
    private final ConnectionFactory connectionFactory;

    private final int waitInterval = 5000;
    private boolean stop = false;

    private Connection connection;
    private Session session;
    private MessageConsumer consumer;

    public DestinationReader(TelemetryCommandProcessor processor, MetricRegistry metricRegistry, ConnectionFactory connectionFactory, Destination destination, String userId, String password) {
        this.destination = destination;
        this.userId = userId;
        this.password = password;
        this.connectionFactory = connectionFactory;
        this.etmEventHandler = new EtmEventHandler(processor);
        this.clonedMessageEventHandler = new ClonedMessageHandler(processor);
        this.jmsGetTimer = metricRegistry.timer("jms-processor.mqget." + destination.getName().replaceAll("\\.", "_"));
    }

    @Override
    public void run() {
        connect();
        while (!this.stop) {
            final Timer.Context jmsGetContext = this.jmsGetTimer.time();
            Message message = null;
            try {
                message = this.consumer.receive(this.waitInterval);
            } catch (JMSException e) {
                if (log.isErrorLevelEnabled()) {
                    log.logInfoMessage("Failed to read message from destination '" + this.destination.getName() + "'.", e);
                }
            } finally {
                jmsGetContext.stop();
            }
            if (message != null) {
                HandlerResult result;
                if ("etmevent".equalsIgnoreCase(this.destination.getMessagesType())) {
                    result = this.etmEventHandler.handleMessage(message);
                } else if ("clone".equalsIgnoreCase(this.destination.getMessagesType())) {
                    result = this.clonedMessageEventHandler.handleMessage(message);
                } else {
                    result = this.etmEventHandler.handleMessage(message);
                    if (HandlerResult.PARSE_FAILURE.equals(result)) {
                        result = this.clonedMessageEventHandler.handleMessage(message);
                    }
                }
            }
            if (Thread.currentThread().isInterrupted()) {
                this.stop = true;
            }
        }
        disconnect();
    }

    private void connect() {
        if (log.isDebugLevelEnabled()) {
            log.logDebugMessage("Connecting to destination '" + this.destination.getName() + "'");
        }
        if (this.connection == null) {
            try {
                if (this.userId != null) {
                    this.connection = this.connectionFactory.createConnection(this.userId, this.password);
                } else {
                    this.connection = this.connectionFactory.createConnection();
                }
                this.connection.start();
            } catch (JMSException e) {
                if (log.isWarningLevelEnabled()) {
                    log.logWarningMessage("Failed to connect to destination '" + this.destination.getName() + "'", e);
                }
            }
        }
        if (this.session == null) {
            try {
                this.session = this.connection.createSession(JMSContext.AUTO_ACKNOWLEDGE);
            } catch (JMSException e) {
                if (log.isWarningLevelEnabled()) {
                    log.logWarningMessage("Failed to connect to destination '" + this.destination.getName() + "'", e);
                }
            }
        }
        if (this.consumer == null) {
            try {
                if ("topic".equals(this.destination.getType())) {
                    this.consumer = this.session.createConsumer(this.session.createTopic(this.destination.getName()));
                } else {
                    this.consumer = this.session.createConsumer(this.session.createQueue(this.destination.getName()));
                }
            } catch (JMSException e) {
                if (log.isWarningLevelEnabled()) {
                    log.logWarningMessage("Failed to connect to destination '" + this.destination.getName() + "'", e);
                }
            }
        }
    }

    private void disconnect() {
        if (log.isDebugLevelEnabled()) {
            log.logDebugMessage("Disconnecting from destination '" + this.destination.getName() + "'");
        }
        if (this.consumer != null) {
            try {
                this.consumer.close();
            } catch (JMSException e) {
                if (log.isDebugLevelEnabled()) {
                    log.logDebugMessage("Unable to close connection from destination '" + this.destination.getName() + "'." , e);
                }
            }
            this.consumer = null;
        }
        if (this.session != null) {
            try {
                this.session.close();
            } catch (JMSException e) {
                if (log.isDebugLevelEnabled()) {
                    log.logDebugMessage("Unable to close connection from destination '" + this.destination.getName() + "'." , e);
                }
            }
            this.session = null;
        }
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (JMSException e) {
                if (log.isDebugLevelEnabled()) {
                    log.logDebugMessage("Unable to close connection from destination '" + this.destination.getName() + "'." , e);
                }
            }
            this.connection = null;
        }
    }
}
