package com.jecstar.etm.launcher;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.jecstar.etm.launcher.background.HttpSessionCleaner;
import com.jecstar.etm.launcher.background.IndexCleaner;
import com.jecstar.etm.launcher.background.LicenseUpdater;
import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.launcher.http.ElasticsearchIdentityManager;
import com.jecstar.etm.launcher.http.HttpServer;
import com.jecstar.etm.launcher.migrations.EtmMigrator;
import com.jecstar.etm.launcher.migrations.MultiTypeDetector;
import com.jecstar.etm.launcher.migrations.ReindexToSingleTypeMigration;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.core.TelemetryCommandProcessorImpl;
import com.jecstar.etm.processor.elastic.PersistenceEnvironmentElasticImpl;
import com.jecstar.etm.processor.ibmmq.IbmMqProcessor;
import com.jecstar.etm.processor.ibmmq.configuration.IbmMq;
import com.jecstar.etm.processor.internal.persisting.BusinessEventLogger;
import com.jecstar.etm.processor.internal.persisting.InternalBulkProcessorWrapper;
import com.jecstar.etm.processor.jms.JmsProcessor;
import com.jecstar.etm.processor.jms.JmsProcessorImpl;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.util.NamedThreadFactory;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.discovery.MasterNotDiscoveredException;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class Launcher {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(Launcher.class);
	
	private ElasticsearchIndexTemplateCreator indexTemplateCreator;
	private TelemetryCommandProcessor processor;
	private HttpServer httpServer;
	private Client elasticClient;
	private ScheduledReporter metricReporter;
	private IbmMqProcessor ibmMqProcessor;
	private JmsProcessor jmsProcessor;
	private ScheduledExecutorService backgroundScheduler;
	private InternalBulkProcessorWrapper bulkProcessorWrapper;
	
	public void launch(CommandLineParameters commandLineParameters, Configuration configuration, InternalBulkProcessorWrapper bulkProcessorWrapper) {
		// TODO maak een thread die periodiek door de LDAP entries heen loopt om te kijken of er gebruikers weggegooid moeten worden.
		this.bulkProcessorWrapper = bulkProcessorWrapper;
		addShutdownHooks(configuration);
		InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
		try {
			initializeElasticsearchClient(configuration);
			this.bulkProcessorWrapper.setClient(this.elasticClient);
            boolean reinitializeTemplates = executeDatabaseMigrations(this.elasticClient);
			new MultiTypeDetector().detect(this.elasticClient);
            this.indexTemplateCreator = new ElasticsearchIndexTemplateCreator(this.elasticClient);
            this.indexTemplateCreator.createTemplates();
            EtmConfiguration etmConfiguration = new ElasticBackedEtmConfiguration(configuration.instanceName, this.elasticClient);
            this.bulkProcessorWrapper.setConfiguration(etmConfiguration);
            this.indexTemplateCreator.addConfigurationChangeNotificationListener(etmConfiguration);
            if (commandLineParameters.isReinitialize() || reinitializeTemplates) {
                this.indexTemplateCreator.reinitialize();
                if (!commandLineParameters.isQuiet()) {
                    System.out.println("Reinitialized system.");
                }
            }
            MetricRegistry metricRegistry = new MetricRegistry();
			initializeMetricReporter(metricRegistry, configuration);
			initializeProcessor(metricRegistry, configuration, etmConfiguration);
			initializeBackgroundProcesses(configuration, etmConfiguration, this.elasticClient);
			
			if (configuration.isHttpServerNecessary()) {
				System.setProperty("org.jboss.logging.provider", "slf4j");
				this.httpServer = new HttpServer(new ElasticsearchIdentityManager(this.elasticClient, etmConfiguration), configuration, etmConfiguration, this.processor, this.elasticClient);
				this.httpServer.start();
			}
			if (configuration.ibmMq.enabled) {
				initializeMqProcessor(metricRegistry, configuration);
			}
			if (configuration.jms.enabled) {
				initializeJmsProcessor(metricRegistry, configuration);
			}

			if (!commandLineParameters.isQuiet()) {
				System.out.println("Enterprise Telemetry Monitor started.");
			}
			if (log.isInfoLevelEnabled()) {
				log.logInfoMessage("Enterprise Telemetry Monitor started.");
			}
			BusinessEventLogger.logEtmStartup();
		} catch (Exception e) {
			if (!commandLineParameters.isQuiet()) {
				e.printStackTrace();
			}
			if (log.isFatalLevelEnabled()) {
				log.logFatalMessage("Error launching Enterprise Telemetry Monitor", e);
			}
		}		
	}

	private void addShutdownHooks(Configuration configuration) {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (log.isInfoLevelEnabled()) {
                log.logInfoMessage("Shutting down Enterprise Telemetry Monitor.");
            }
            if (Launcher.this.indexTemplateCreator != null) {
                try { Launcher.this.indexTemplateCreator.removeConfigurationChangeNotificationListener(); } catch (Throwable t) {}
            }
            if (Launcher.this.backgroundScheduler != null) {
                try { Launcher.this.backgroundScheduler.shutdownNow(); } catch (Throwable t) {}
            }
            if (Launcher.this.jmsProcessor != null) {
				try { Launcher.this.jmsProcessor.stop(); } catch (Throwable t) {}
			}
            if (Launcher.this.ibmMqProcessor != null) {
                try { Launcher.this.ibmMqProcessor.stop(); } catch (Throwable t) {}
            }
            if (Launcher.this.httpServer != null) {
                try { Launcher.this.httpServer.stop(); } catch (Throwable t) {}
            }
            if (Launcher.this.processor != null) {
                try { Launcher.this.processor.stopAll(); } catch (Throwable t) {}
            }
            if (Launcher.this.metricReporter != null) {
                try { Launcher.this.metricReporter.close(); } catch (Throwable t) {}
            }
            if (Launcher.this.bulkProcessorWrapper != null) {
                try {
                    BusinessEventLogger.logEtmShutdown();
                    Launcher.this.bulkProcessorWrapper.close();
                } catch (Throwable t) {}
            }
            if (Launcher.this.elasticClient != null) {
                try { Launcher.this.elasticClient.close(); } catch (Throwable t) {}
            }
        }));
	}
	
	
	private void initializeBackgroundProcesses(final Configuration configuration, final EtmConfiguration etmConfiguration, final Client client) {
		int threadPoolSize = 2;
		if (configuration.http.guiEnabled || configuration.http.restProcessorEnabled) {
			threadPoolSize++;
		}
		this.backgroundScheduler = new ScheduledThreadPoolExecutor(threadPoolSize, new NamedThreadFactory("etm_background_scheduler"));
		this.backgroundScheduler.scheduleAtFixedRate(new LicenseUpdater(etmConfiguration, client), 0, 6, TimeUnit.HOURS);
		this.backgroundScheduler.scheduleAtFixedRate(new IndexCleaner(etmConfiguration, client), 1, 15, TimeUnit.MINUTES);
		if (configuration.http.guiEnabled) {
			this.backgroundScheduler.scheduleAtFixedRate(new HttpSessionCleaner(etmConfiguration, client), 2, 15, TimeUnit.MINUTES);
		}
		
	}

	private void initializeProcessor(MetricRegistry metricRegistry, Configuration configuration, EtmConfiguration etmConfiguration) {
		if (this.processor == null) {
			this.processor = new TelemetryCommandProcessorImpl(metricRegistry);
			this.processor.start(new NamedThreadFactory("etm_processor"), new PersistenceEnvironmentElasticImpl(etmConfiguration, this.elasticClient), etmConfiguration);
		}
	}

    /**
     * Execute all necessary Elasticsearch data migrations.
     * @param client The Elasticsearch client.
     * @return <code>true</code> when the index templates need to be reinitialized, <code>false</code> otherwise.
     */
	private boolean executeDatabaseMigrations(Client client) {
        EtmMigrator etmMigrator = new ReindexToSingleTypeMigration(client);
        if (etmMigrator.shouldBeExecuted())  {
            etmMigrator.migrate();
            return true;
        }
        return false;
	}
	
	private void initializeElasticsearchClient(Configuration configuration) {
		if (this.elasticClient != null) {
			return;
		}
		Builder settingsBuilder = Settings.builder()
			.put("cluster.name", configuration.elasticsearch.clusterName)
			.put("client.transport.sniff", true);
		TransportClient transportClient;
		if (configuration.elasticsearch.username != null && configuration.elasticsearch.password != null) {
			settingsBuilder.put("xpack.security.user", configuration.elasticsearch.username + ":" + configuration.elasticsearch.password);
			if (configuration.elasticsearch.sslKeyLocation != null) {
				settingsBuilder.put("xpack.ssl.key", configuration.elasticsearch.sslKeyLocation.getAbsolutePath());
			}
			if (configuration.elasticsearch.sslCertificateLocation != null) {
				settingsBuilder.put("xpack.ssl.certificate", configuration.elasticsearch.sslCertificateLocation.getAbsolutePath());
			}
			if (configuration.elasticsearch.sslCertificateAuthoritiesLocation != null) {
				settingsBuilder.put("xpack.ssl.certificate_authorities", configuration.elasticsearch.sslCertificateAuthoritiesLocation.getAbsolutePath());
			}
			if (configuration.elasticsearch.sslEnabled) {
				settingsBuilder.put("xpack.security.transport.ssl.enabled", "true");
			}
			transportClient = new PreBuiltXPackTransportClient(settingsBuilder.build());
		} else {
			transportClient = new PreBuiltTransportClient(settingsBuilder.build());
		}
		String[] hosts = configuration.elasticsearch.connectAddresses.split(",");
		int hostsAdded = addElasticsearchHostsToTransportClient(hosts, transportClient);
		if (configuration.elasticsearch.waitForConnectionOnStartup) {
			while (hostsAdded == 0) {
				// Currently this can only happen in docker swarm installations where the elasticsearch service isn't fully started when ETM starts. This will result in a 
				// UnknownHostException so that leaves with a transportclient without any hosts. Also this may happen when the end users misspells the hostname in the configuration.
				if (Thread.currentThread().isInterrupted()) {
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				hostsAdded = addElasticsearchHostsToTransportClient(hosts, transportClient);
			}
			waitForActiveConnection(transportClient);
		}
		this.elasticClient = transportClient;
	}
	
	private int addElasticsearchHostsToTransportClient(String[] hosts, TransportClient transportClient) {
		int added = 0;
		for (String host : hosts) {
			TransportAddress transportAddress = createTransportAddress(host);
			if (transportAddress != null) {
				transportClient.addTransportAddress(transportAddress);
				added++;
			}
		}
		return added;
	}
	
	private TransportAddress createTransportAddress(String host) {
		int ix = host.lastIndexOf(":");
		if (ix != -1) {
			try {
				InetAddress inetAddress = InetAddress.getByName(host.substring(0, ix));
				int port = Integer.parseInt(host.substring(ix + 1));
				return new TransportAddress(inetAddress, port);
			} catch (UnknownHostException e) {
				if (log.isWarningLevelEnabled()) {
					log.logWarningMessage("Unable to connect to '" + host + "'", e);
				}
			}
		}
		return null;
	}

	private void waitForActiveConnection(TransportClient transportClient) {
		while(transportClient.connectedNodes().isEmpty()) {
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
			// Wait for any elasticsearch node to become active.
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		boolean esClusterInitialized = false;
		while (!esClusterInitialized) {
			try {
                ClusterHealthResponse clusterHealthResponse = transportClient.admin().cluster().prepareHealth().get();
                if (clusterHealthResponse.getInitializingShards() == 0
                        && clusterHealthResponse.getNumberOfPendingTasks() == 0
                        && clusterHealthResponse.getNumberOfDataNodes() > 0) {
                    esClusterInitialized = true;
                }
			} catch (MasterNotDiscoveredException | ClusterBlockException e) {}
			if (!esClusterInitialized) {
                // Wait for all shards to be initialized and no more tasks pending and at least 1 data node to be available.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
            }
		}
	
		
	}

	private void initializeMqProcessor(MetricRegistry metricRegistry, Configuration configuration) {
		try {
			Class<?> clazz = Class.forName("com.jecstar.etm.processor.ibmmq.IbmMqProcessorImpl");
			this.ibmMqProcessor = (IbmMqProcessor) clazz
					.getConstructor(
							TelemetryCommandProcessor.class,
							MetricRegistry.class,
							IbmMq.class, 
							String.class,
							String.class
						).newInstance(
							this.processor, 
							metricRegistry,
							configuration.ibmMq, 
							configuration.clusterName, 
							configuration.instanceName);
			this.ibmMqProcessor.start();
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			if (log.isWarningLevelEnabled()) {
				log.logWarningMessage("Unable to instantiate Ibm MQ Processor. Is the \"com.ibm.mq.allclient.jar\" file added to the lib/ext directory?", e);
			}
		}
	}

	private void initializeJmsProcessor(MetricRegistry metricRegistry, Configuration configuration) {
		this.jmsProcessor = new JmsProcessorImpl(this.processor, metricRegistry, configuration.jms);
		this.jmsProcessor.start();
	}
	
	
	private void initializeMetricReporter(MetricRegistry metricRegistry, Configuration configuration) {
		this.metricReporter = new MetricReporterElasticImpl(metricRegistry, configuration.instanceName, this.elasticClient);
		this.metricReporter.start(1, TimeUnit.MINUTES);
	}
	


}
