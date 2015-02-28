package com.jecstar.etm.processor.jee.configurator;

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
import com.jecstar.etm.core.configuration.ConfigurationChangeListener;
import com.jecstar.etm.core.configuration.ConfigurationChangedEvent;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.jee.configurator.core.ProcessorConfiguration;
import com.jecstar.etm.processor.processor.TelemetryEventProcessor;

@ManagedBean
@Singleton
public class TelemetryEventProcessorProducer implements ConfigurationChangeListener {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(TelemetryEventProcessorProducer.class);
	
	@ProcessorConfiguration
	@Inject
	private EtmConfiguration configration;

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
				this.configration.addEtmConfigurationChangeListener(this);
				this.telemetryEventProcessor.start(Executors.newCachedThreadPool(new EtmThreadFactory()), this.session, this.solrServer,
				        this.configration);
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

	@Override
    public void configurationChanged(ConfigurationChangedEvent event) {
		if (event.isAnyChanged(EtmConfiguration.ETM_ENHANCING_HANDLER_COUNT, EtmConfiguration.ETM_INDEXING_HANDLER_COUNT,
		        EtmConfiguration.ETM_PERSISTING_HANDLER_COUNT, EtmConfiguration.ETM_RINGBUFFER_SIZE)) {
			if (this.telemetryEventProcessor != null) {
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Detected a change in the configuration that needs to restart ETM processor.");
				}
				this.telemetryEventProcessor.hotRestart();
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Restarted ETM processor.");
				}
			}
		}
    }
}
