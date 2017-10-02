package com.jecstar.etm.processor.jms.configuration;

import javax.jms.ConnectionFactory;

public interface ConnectionFactoryFactory {

    public ConnectionFactory createConnectionFactory();

}
