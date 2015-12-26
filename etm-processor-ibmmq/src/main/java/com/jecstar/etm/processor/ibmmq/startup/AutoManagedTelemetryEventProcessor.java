package com.jecstar.etm.processor.ibmmq.startup;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.elasticsearch.client.Client;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.core.TelemetryEvent;
import com.jecstar.etm.core.configuration.ConfigurationChangeListener;
import com.jecstar.etm.core.configuration.ConfigurationChangedEvent;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.processor.processor.PersistenceEnvironment;
import com.jecstar.etm.processor.processor.TelemetryEventProcessor;

public class AutoManagedTelemetryEventProcessor implements ConfigurationChangeListener {
	
	private final TelemetryEventProcessor processor;
	private final EtmConfiguration etmConfiguration;
	private final PersistenceEnvironment persistenceEnvironment;
	private final Client elasticClient;
	
	public AutoManagedTelemetryEventProcessor(final EtmConfiguration etmConfiguration, final PersistenceEnvironment persistenceEnvironment, final Client elasticClient) {
		this.etmConfiguration = etmConfiguration;
		this.etmConfiguration.addConfigurationChangeListener(this);
		this.persistenceEnvironment = persistenceEnvironment;
		this.elasticClient = elasticClient;
		this.processor = new TelemetryEventProcessor();
	}
	
	public void start() {
		this.processor.start(Executors.newCachedThreadPool(new EtmThreadFactory()), this.persistenceEnvironment, this.elasticClient, this.etmConfiguration, new MetricRegistry());
	}
	
	public void stop() {
		this.processor.stopAll();
	}
	
	public void processTelemetryEvent(final TelemetryEvent telemetryEvent) {
		this.processor.processTelemetryEvent(telemetryEvent);
	}

	@Override
	public void configurationChanged(ConfigurationChangedEvent event) {
		if (event.isAnyChanged(EtmConfiguration.CONFIG_KEY_ENHANCING_HANDLER_COUNT,
				EtmConfiguration.CONFIG_KEY_PERSISTING_HANDLER_COUNT, EtmConfiguration.CONFIG_KEY_EVENT_BUFFER_SIZE)) {
			if (processor != null) {
//			if (log.isInfoLevelEnabled()) {
//				log.logInfoMessage("Detected a change in the configuration that needs to restart ETM processor.");
//			}
				processor.hotRestart();
//			if (log.isInfoLevelEnabled()) {
//				log.logInfoMessage("Restarted ETM processor.");
//			}
			}
		}
	}

	private static class EtmThreadFactory implements ThreadFactory {
		private static final AtomicInteger poolNumber = new AtomicInteger(1);
		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String namePrefix;

		EtmThreadFactory() {
			SecurityManager securityManager = System.getSecurityManager();
			this.group = (securityManager != null) ? securityManager.getThreadGroup()
					: Thread.currentThread().getThreadGroup();
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
