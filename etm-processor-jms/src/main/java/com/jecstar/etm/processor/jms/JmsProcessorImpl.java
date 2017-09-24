package com.jecstar.etm.processor.jms;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.jms.configuration.ConnectionFactory;
import com.jecstar.etm.processor.jms.configuration.Destination;
import com.jecstar.etm.processor.jms.configuration.Jms;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.util.NamedThreadFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class JmsProcessorImpl implements JmsProcessor {

    private static final LogWrapper log = LogFactory.getLogger(JmsProcessorImpl.class);

    private final TelemetryCommandProcessor processor;
    private final MetricRegistry metricRegistry;
    private final String instanceName;
    private final String clusterName;
    private final Jms config;
    private ExecutorService executorService;
    private List<Connection> connections = new ArrayList<>();

    public JmsProcessorImpl(TelemetryCommandProcessor processor, MetricRegistry metricRegistry, Jms config, String clusterName, String instanceName) {
        this.processor = processor;
        this.metricRegistry = metricRegistry;
        this.config = config;
        this.clusterName = clusterName;
        this.instanceName = instanceName;
    }

    @Override
    public void start() {
        if (this.config.getTotalNumberOfListeners() <= 0) {
            return;
        }
        this.executorService = Executors.newFixedThreadPool(this.config.getTotalNumberOfListeners(), new NamedThreadFactory("jms_processor"));
        for (ConnectionFactory connectionFactory : this.config.getConnectionFactories()) {
            javax.jms.ConnectionFactory jmsConnectionFactory = createJmsConnectionFactory(connectionFactory);
            Connection connection = null;
            try {
                if (connectionFactory.userId != null || connectionFactory.password != null) {
                    connection = jmsConnectionFactory.createConnection(connectionFactory.userId, connectionFactory.password);
                } else {
                    connection = jmsConnectionFactory.createConnection();
                }
                connection.start();
                this.connections.add(connection);
            } catch (JMSException e) {
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage("Unable to setup connection to messaging infrastructure.", e);
                }
                continue;
            }
            for (Destination destination : connectionFactory.getDestinations()) {
                for (int i = 0; i < destination.getNrOfListeners(); i++) {
                    this.executorService.submit(new DestinationReader(this.clusterName + "_" + this.instanceName, this.processor, this.metricRegistry, connection, destination));
                }
            }
        }
    }

    @Override
    public void stop() {
        if (this.executorService != null) {
            this.executorService.shutdownNow();
            try {
                this.executorService.awaitTermination(15, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            this.executorService = null;
        }
        for (Connection connection : this.connections) {
            try {
                connection.close();
            } catch (JMSException e) {
                if (log.isDebugLevelEnabled()) {
                    log.logDebugMessage("Unable to close connection to messaging infrastructure.", e);
                }
            }
        }
        this.connections.clear();
    }

    private javax.jms.ConnectionFactory createJmsConnectionFactory(ConnectionFactory connectionFactory) {
        javax.jms.ConnectionFactory jmsConnectionFactory = null;
        if (connectionFactory.factoryClassName != null) {
            try {
                Class<?> clazz = Class.forName(connectionFactory.factoryClassName);
                Object object = clazz.newInstance();
                if (!(object instanceof ConnectionFactoryFactory)) {
                    if (log.isErrorLevelEnabled()) {
                        log.logErrorMessage("'" + connectionFactory.factoryClassName + "' is not of type '" +
                                ConnectionFactoryFactory.class.getName() + "' but of type '" +
                                object.getClass().getName() + "'. Unable to setup connection to destinations.");
                    }
                    return null;
                }
                return ((ConnectionFactoryFactory) object).createConnectionFactory();
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage("Unable to instantiate '" + connectionFactory.factoryClassName + "'.", e);
                }
                return null;
            }
        } else if (connectionFactory.className != null) {
            try {
                Class<?> clazz = Class.forName(connectionFactory.className);
                Object object = null;
                if (connectionFactory.connectionURI != null) {
                    object = clazz.newInstance();
                } else {
                    Constructor<?> constructor = clazz.getConstructor(String.class);
                    object = constructor.newInstance(connectionFactory.connectionURI);
                }
                if (!(object instanceof javax.jms.ConnectionFactory)) {
                    if (log.isErrorLevelEnabled()) {
                        log.logErrorMessage("'" + connectionFactory.className + "' is not of type '" +
                                javax.jms.ConnectionFactory.class.getName() + "' but of type '" +
                                object.getClass().getName() + "'. Unable to setup connection to destinations.");
                    }
                    return (javax.jms.ConnectionFactory) object;
                }
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage("Unable to instantiate '" + connectionFactory.factoryClassName + "'.", e);
                }
                return null;
            }

        }
        if (log.isErrorLevelEnabled()) {
            log.logErrorMessage("No factoryClassName or className provided in jms configuration section. Unable to read from destinations.");
        }
        return null;
    }


}
