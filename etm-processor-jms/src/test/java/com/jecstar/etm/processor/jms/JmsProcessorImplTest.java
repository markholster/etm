package com.jecstar.etm.processor.jms;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.jms.configuration.Destination;
import com.jecstar.etm.processor.jms.configuration.JNDIConnectionFactory;
import com.jecstar.etm.processor.jms.configuration.Jms;
import com.jecstar.etm.processor.jms.configuration.NativeConnectionFactory;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ActiveMQServers;
import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.jms.*;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Test class for the <code>JmsProcessorImpl</code> class.
 */
public class JmsProcessorImplTest {

    private ActiveMQServer server;

    @BeforeEach
    public void setup() throws Exception {
        this.server = ActiveMQServers.newActiveMQServer(new ConfigurationImpl()
                .setPersistenceEnabled(false)
                .setJournalDirectory("target/data/journal")
                .setSecurityEnabled(false)
                .addAcceptorConfiguration("tcp", "tcp://localhost:61616")
                .addConnectorConfiguration("connector", "tcp://localhost:61616"));
        this.server.start();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (this.server != null) {
            this.server.stop();
        }
    }

    /**
     * Test the consumption of messages.
     *
     * @throws JMSException
     */
    @Test
    public void testMessageConsumption() throws JMSException {
        DummyCommandProcessor commandProcessor = new DummyCommandProcessor();
        JmsProcessor jmsProcessor = createJmsProcessor(commandProcessor, "etm.queue.1");
        jmsProcessor.start();
        final int nrOfMessages = 100;
        addMessage(nrOfMessages);

        assertEquals(nrOfMessages, waitFor(commandProcessor, nrOfMessages, 30000));

        jmsProcessor.stop();
    }

    /**
     * Test the consumption of messages after the server is restarted.
     *
     * @throws Exception
     */
    @Test
    public void testConnectionReset() throws Exception {
        DummyCommandProcessor commandProcessor = new DummyCommandProcessor();
        JmsProcessor jmsProcessor = createJmsProcessor(commandProcessor, "etm.queue.1");
        jmsProcessor.start();
        // Add 10 messages.
        final int nrOfMessages = 10;
        addMessage(nrOfMessages);
        // wait for the messages to be processed.
        assertEquals(nrOfMessages, waitFor(commandProcessor, nrOfMessages, 30000));

        // Now stop the server.
        this.server.stop();
        // And start it again.
        this.server.start();

        // And test if processing is continued.
        addMessage(nrOfMessages);
        assertEquals(nrOfMessages, waitFor(commandProcessor, nrOfMessages, 30000));

        jmsProcessor.stop();
    }

    @Test
    public void testJndiConnedctionFactory() throws JMSException {
        DummyCommandProcessor commandProcessor = new DummyCommandProcessor();
        Jms config = new Jms();
        config.enabled = true;


        JNDIConnectionFactory factory = new JNDIConnectionFactory();
        factory.initialContextFactory = "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory";
        factory.providerURL = "tcp://localhost:61616";
        factory.jndiName = "ConnectionFactory";

        Destination destination = new Destination();
        destination.setName("etm.queue.1");
        factory.destinations.add(destination);

        config.getConnectionFactories().add(factory);

        JmsProcessorImpl jmsProcessor = new JmsProcessorImpl(commandProcessor, new MetricRegistry(), config);

        jmsProcessor.start();
        // Add 10 messages.
        final int nrOfMessages = 10;
        addMessage(nrOfMessages);
        // wait for the messages to be processed.
        assertEquals(nrOfMessages, waitFor(commandProcessor, nrOfMessages, 30000));

        jmsProcessor.stop();
    }

    private JmsProcessor createJmsProcessor(TelemetryCommandProcessor processor, String queueName) {
        Jms config = new Jms();
        config.enabled = true;

        Destination destination = new Destination();
        destination.setName(queueName);

        NativeConnectionFactory factory = new NativeConnectionFactory();
        factory.className = "org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory";
        factory.destinations.add(destination);
        factory.constructorParameters.add("tcp://localhost:61616");

        config.getConnectionFactories().add(factory);

        JmsProcessorImpl jmsProcessor = new JmsProcessorImpl(processor, new MetricRegistry(), config);
        return jmsProcessor;
    }

    private int waitFor(DummyCommandProcessor commandProcessor, int nrOfMessages, long maxWaitTime) {
        long start = System.currentTimeMillis();
        while (commandProcessor.getTotalEventCount() != nrOfMessages) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (System.currentTimeMillis() - start > maxWaitTime) {
                break;
            }
        }
        return commandProcessor.getTotalEventCount();
    }

    private void addMessage(int nrOfMessages) throws JMSException {
        ActiveMQJMSConnectionFactory cf = new ActiveMQJMSConnectionFactory("tcp://localhost:61616");
        Connection connection = cf.createConnection();
        connection.start();
        Session session = connection.createSession();
        MessageProducer producer = session.createProducer(session.createQueue("etm.queue.1"));
        for (int i = 0; i < nrOfMessages; i++) {
            TextMessage textMessage = session.createTextMessage("Test message " + i);
            producer.send(textMessage);
        }
        producer.close();
        session.close();
        connection.stop();
    }

}
