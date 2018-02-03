package com.jecstar.etm.processor.kafka;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.kafka.configuration.Topic;
import com.jecstar.etm.processor.kafka.handler.EtmEventHandler;
import com.jecstar.etm.processor.reader.DestinationReader;
import com.jecstar.etm.processor.reader.DestinationReaderInstantiationContext;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.config.types.Password;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.security.auth.SecurityProtocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class KafkaDestinationReader implements DestinationReader {

    private static final LogWrapper log = LogFactory.getLogger(KafkaDestinationReader.class);

    private final Timer kafkaPollTimer;
    private final Topic topic;
    private final EtmEventHandler etmEventHandler;

    private KafkaConsumer<String, String> consumer;

    private boolean stop = false;

    KafkaDestinationReader(final TelemetryCommandProcessor processor,
                           final MetricRegistry metricRegistry,
                           final Topic topic,
                           final DestinationReaderInstantiationContext<KafkaDestinationReader> instantiationContext) {
        this.etmEventHandler = new EtmEventHandler(processor);
        this.kafkaPollTimer = metricRegistry.timer("kafka-processor.poll." + topic.getName().replaceAll("\\.", "_"));
        this.topic = topic;
    }

    @Override
    public void stop() {
        this.stop = true;
        this.consumer.wakeup();
    }

    @Override
    public void run() {
        connect();
        List<String> topics = new ArrayList<>(1);
        topics.add(this.topic.getName());
        try {
            this.consumer.subscribe(topics);
            if (this.topic.getStartFrom() != null) {
                // Make sure the consumer has an assignment.
                this.consumer.poll(0);
                if (this.topic.startFromBeginning()) {
                    this.consumer.seekToBeginning(this.consumer.assignment());
                }
            }
            while (!this.stop) {
                ConsumerRecords<String, String> records;
                final Timer.Context kafkaPollContext = this.kafkaPollTimer.time();
                try {
                    records = this.consumer.poll(100);
                } finally {
                    kafkaPollContext.stop();
                }
                for (ConsumerRecord<String, String> record : records) {
                    this.etmEventHandler.handleMessage(record);
                }
                this.consumer.commitSync();
                if (Thread.currentThread().isInterrupted()) {
                    this.stop = true;
                }
            }
        } catch (WakeupException e) {

        } finally {
            this.consumer.close();
        }
    }

    private void connect() {
        if (log.isDebugLevelEnabled()) {
            log.logDebugMessage("Connecting to destination '" + this.topic.getName() + "'");
        }
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, this.topic.bootstrapServers.stream().collect(Collectors.joining(",")));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, this.topic.getGroupId());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        if (this.topic.getMaxPollRecords() > 0) {
            props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, this.topic.getMaxPollRecords());
        }
        if (this.topic.getMaxPollInterval() > 0) {
            props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, this.topic.getMaxPollInterval());
        }
        if (this.topic.getSessionTimeout() > 0) {
            props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, this.topic.getSessionTimeout());
        }
        if (this.topic.getHeartbeatInterval() > 0) {
            props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, this.topic.getHeartbeatInterval());
        }
        if (this.topic.getCipherSuites().size() > 0) {
            props.put(SslConfigs.SSL_CIPHER_SUITES_CONFIG, this.topic.getCipherSuites());
        }
        if (this.topic.getSslProtocols() != null) {
            props.put(SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG, this.topic.getSslProtocols());
        }
        if (this.topic.getSslTruststoreLocation() != null) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name);
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, this.topic.getSslTruststoreLocation().getPath());
            props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, this.topic.getSslTruststoreType());
            if (this.topic.getSslTruststorePassword() != null) {
                props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, new Password(this.topic.getSslTruststorePassword()));
            }
        }
        if (this.topic.getSslKeystoreLocation() != null) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name);
            props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, this.topic.getSslKeystoreLocation().getPath());
            props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, this.topic.getSslKeystoreType());
            if (this.topic.getSslKeystorePassword() != null) {
                props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, new Password(this.topic.getSslKeystorePassword()));
            }
        }

        this.consumer = new KafkaConsumer<>(props);
    }
}
