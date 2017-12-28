package com.jecstar.etm.processor.jms.configuration;

import java.util.ArrayList;
import java.util.List;

public class Jms {

    public boolean enabled = false;

    public final List<AbstractConnectionFactory> connectionFactories = new ArrayList<>();

    public int getMinimumNumberOfListeners() {
        if (this.connectionFactories.isEmpty()) {
            return 0;
        }
        return this.connectionFactories.stream().mapToInt(
                f -> f.destinations.stream().mapToInt(Destination::getMinNrOfListeners).sum()
        ).sum();
    }

    public int getMaximumNumberOfListeners() {
        if (this.connectionFactories.isEmpty()) {
            return 0;
        }
        return this.connectionFactories.stream().mapToInt(
                f -> f.destinations.stream().mapToInt(Destination::getMaxNrOfListeners).sum()
        ).sum();
    }

    public List<AbstractConnectionFactory> getConnectionFactories() {
        return this.connectionFactories;
    }
}
