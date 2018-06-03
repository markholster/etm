package com.jecstar.etm.launcher;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.jecstar.etm.launcher.background.HttpSessionCleaner;
import com.jecstar.etm.launcher.background.IndexCleaner;
import com.jecstar.etm.launcher.background.LdapSynchronizer;
import com.jecstar.etm.launcher.background.LicenseUpdater;
import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.launcher.http.ElasticsearchIdentityManager;
import com.jecstar.etm.launcher.http.HttpServer;
import com.jecstar.etm.launcher.migrations.EtmMigrator;
import com.jecstar.etm.launcher.migrations.MultiTypeDetector;
import com.jecstar.etm.launcher.migrations.v3.EndpointHandlerToSingleListMigrator;
import com.jecstar.etm.launcher.migrations.v3.SearchTemplateHandlingTimeMigrator;
import com.jecstar.etm.launcher.migrations.v3.Version2xTo3xMigrator;
import com.jecstar.etm.launcher.migrations.v3.Version300To301Migrator;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.core.TelemetryCommandProcessorImpl;
import com.jecstar.etm.processor.elastic.PersistenceEnvironmentElasticImpl;
import com.jecstar.etm.processor.ibmmq.IbmMqProcessor;
import com.jecstar.etm.processor.ibmmq.configuration.IbmMq;
import com.jecstar.etm.processor.jms.JmsProcessor;
import com.jecstar.etm.processor.jms.JmsProcessorImpl;
import com.jecstar.etm.processor.kafka.KafkaProcessor;
import com.jecstar.etm.processor.kafka.KafkaProcessorImpl;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.persisting.internal.BusinessEventLogger;
import com.jecstar.etm.server.core.persisting.internal.InternalBulkProcessorWrapper;
import com.jecstar.etm.server.core.util.NamedThreadFactory;
import com.jecstar.etm.signaler.Signaler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.elasticsearch.client.Client;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class LaunchEtmCommand extends AbstractCommand {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(LaunchEtmCommand.class);

    private ElasticsearchIndexTemplateCreator indexTemplateCreator;
    private TelemetryCommandProcessor processor;
    private HttpServer httpServer;
    private Client elasticClient;
    private ScheduledReporter metricReporter;
    private IbmMqProcessor ibmMqProcessor;
    private JmsProcessor jmsProcessor;
    private KafkaProcessor kafkaProcessor;
    private ScheduledExecutorService backgroundScheduler;
    private InternalBulkProcessorWrapper bulkProcessorWrapper;

    public void launch(CommandLineParameters commandLineParameters, Configuration configuration, InternalBulkProcessorWrapper bulkProcessorWrapper) {
        this.bulkProcessorWrapper = bulkProcessorWrapper;
        addShutdownHooks(configuration);
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
        try {
            if (this.elasticClient == null) {
                this.elasticClient = createElasticsearchClient(configuration);
            }
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
                if (commandLineParameters.isReinitialize()) {
                    // When the --reinitialize option is passed to the command line, we stop processing over here because reinitializing is done.
                    return;
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
            if (configuration.kafka.enabled) {
                initializeKafkaProcessor(metricRegistry, configuration);
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
            try {
                if (log.isInfoLevelEnabled()) {
                    log.logInfoMessage("Shutting down Enterprise Telemetry Monitor.");
                }
            } catch (Throwable t) {
            }
            if (LaunchEtmCommand.this.indexTemplateCreator != null) {
                try {
                    LaunchEtmCommand.this.indexTemplateCreator.removeConfigurationChangeNotificationListener();
                } catch (Throwable t) {
                }
            }
            if (LaunchEtmCommand.this.backgroundScheduler != null) {
                try {
                    LaunchEtmCommand.this.backgroundScheduler.shutdownNow();
                } catch (Throwable t) {
                }
            }
            if (LaunchEtmCommand.this.kafkaProcessor != null) {
                try {
                    LaunchEtmCommand.this.kafkaProcessor.stop();
                } catch (Throwable t) {
                }
            }
            if (LaunchEtmCommand.this.jmsProcessor != null) {
                try {
                    LaunchEtmCommand.this.jmsProcessor.stop();
                } catch (Throwable t) {
                }
            }
            if (LaunchEtmCommand.this.ibmMqProcessor != null) {
                try {
                    LaunchEtmCommand.this.ibmMqProcessor.stop();
                } catch (Throwable t) {
                }
            }
            if (LaunchEtmCommand.this.httpServer != null) {
                try {
                    LaunchEtmCommand.this.httpServer.stop();
                } catch (Throwable t) {
                }
            }
            if (LaunchEtmCommand.this.processor != null) {
                try {
                    LaunchEtmCommand.this.processor.stopAll();
                } catch (Throwable t) {
                }
            }
            if (LaunchEtmCommand.this.metricReporter != null) {
                try {
                    LaunchEtmCommand.this.metricReporter.close();
                } catch (Throwable t) {
                }
            }
            if (LaunchEtmCommand.this.bulkProcessorWrapper != null) {
                try {
                    BusinessEventLogger.logEtmShutdown();
                    LaunchEtmCommand.this.bulkProcessorWrapper.close();
                } catch (Throwable t) {
                }
            }
            if (LaunchEtmCommand.this.elasticClient != null) {
                try {
                    LaunchEtmCommand.this.elasticClient.close();
                } catch (Throwable t) {
                }
            }
        }));
    }


    private void initializeBackgroundProcesses(final Configuration configuration, final EtmConfiguration etmConfiguration, final Client client) {
        int threadPoolSize = 2;
        if (configuration.http.guiEnabled || configuration.http.restProcessorEnabled) {
            threadPoolSize += 2;
        }
        if (configuration.signaler.enabled) {
            threadPoolSize++;
        }
        this.backgroundScheduler = new ScheduledThreadPoolExecutor(threadPoolSize, new NamedThreadFactory("etm_background_scheduler"));
        this.backgroundScheduler.scheduleAtFixedRate(new LicenseUpdater(etmConfiguration, client), 0, 6, TimeUnit.HOURS);
        this.backgroundScheduler.scheduleAtFixedRate(new IndexCleaner(etmConfiguration, client), 1, 15, TimeUnit.MINUTES);
        if (configuration.http.guiEnabled || configuration.http.restProcessorEnabled) {
            this.backgroundScheduler.scheduleAtFixedRate(new LdapSynchronizer(etmConfiguration, client), 1, 3, TimeUnit.HOURS);
            this.backgroundScheduler.scheduleAtFixedRate(new HttpSessionCleaner(etmConfiguration, client), 2, 15, TimeUnit.MINUTES);
        }
        if (configuration.signaler.enabled) {
            this.backgroundScheduler.scheduleAtFixedRate(new Signaler(configuration.clusterName, etmConfiguration, client), 1, 1, TimeUnit.MINUTES);
        }
    }

    private void initializeProcessor(MetricRegistry metricRegistry, Configuration configuration, EtmConfiguration etmConfiguration) {
        if (!configuration.ibmMq.enabled && !configuration.jms.enabled && !configuration.kafka.enabled && !configuration.http.restProcessorEnabled) {
            return;
        }
        if (this.processor == null) {
            this.processor = new TelemetryCommandProcessorImpl(metricRegistry);
            this.processor.start(new NamedThreadFactory("etm_processor"), new PersistenceEnvironmentElasticImpl(etmConfiguration, this.elasticClient), etmConfiguration);
        }
    }

    /**
     * Execute all necessary Elasticsearch data migrations.
     *
     * @param client The Elasticsearch client.
     * @return <code>true</code> when the index templates need to be reinitialized, <code>false</code> otherwise.
     */
    private boolean executeDatabaseMigrations(Client client) {
        boolean reinitialze = false;
        EtmMigrator etmMigrator = new Version2xTo3xMigrator(client);
        if (etmMigrator.shouldBeExecuted()) {
            etmMigrator.migrate();
            reinitialze = true;
        }
        etmMigrator = new Version300To301Migrator(client);
        if (etmMigrator.shouldBeExecuted()) {
            etmMigrator.migrate();
            reinitialze = true;
        }
        etmMigrator = new EndpointHandlerToSingleListMigrator(client);
        if (etmMigrator.shouldBeExecuted()) {
            etmMigrator.migrate();
            reinitialze = true;
        }
        etmMigrator = new SearchTemplateHandlingTimeMigrator(client);
        if (etmMigrator.shouldBeExecuted()) {
            etmMigrator.migrate();
        }
        return reinitialze;
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

    private void initializeKafkaProcessor(MetricRegistry metricRegistry, Configuration configuration) {
        this.kafkaProcessor = new KafkaProcessorImpl(this.processor, metricRegistry, configuration.kafka);
        this.kafkaProcessor.start();
    }

}
