package com.jecstar.etm.processor.kafka;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.kafka.configuration.Kafka;
import com.jecstar.etm.processor.kafka.configuration.Topic;
import com.jecstar.etm.processor.reader.DestinationReaderPool;
import com.jecstar.etm.server.core.util.NamedThreadFactory;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class KafkaProcessorImpl implements KafkaProcessor {

    private final TelemetryCommandProcessor processor;
    private final MetricRegistry metricRegistry;
    private final Kafka config;
    private ArrayList<DestinationReaderPool<KafkaDestinationReader>> readerPools = new ArrayList<>();

    private ExecutorService executorService;

    public KafkaProcessorImpl(TelemetryCommandProcessor processor, MetricRegistry metricRegistry, Kafka config) {
        this.processor = processor;
        this.metricRegistry = metricRegistry;
        this.config = config;
    }

    @Override
    public void start() {
        if (this.config.getNumberOfListeners() <= 0) {
            return;
        }
        this.executorService = Executors.newFixedThreadPool(this.config.getNumberOfListeners(), new NamedThreadFactory("kafka_processor"));
        for (Topic topic : this.config.getTopics()) {
            DestinationReaderPool<KafkaDestinationReader> readerPool = new DestinationReaderPool<>(
                    this.processor,
                    this.executorService,
                    topic.getName(),
                    topic.getNrOfListeners(),
                    topic.getNrOfListeners(),
                    f -> new KafkaDestinationReader(
                            this.processor,
                            this.metricRegistry,
                            topic,
                            f
                    )
            );
            Gauge<Integer> readerPoolGauge = readerPool::getNumberOfActiveReaders;
            this.metricRegistry.register("kafka-processor.readerpool." + topic.getName().replaceAll("\\.", "_") + ".size", readerPoolGauge);
            this.readerPools.add(readerPool);
        }
    }


    @Override
    public void stop() {
        for (DestinationReaderPool<KafkaDestinationReader> readerPool : this.readerPools) {
            readerPool.stop();
        }
        this.readerPools.clear();
        if (this.executorService != null) {
            this.executorService.shutdownNow();
            try {
                this.executorService.awaitTermination(15, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            this.executorService = null;
        }
    }

}
