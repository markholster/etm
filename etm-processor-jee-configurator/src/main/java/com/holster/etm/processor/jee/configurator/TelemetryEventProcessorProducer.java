package com.holster.etm.processor.jee.configurator;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.ManagedBean;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrServer;

import com.datastax.driver.core.Session;
import com.holster.etm.jee.configurator.core.ProcessorConfiguration;
import com.holster.etm.processor.processor.TelemetryEventProcessor;

@ManagedBean
@Singleton
public class TelemetryEventProcessorProducer {

	@ProcessorConfiguration
	@Inject
	private Properties configration;

	@ProcessorConfiguration
	@Inject
	private Session session;

	@ProcessorConfiguration
	@Inject
	private SolrServer solrServer;

	private TelemetryEventProcessor telemetryEventProcessor;

	@Produces
	@ProcessorConfiguration
	public TelemetryEventProcessor getTelemetryEventProcessor() {
		synchronized (this) {
			if (this.telemetryEventProcessor == null) {
				this.telemetryEventProcessor = new TelemetryEventProcessor();
				int enhancingHandlerCount = Integer.valueOf(this.configration.getProperty("etm.enhancing_handler_count", "5"));
				int indexingHandlerCount = Integer.valueOf(this.configration.getProperty("etm.indexing_handler_count", "5"));
				int persistingHandlerCount = Integer.valueOf(this.configration.getProperty("etm.persisting_handler_count", "5"));
				int ringbufferSize = Integer.valueOf(this.configration.getProperty("etm.ringbuffer_size", "4096"));
				String keyspace = this.configration.getProperty("cassandra.keyspace", "etm");
				this.telemetryEventProcessor.start(Executors.newCachedThreadPool(new EtmThreadFactory()), this.session, this.solrServer,
				        ringbufferSize, enhancingHandlerCount, indexingHandlerCount, persistingHandlerCount, keyspace);
			}
		}
		return this.telemetryEventProcessor;
	}

	@PreDestroy
	public void preDestroy() {
		if (this.telemetryEventProcessor != null) {
			this.telemetryEventProcessor.stopAll();
		}
	}

	private static class EtmThreadFactory implements ThreadFactory {
		private static final AtomicInteger poolNumber = new AtomicInteger(1);
		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String namePrefix;

		EtmThreadFactory() {
			SecurityManager securityManager = System.getSecurityManager();
			this.group = (securityManager != null) ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
			this.namePrefix = "etmpool-" + poolNumber.getAndIncrement() + "-thread-";
		}

		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(this.group, runnable, this.namePrefix + this.threadNumber.getAndIncrement(), 0);
			if (thread.isDaemon()) {
				thread.setDaemon(false);
			}
			if (thread.getPriority() != Thread.NORM_PRIORITY) {
				thread.setPriority(Thread.NORM_PRIORITY);
			}
			return thread;
		}
	}
}
