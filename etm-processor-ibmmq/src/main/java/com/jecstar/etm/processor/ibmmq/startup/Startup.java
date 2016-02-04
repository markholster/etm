package com.jecstar.etm.processor.ibmmq.startup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.codahale.metrics.Slf4jReporter;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.processor.ibmmq.DestinationReader;
import com.jecstar.etm.processor.ibmmq.ElasticBackedEtmConfiguration;
import com.jecstar.etm.processor.ibmmq.PersistenceEnvironmentElasticImpl;
import com.jecstar.etm.processor.ibmmq.configuration.Configuration;
import com.jecstar.etm.processor.ibmmq.configuration.Destination;
import com.jecstar.etm.processor.ibmmq.configuration.QueueManager;
import com.jecstar.etm.processor.processor.PersistenceEnvironment;

public class Startup {
	
	private static final LogWrapper log = LogFactory.getLogger(Startup.class);
	
	private Node node;
	private Client elasticClient;
	private EtmConfiguration etmConfiguration;
	private PersistenceEnvironment persistenceEnvironment;
	private AutoManagedTelemetryEventProcessor processor;
	private Slf4jReporter metricReporter;

	private ExecutorService executorService;
	private ScheduledExecutorService scheduledExecutorService;

	public void launch(String baseDir, String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (Startup.this.scheduledExecutorService != null) {
					if (log.isInfoLevelEnabled()) {
						log.logInfoMessage("Shutting down scheduler executors...");
					}
					Startup.this.scheduledExecutorService.shutdown();
					try {
						Startup.this.scheduledExecutorService.awaitTermination(1, TimeUnit.MINUTES);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}				
				if (Startup.this.executorService != null) {
					if (log.isInfoLevelEnabled()) {
						log.logInfoMessage("Shutting down listeners...");
					}
					Startup.this.executorService.shutdown();
					try {
						Startup.this.executorService.awaitTermination(1, TimeUnit.MINUTES);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
				if (Startup.this.processor != null) {
					if (log.isInfoLevelEnabled()) {
						log.logInfoMessage("Shutting down event processor...");
					}
					try { Startup.this.processor.stop(); } catch (Throwable t) {}
				}
				if (Startup.this.persistenceEnvironment != null) {
					if (log.isInfoLevelEnabled()) {
						log.logInfoMessage("Closing persistence environment...");
					}
					try { Startup.this.persistenceEnvironment.close(); } catch (Throwable t) {}
				}
 				if (Startup.this.elasticClient != null) {
					if (log.isInfoLevelEnabled()) {
						log.logInfoMessage("Closing cluster client...");
					}
 					try { Startup.this.elasticClient.close(); } catch (Throwable t) {}
				}
 				if (Startup.this.node != null) {
					if (log.isInfoLevelEnabled()) {
						log.logInfoMessage("Closing cluster node...");
					}
 					try { Startup.this.node.close(); } catch (Throwable t) {}
 				}
				if (Startup.this.metricReporter != null) {
					if (log.isInfoLevelEnabled()) {
						log.logInfoMessage("Shutting down metric reporter...");
					}
 					try { Startup.this.metricReporter.stop(); } catch (Throwable t) {}					
				}

				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Shutdown complete.");
				}
			}
		});
		Configuration configuration = loadConfiguration(baseDir, args);
		if (configuration == null) {
			return;
		}
		int nrOfListeners = configuration.getTotalNumberOfListeners();
		if (nrOfListeners < 1) {
			if (log.isInfoLevelEnabled()) {
				log.logInfoMessage("No listeners configured, nothing to do.");
			}
			return;
		}
		this.node = createElasticNode(configuration);
		if (this.node == null) {
			return;
		}
		this.elasticClient = this.node.client();
		this.etmConfiguration = new ElasticBackedEtmConfiguration(configuration.getCalculatedNodeName(), "processor", this.elasticClient);
		this.persistenceEnvironment = new PersistenceEnvironmentElasticImpl(this.etmConfiguration, this.elasticClient);
		this.persistenceEnvironment.createEnvironment();
		this.processor = new AutoManagedTelemetryEventProcessor(this.etmConfiguration, this.persistenceEnvironment, this.elasticClient);
		this.processor.start();
		
		this.executorService = Executors.newFixedThreadPool(nrOfListeners);
		for (QueueManager queueManager : configuration.getQueueManagers()) {
			for (Destination destination : queueManager.getDestinations()) {
				for (int i=0; i < destination.getNrOfListeners(); i++) {
					this.executorService.submit(new DestinationReader(this.processor, queueManager, destination));
				}
			}
		}
		if (configuration.getFlushInterval() > 0) {
			this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
			this.scheduledExecutorService.scheduleAtFixedRate(
					new FlushRequestor(this.processor), 
					configuration.getFlushInterval(), 
					configuration.getFlushInterval(), 
					TimeUnit.MILLISECONDS);
		}
		
		if (configuration.isLogMetrics()) {
			this.metricReporter = Slf4jReporter.forRegistry(this.processor.getMetricRegistry())
		       .convertRatesTo(TimeUnit.SECONDS)
		       .convertDurationsTo(TimeUnit.MILLISECONDS)
		       .build();
			this.metricReporter.start(1, TimeUnit.MINUTES);
		}
		if (log.isInfoLevelEnabled()) {
			log.logInfoMessage("ETM Processor started.");
		}
	}
	
	private Configuration loadConfiguration(String baseDir, String[] args) {
		File configFile = null;
		if (args != null && args.length > 0) {
			for (String argument : args) {
				if (argument.startsWith("--config-file=")) {
					configFile = new File(argument.substring(14));
					break;
				}
			}
		}
		if (configFile == null) {
			if (baseDir != null) {
				configFile = new File(baseDir + File.separator + "etc", "etm.yml");
			} else {
				configFile = new File("etc", "etm.yml");
			}
		}
		if (log.isInfoLevelEnabled()) {
			log.logInfoMessage("Reading configuration from '" + configFile.getPath() + "'.");
		}
		try (FileReader reader = new FileReader(configFile)) {
			YamlReader ymlReader = new YamlReader(reader);
			ymlReader.getConfig().setClassTag("configuration", Configuration.class);
			ymlReader.getConfig().setClassTag("destination", Destination.class);
			return (Configuration) ymlReader.read();
		} catch (FileNotFoundException e) {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("No config file at '" + configFile.getPath() + "'. Using defaults instead.");
			}
			return new Configuration();
		} catch (IOException e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Failed to load configuration", e);
			}
		}		
		return null;
	}
	
	private Node createElasticNode(Configuration configuration) {
		try {
			Builder settings = Settings.settingsBuilder().put("client.transport.sniff", true)
					.put("cluster.name", configuration.getClusterName())
					.put("path.home", "./");
			String masterAddresses = configuration.getMasterAddresses();
			if (masterAddresses != null) {
				settings.put("node.master", false);
				settings.put("discovery.zen.ping.multicast.enabled", false);
				settings.put("discovery.zen.ping.unicast.hosts", masterAddresses);
			} 
			settings.put("http.enabled", false);
			settings.put("node.name", configuration.getCalculatedNodeName());
			return NodeBuilder.nodeBuilder().settings(settings)
				        .client(true)
				        .data(false)
				        .clusterName(configuration.getClusterName()).node();
	    } catch (Exception e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Failed to connect to cluster", e);
			}
	    }
		return null;
	}
}
