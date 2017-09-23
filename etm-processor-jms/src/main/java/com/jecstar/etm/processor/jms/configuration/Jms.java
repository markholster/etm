package com.jecstar.etm.processor.jms.configuration;

import java.util.ArrayList;
import java.util.List;

public class Jms {

    public boolean enabled = false;

    public final List<ConnectionFactory> connectionFactories = new ArrayList<>();

    public int getTotalNumberOfListeners() {
        if (this.connectionFactories.isEmpty()) {
            return 0;
        }
        return this.connectionFactories.stream().mapToInt(
                f -> f.getDestinations().stream().mapToInt(Destination::getNrOfListeners).sum()
        ).sum();
    }

    public List<ConnectionFactory> getConnectionFactories() {
        return this.connectionFactories;
    }
}
