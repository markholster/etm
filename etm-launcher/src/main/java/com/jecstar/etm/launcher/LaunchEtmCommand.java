/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.launcher;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.jecstar.etm.launcher.background.*;
import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.launcher.http.ElasticsearchIdentityManager;
import com.jecstar.etm.launcher.http.HttpServer;
import com.jecstar.etm.launcher.migrations.EtmMigrator;
import com.jecstar.etm.launcher.migrations.v4.Version41Migrator;
import com.jecstar.etm.launcher.migrations.v4.Version42Migrator;
import com.jecstar.etm.launcher.migrations.v4.Version4Migrator;
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
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.persisting.internal.BusinessEventLogger;
import com.jecstar.etm.server.core.persisting.internal.InternalBulkProcessorWrapper;
import com.jecstar.etm.server.core.util.NamedThreadFactory;
import com.jecstar.etm.signaler.Signaler;

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
    private DataRepository dataRepository;
    private ScheduledReporter metricReporter;
    private IbmMqProcessor ibmMqProcessor;
    private JmsProcessor jmsProcessor;
    private KafkaProcessor kafkaProcessor;
    private ScheduledExecutorService backgroundScheduler;
    private InternalBulkProcessorWrapper bulkProcessorWrapper;

    public void launch(CommandLineParameters commandLineParameters, Configuration configuration, InternalBulkProcessorWrapper bulkProcessorWrapper) {
        this.bulkProcessorWrapper = bulkProcessorWrapper;
        addShutdownHooks();
        try {
            if (this.dataRepository == null) {
                this.dataRepository = createElasticsearchClient(configuration);
            }
            this.bulkProcessorWrapper.setClient(this.dataRepository.getClient());
            boolean reinitializeTemplates = executeDatabaseMigrations(this.dataRepository);
            this.indexTemplateCreator = new ElasticsearchIndexTemplateCreator(this.dataRepository);
            this.indexTemplateCreator.createTemplates();
            EtmConfiguration etmConfiguration = new ElasticBackedEtmConfiguration(configuration.instanceName, this.dataRepository);
            this.bulkProcessorWrapper.setConfiguration(etmConfiguration);
            this.dataRepository.setLicenseRateLimiter(etmConfiguration.getLicenseRateLimiter());
            this.indexTemplateCreator.addConfigurationChangeNotificationListener(etmConfiguration);
            if (commandLineParameters.isReinitialize() || reinitializeTemplates) {
                this.indexTemplateCreator.reinitialize();
                if (!commandLineParameters.isQuiet()) {
                    System.out.println("Reinitialized system.");
                }
                if (commandLineParameters.isReinitialize()) {
                    // When the --reinitialize option is passed to the command line, we stop processing over here because reinitializing is done.
                    this.bulkProcessorWrapper.close();
                    this.dataRepository.close();
                    return;
                }
            }
            MetricRegistry metricRegistry = new MetricRegistry();
            initializeMetricReporter(metricRegistry, configuration);
            initializeProcessor(metricRegistry, configuration, etmConfiguration);
            initializeBackgroundProcesses(configuration, etmConfiguration, this.dataRepository);

            if (configuration.isHttpServerNecessary()) {
                System.setProperty("org.jboss.logging.provider", "slf4j");
                this.httpServer = new HttpServer(new ElasticsearchIdentityManager(this.dataRepository, etmConfiguration), configuration, etmConfiguration, this.processor, this.dataRepository);
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

    private void addShutdownHooks() {
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
            if (LaunchEtmCommand.this.dataRepository != null) {
                try {
                    LaunchEtmCommand.this.dataRepository.close();
                } catch (Throwable t) {
                }
            }
        }));
    }


    private void initializeBackgroundProcesses(final Configuration configuration, final EtmConfiguration etmConfiguration, final DataRepository dataRepository) {
        int threadPoolSize = 3;
        if (configuration.http.guiEnabled || configuration.http.restProcessorEnabled) {
            threadPoolSize += 2;
        }
        if (configuration.signaler.enabled) {
            threadPoolSize++;
        }
        this.backgroundScheduler = new ScheduledThreadPoolExecutor(threadPoolSize, new NamedThreadFactory("etm_background_scheduler"));
        this.backgroundScheduler.scheduleAtFixedRate(new InstanceBroadcaster(etmConfiguration, dataRepository, String.valueOf(configuration.calculateInstanceHash())), 0, 1, TimeUnit.MINUTES);
        this.backgroundScheduler.scheduleAtFixedRate(new LicenseUpdater(etmConfiguration, dataRepository, configuration.licenseUpdateUrl), 0, 6, TimeUnit.HOURS);
        this.backgroundScheduler.scheduleAtFixedRate(new IndexCleaner(etmConfiguration, dataRepository), 1, 15, TimeUnit.MINUTES);
        if (configuration.http.guiEnabled || configuration.http.restProcessorEnabled) {
            this.backgroundScheduler.scheduleAtFixedRate(new LdapSynchronizer(etmConfiguration, dataRepository), 1, 3, TimeUnit.HOURS);
            this.backgroundScheduler.scheduleAtFixedRate(new HttpSessionCleaner(etmConfiguration, dataRepository), 2, 15, TimeUnit.MINUTES);
        }
        if (configuration.signaler.enabled) {
            this.backgroundScheduler.scheduleAtFixedRate(new Signaler(configuration.clusterName, etmConfiguration, dataRepository), 1, 1, TimeUnit.MINUTES);
        }
    }

    private void initializeProcessor(MetricRegistry metricRegistry, Configuration configuration, EtmConfiguration etmConfiguration) {
        if (!configuration.ibmMq.enabled && !configuration.jms.enabled && !configuration.kafka.enabled && !configuration.http.restProcessorEnabled) {
            return;
        }
        if (this.processor == null) {
            this.processor = new TelemetryCommandProcessorImpl(metricRegistry);
            this.processor.start(new NamedThreadFactory("etm_processor"), new PersistenceEnvironmentElasticImpl(etmConfiguration, this.dataRepository), etmConfiguration);
        }
    }

    /**
     * Execute all necessary Elasticsearch data migrations.
     *
     * @param dataRepository The <code>DataRepository</code>.
     * @return <code>true</code> when the index templates need to be reinitialized, <code>false</code> otherwise.
     */
    private boolean executeDatabaseMigrations(DataRepository dataRepository) {
        boolean reinitialze = false;
        EtmMigrator etmMigrator = new Version4Migrator(dataRepository);
        if (etmMigrator.shouldBeExecuted()) {
            etmMigrator.migrate();
        }
        etmMigrator = new Version41Migrator(dataRepository);
        if (etmMigrator.shouldBeExecuted()) {
            etmMigrator.migrate();
        }
        etmMigrator = new Version42Migrator(dataRepository);
        if (etmMigrator.shouldBeExecuted()) {
            etmMigrator.migrate();
            reinitialze = true;
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
        this.metricReporter = new MetricReporterElasticImpl(metricRegistry, configuration.instanceName, this.dataRepository);
        this.metricReporter.start(1, TimeUnit.MINUTES);
    }

    private void initializeKafkaProcessor(MetricRegistry metricRegistry, Configuration configuration) {
        this.kafkaProcessor = new KafkaProcessorImpl(this.processor, metricRegistry, configuration.kafka);
        this.kafkaProcessor.start();
    }

}
