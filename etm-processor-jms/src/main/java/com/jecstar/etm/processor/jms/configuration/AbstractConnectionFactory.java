package com.jecstar.etm.processor.jms.configuration;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractConnectionFactory {

    public String userId;

    public String password;

    public List<Destination> destinations = new ArrayList<>();

}
