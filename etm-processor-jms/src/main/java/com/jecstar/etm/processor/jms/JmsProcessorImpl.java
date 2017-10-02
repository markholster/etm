package com.jecstar.etm.processor.jms;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.jms.configuration.*;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.util.NamedThreadFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
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
    private List<Context> contexts = new ArrayList<>();

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
        for (AbstractConnectionFactory abstractConnectionFactory : this.config.getConnectionFactories()) {
            ConnectionFactory jmsConnectionFactory = createJmsConnectionFactory(abstractConnectionFactory);
            Connection connection = null;
            try {
                if (abstractConnectionFactory.userId != null || abstractConnectionFactory.password != null) {
                    connection = jmsConnectionFactory.createConnection(abstractConnectionFactory.userId, abstractConnectionFactory.password);
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
            for (Destination destination : abstractConnectionFactory.getDestinations()) {
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
        for (Context context : this.contexts) {
            try {
                context.close();
            } catch (NamingException e) {
                if (log.isDebugLevelEnabled()) {
                    log.logDebugMessage("Unable to close connection to context of messaging infrastructure.", e);
                }
            }
        }
        this.contexts.clear();
    }

    private ConnectionFactory createJmsConnectionFactory(AbstractConnectionFactory abstractConnectionFactory) {
        ConnectionFactory jmsConnectionFactory = null;
        if (abstractConnectionFactory instanceof CustomConnectionFactory) {
            CustomConnectionFactory customConnectionFactory = (CustomConnectionFactory) abstractConnectionFactory;
            try {
                Class<?> clazz = Class.forName(customConnectionFactory.factoryClassName);
                Object object = clazz.newInstance();
                if (!(object instanceof ConnectionFactoryFactory)) {
                    if (log.isErrorLevelEnabled()) {
                        log.logErrorMessage("'" + customConnectionFactory.factoryClassName + "' is not of type '" +
                                ConnectionFactoryFactory.class.getName() + "' but of type '" +
                                object.getClass().getName() + "'. Unable to setup connection to destinations.");
                    }
                    return null;
                }
                return ((ConnectionFactoryFactory) object).createConnectionFactory();
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage("Unable to instantiate '" + customConnectionFactory.factoryClassName + "'.", e);
                }
                return null;
            }
        } else if (abstractConnectionFactory instanceof NativeConnectionFactory) {
            NativeConnectionFactory nativeConnectionFactory = (NativeConnectionFactory) abstractConnectionFactory;
            try {
                Class<?> clazz = Class.forName(nativeConnectionFactory.className);
                Object object = clazz.newInstance();
                if (!(object instanceof ConnectionFactory)) {
                    if (log.isErrorLevelEnabled()) {
                        log.logErrorMessage("'" + nativeConnectionFactory.className + "' is not of type '" +
                                ConnectionFactory.class.getName() + "' but of type '" +
                                object.getClass().getName() + "'. Unable to setup connection to destinations.");
                    }
                    return null;
                }
                addParameters(object, nativeConnectionFactory.parameters);
                return (ConnectionFactory) object;
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchFieldException e) {
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage("Unable to instantiate '" + nativeConnectionFactory.className + "'.", e);
                }
                return null;
            }
        } else if (abstractConnectionFactory instanceof JNDIConnectionFactory) {
            JNDIConnectionFactory jndiConnectionFactory = (JNDIConnectionFactory) abstractConnectionFactory;
            Hashtable<String, String> environment = new Hashtable<>();
            environment.put(Context.INITIAL_CONTEXT_FACTORY, jndiConnectionFactory.initialContextFactory);
            environment.put(Context.PROVIDER_URL, jndiConnectionFactory.providerURL);
            environment.putAll(jndiConnectionFactory.parameters);
            try {
                Context context = new InitialContext(environment);
                this.contexts.add(context);
                Object object = context.lookup(jndiConnectionFactory.jndiName);
                if (!(object instanceof ConnectionFactory)) {
                    if (log.isErrorLevelEnabled()) {
                        log.logErrorMessage("'" + jndiConnectionFactory.jndiName + "' is not of type '" +
                                ConnectionFactory.class.getName() + "' but of type '" +
                                object.getClass().getName() + "'. Unable to setup connection to destinations.");
                    }
                    return null;
                }
            } catch (NamingException e) {
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage("Unable to locate '" + jndiConnectionFactory.jndiName + "'.", e);
                }
                return null;
            }
        }
        if (log.isErrorLevelEnabled()) {
            log.logErrorMessage("No factoryClassName or className provided in jms configuration section. Unable to read from destinations.");
        }
        return null;
    }

    private void addParameters(Object object, Map<String, String> parameters) throws NoSuchFieldException, IllegalAccessException {
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            final Field field = object.getClass().getField(entry.getKey());
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            if (Boolean.class.equals(field.getType()) || boolean.class.equals(field.getType())) {
                field.setBoolean(object,Boolean.valueOf(entry.getValue()));
            } else if (Byte.class.equals(field.getType()) || byte.class.equals(field.getType())) {
                field.setInt(object, Byte.valueOf(entry.getValue()));
            } else if (Character.class.equals(field.getType()) || char.class.equals(field.getType())) {
                field.setInt(object, entry.getValue().charAt(0));
            } else if (Integer.class.equals(field.getType()) || int.class.equals(field.getType())) {
                field.setInt(object, Integer.valueOf(entry.getValue()));
            } else if (Double.class.equals(field.getType()) || double.class.equals(field.getType())) {
                field.setDouble(object, Double.valueOf(entry.getValue()));
            } else if (Float.class.equals(field.getType()) || float.class.equals(field.getType())) {
                field.setFloat(object, Float.valueOf(entry.getValue()));
            } else if (Long.class.equals(field.getType()) || long.class.equals(field.getType())) {
                field.setLong(object, Long.valueOf(entry.getValue()));
            } else if (Short.class.equals(field.getType()) || short.class.equals(field.getType())) {
                field.setShort(object, Short.valueOf(entry.getValue()));
            } else {
                field.set(object, Byte.valueOf(entry.getValue()));
            }
        }
    }

}
