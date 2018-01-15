package com.jecstar.etm.processor.ibmmq;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.ibmmq.configuration.Destination;
import com.jecstar.etm.processor.ibmmq.configuration.IbmMq;
import com.jecstar.etm.processor.ibmmq.configuration.QueueManager;
import com.jecstar.etm.processor.reader.DestinationReaderPool;
import com.jecstar.etm.server.core.util.NamedThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class IbmMqProcessorImpl implements IbmMqProcessor {

	private final TelemetryCommandProcessor processor;
	private final MetricRegistry metricRegistry;
	private final String instanceName;
	private final String clusterName;
	private final IbmMq config;
	private ExecutorService executorService;
	private List<DestinationReaderPool<IbmMqDestinationReader>> readerPools = new ArrayList<>();

	public IbmMqProcessorImpl(TelemetryCommandProcessor processor, MetricRegistry metricRegistry, IbmMq config, String clusterName, String instanceName) {
		this.processor = processor;
		this.metricRegistry = metricRegistry;
		this.config = config;
		this.clusterName = clusterName;
		this.instanceName = instanceName;
	}

	@Override
	public void start() {
		if (this.config.getMinimumNumberOfListeners() <= 0) {
			return;
		}
        this.executorService = new ThreadPoolExecutor(this.config.getMinimumNumberOfListeners(), this.config.getMaximumNumberOfListeners(),
                0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(),
                new NamedThreadFactory("ibm_mq_processor"));
		for (QueueManager queueManager : this.config.getQueueManagers()) {
			for (Destination destination : queueManager.getDestinations()) {
                DestinationReaderPool<IbmMqDestinationReader> readerPool = new DestinationReaderPool<>(
                        this.processor,
                        this.executorService,
                        destination.getName(),
                        destination.getMinNrOfListeners(),
                        destination.getMaxNrOfListeners(),
                        f -> new IbmMqDestinationReader(
                                this.clusterName + "_" + this.instanceName,
                                this.processor,
                                this.metricRegistry,
                                queueManager,
                                destination,
                                f
                        )
                );
                Gauge<Integer> readerPoolGauge = readerPool::getNumberOfActiveReaders;
                this.metricRegistry.register("ibmmq-processor.readerpool." + destination.getName().replaceAll("\\.", "_") + ".size", readerPoolGauge);
                this.readerPools.add(readerPool);
            }
		}
	}

	@Override
	public void stop() {
	    for (DestinationReaderPool<IbmMqDestinationReader> readerPool : this.readerPools) {
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
