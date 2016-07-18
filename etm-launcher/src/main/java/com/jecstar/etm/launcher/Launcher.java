package com.jecstar.etm.launcher;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.launcher.http.ElasticsearchIdentityManager;
import com.jecstar.etm.launcher.http.HttpServer;
import com.jecstar.etm.launcher.retention.IndexCleaner;
import com.jecstar.etm.processor.elastic.PersistenceEnvironmentElasticImpl;
import com.jecstar.etm.processor.ibmmq.IbmMqProcessor;
import com.jecstar.etm.processor.ibmmq.configuration.IbmMq;
import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.slf4j.InternalEtmLogForwarder;

public class Launcher {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(Launcher.class);

	private ElasticsearchIndextemplateCreator indexTemplateCreator;
	private TelemetryCommandProcessor processor;
	private HttpServer httpServer;
	private Client elasticClient;
	private ScheduledReporter metricReporter;
	private IbmMqProcessor ibmMqProcessor;
	private ScheduledExecutorService retentionScheduler;

	
	public void launch(CommandLineParameters commandLineParameters, Configuration configuration) {
		addShutdownHooks();
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
		try {
			initializeElasticsearchClient(configuration);
			this.indexTemplateCreator = new ElasticsearchIndextemplateCreator(this.elasticClient);
			this.indexTemplateCreator.createTemplates();
			EtmConfiguration etmConfiguration = new ElasticBackedEtmConfiguration(configuration.instanceName, this.elasticClient);
			this.indexTemplateCreator.addConfigurationChangeNotificationListener(etmConfiguration);
			MetricRegistry metricRegistry = new MetricRegistry();
			initializeMetricReporter(metricRegistry, configuration);
			initializeProcessor(metricRegistry, configuration, etmConfiguration);
			InternalEtmLogForwarder.processor = processor;
			initializeIndexCleaner(etmConfiguration, this.elasticClient);
			
			if (configuration.isHttpServerNecessary()) {
				System.setProperty("org.jboss.logging.provider", "slf4j");
				this.httpServer = new HttpServer(new ElasticsearchIdentityManager(this.elasticClient), configuration, etmConfiguration, this.processor, this.elasticClient);
				this.httpServer.start();
			}
			if (configuration.ibmMq.enabled) {
				initializeMqProcessor(configuration);
			}
			if (!commandLineParameters.isQuiet()) {
				System.out.println("Enterprise Telemetry Monitor started.");
			}
			if (log.isInfoLevelEnabled()) {
				log.logInfoMessage("Enterprise Telemetry Monitor started.");
			}
		} catch (Exception e) {
			if (!commandLineParameters.isQuiet()) {
				e.printStackTrace();
			}
			if (log.isFatalLevelEnabled()) {
				log.logFatalMessage("Error launching Enterprise Telemetry Monitor", e);
			}
		}		
	}

	private void addShutdownHooks() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Shutting down Enterprise Telemetry Monitor.");
				}
				if (Launcher.this.indexTemplateCreator != null) {
					try { Launcher.this.indexTemplateCreator.removeConfigurationChangeNotificationListener(); } catch (Throwable t) {}
				}
				if (Launcher.this.retentionScheduler != null) {
					try { Launcher.this.retentionScheduler.shutdownNow(); } catch (Throwable t) {}
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
				if (Launcher.this.elasticClient != null) {
					try { Launcher.this.elasticClient.close(); } catch (Throwable t) {}
				}
			}
		});
	}
	
	
	private void initializeIndexCleaner(EtmConfiguration etmConfiguration, Client client) {
		this.retentionScheduler = new ScheduledThreadPoolExecutor(1);
		this.retentionScheduler.scheduleAtFixedRate(new IndexCleaner(etmConfiguration, client), 0, 15, TimeUnit.MINUTES);
	}

	
	private void initializeProcessor(MetricRegistry metricRegistry, Configuration configuration, EtmConfiguration etmConfiguration) {
		if (this.processor == null) {
			this.processor = new TelemetryCommandProcessor(metricRegistry);
			this.processor.start(Executors.defaultThreadFactory(), new PersistenceEnvironmentElasticImpl(etmConfiguration, this.elasticClient), etmConfiguration);
		}
	}
	
	private void initializeElasticsearchClient(Configuration configuration) {
		if (this.elasticClient != null) {
			return;
		}
		TransportClient transportClient = TransportClient.builder().settings(Settings.builder()
				.put("cluster.name", configuration.clusterName)
				.put("client.transport.sniff", true)).build();
		String[] hosts = configuration.elasticsearch.connectAddresses.split(",");
		for (String host : hosts) {
			int ix = host.lastIndexOf(":");
			if (ix != -1) {
				try {
					InetAddress inetAddress = InetAddress.getByName(host.substring(0, ix));
					int port = Integer.parseInt(host.substring(ix + 1));
					transportClient.addTransportAddress(new InetSocketTransportAddress(inetAddress, port));
				} catch (UnknownHostException e) {
					if (log.isWarningLevelEnabled()) {
						log.logWarningMessage("Unable to connect to '" + host + "'", e);
					}
				}
			}
		}
		this.elasticClient = transportClient;
	}
	
	private void initializeMqProcessor(Configuration configuration) {
		try {
			Class<?> clazz = Class.forName("com.jecstar.etm.processor.ibmmq.IbmMqProcessorImpl");
			this.ibmMqProcessor = (IbmMqProcessor) clazz
					.getConstructor(
							TelemetryCommandProcessor.class, 
							IbmMq.class, 
							String.class,
							String.class
						).newInstance(
							this.processor, 
							configuration.ibmMq, 
							configuration.clusterName, 
							configuration.instanceName);
			this.ibmMqProcessor.start();
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			if (log.isWarningLevelEnabled()) {
				log.logWarningMessage("Unable to instantiate Ibm MQ Processor. Is the \"com.ibm.mq.allclient.jar\" file added to the lib directory?", e);
			}
		}
	}
	
	
	private void initializeMetricReporter(MetricRegistry metricRegistry, Configuration configuration) {
		this.metricReporter = new MetricReporterElasticImpl(metricRegistry, configuration.instanceName, this.elasticClient);
		this.metricReporter.start(1, TimeUnit.MINUTES);

	}
}
