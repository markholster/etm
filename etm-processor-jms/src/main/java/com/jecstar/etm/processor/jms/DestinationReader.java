package com.jecstar.etm.processor.jms;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.jms.configuration.Destination;

import javax.jms.Connection;

public class DestinationReader implements Runnable {

    public DestinationReader(String configurationName, final TelemetryCommandProcessor processor, MetricRegistry metricRegistry, final Connection connection, final Destination destination) {

    }

    @Override
    public void run() {

    }
}
