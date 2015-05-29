package com.jecstar.etm.processor.jee.configurator;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.ManagedBean;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.core.configuration.ConfigurationChangeListener;
import com.jecstar.etm.core.configuration.ConfigurationChangedEvent;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.jee.configurator.core.ProcessorConfiguration;
import com.jecstar.etm.processor.processor.PersistenceEnvironment;
import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;

@ManagedBean
@Singleton
public class TelemetryEventCommandProducer implements ConfigurationChangeListener {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(TelemetryEventCommandProducer.class);
	
	@ProcessorConfiguration
	@Inject
	private EtmConfiguration configration;

	@ProcessorConfiguration
	@Inject
	private PersistenceEnvironment persistenceEnvironment;

	@ProcessorConfiguration
	@Inject
	private SolrClient solrClient;

	@ProcessorConfiguration
	@Inject
	private MetricRegistry metricRegistry;

	
	private TelemetryCommandProcessor telemetryCommandProcessor;

	@Produces
	@ProcessorConfiguration
	public TelemetryCommandProcessor getTelemetryEventProcessor() {
		synchronized (this) {
			if (this.telemetryCommandProcessor == null) {
				this.telemetryCommandProcessor = new TelemetryCommandProcessor();
				this.configration.addEtmConfigurationChangeListener(this);
				this.telemetryCommandProcessor.start(Executors.newCachedThreadPool(new EtmThreadFactory()), this.persistenceEnvironment, this.solrClient,
				        this.configration, this.metricRegistry);
			}
		}
		return this.telemetryCommandProcessor;
	}

	@PreDestroy
	public void preDestroy() {
		if (this.telemetryCommandProcessor != null) {
			this.telemetryCommandProcessor.stopAll();
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

	@Override
    public void configurationChanged(ConfigurationChangedEvent event) {
		if (event.isAnyChanged(EtmConfiguration.ETM_ENHANCING_HANDLER_COUNT, EtmConfiguration.ETM_INDEXING_HANDLER_COUNT,
		        EtmConfiguration.ETM_PERSISTING_HANDLER_COUNT, EtmConfiguration.ETM_RINGBUFFER_SIZE)) {
			if (this.telemetryCommandProcessor != null) {
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Detected a change in the configuration that needs to restart ETM processor.");
				}
				this.telemetryCommandProcessor.hotRestart();
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Restarted ETM processor.");
				}
			}
		}
    }
}
