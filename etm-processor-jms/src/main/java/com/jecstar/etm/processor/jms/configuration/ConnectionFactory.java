package com.jecstar.etm.processor.jms.configuration;

import java.util.ArrayList;
import java.util.List;

public class ConnectionFactory {

    public String factoryClassName;

    public String className;

    public String connectionURI;

    public String userId;

    public String password;

    private List<Destination> destinations = new ArrayList<>();

    public List<Destination> getDestinations() {
        return this.destinations;
    }
}
