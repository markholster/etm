package com.jecstar.etm.processor.jms;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.jms.configuration.Destination;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import javax.jms.ConnectionFactory;

public class DestinationReader implements Runnable {

    private static final LogWrapper log = LogFactory.getLogger(DestinationReader.class);

    private final Timer jmsGetTimer;
    private final Destination destination;
    private final String userId;
    private final String password;

    public DestinationReader(String configurationName, TelemetryCommandProcessor processor, MetricRegistry metricRegistry, ConnectionFactory connectionFactory, Destination destination, String userId, String password) {
        this.destination = destination;
        this.userId = userId;
        this.password = password;
//        this.etmEventHandler = new EtmEventHandler(processor);
//        this.iibEventHandler = new IIBEventHandler(processor);
//        this.clonedMessageEventHandler = new ClonedMessageHandler(processor);
        this.jmsGetTimer = metricRegistry.timer("jms-processor.mqget." + destination.getName().replaceAll("\\.", "_"));
    }

    @Override
    public void run() {

    }

    private void connect() {
        if (log.isDebugLevelEnabled()) {
            log.logDebugMessage("Connecting to destination '" + this.destination.getName() + "'");
        }

    }
}
