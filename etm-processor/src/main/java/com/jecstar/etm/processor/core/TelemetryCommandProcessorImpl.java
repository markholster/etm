package com.jecstar.etm.processor.core;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.domain.*;
import com.jecstar.etm.domain.builder.*;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.metrics.GarbageCollectorMetricSet;
import com.jecstar.etm.processor.metrics.MemoryUsageMetricSet;
import com.jecstar.etm.processor.metrics.NetworkMetricSet;
import com.jecstar.etm.processor.metrics.OperatingSystemMetricSet;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.configuration.ConfigurationChangedEvent;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.persisting.internal.BusinessEventLogger;
import com.lmax.disruptor.RingBuffer;

import java.io.IOException;
import java.util.concurrent.ThreadFactory;

public class TelemetryCommandProcessorImpl implements TelemetryCommandProcessor {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(TelemetryCommandProcessorImpl.class);

    private RingBuffer<TelemetryCommand> ringBuffer;
    private boolean started = false;

    private ThreadFactory threadFactory;
    private EtmConfiguration etmConfiguration;
    private DisruptorEnvironment disruptorEnvironment;
    private PersistenceEnvironment persistenceEnvironment;
    private final MetricRegistry metricRegistry;
    private Timer offerTimer;

    private boolean licenseExpiredLogged = false;
    private boolean licenseNotYetValidLogged = false;
    private boolean licenseCountExceededLogged = false;
    private boolean licenseSizeExceededLogged = false;

    public TelemetryCommandProcessorImpl(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void start(final ThreadFactory threadFactory, final PersistenceEnvironment persistenceEnvironment, final EtmConfiguration etmConfiguration) {
        if (this.started) {
            throw new IllegalStateException();
        }
        this.started = true;
        this.threadFactory = threadFactory;
        this.persistenceEnvironment = persistenceEnvironment;
        this.etmConfiguration = etmConfiguration;
        this.etmConfiguration.addConfigurationChangeListener(this);
        this.offerTimer = this.metricRegistry.timer("event-processor.offering");
        this.disruptorEnvironment = new DisruptorEnvironment(etmConfiguration, this.threadFactory, this.persistenceEnvironment, this.metricRegistry);
        this.ringBuffer = this.disruptorEnvironment.start();
        this.metricRegistry.register("event-processor.ringbuffer-capacity", (Gauge<Long>) () -> TelemetryCommandProcessorImpl.this.ringBuffer.remainingCapacity());
        this.metricRegistry.registerAll(new GarbageCollectorMetricSet());
        this.metricRegistry.registerAll(new MemoryUsageMetricSet());
        this.metricRegistry.registerAll(new OperatingSystemMetricSet());
        if (NetworkMetricSet.isCapableOfMonitoring()) {
            this.metricRegistry.registerAll(new NetworkMetricSet());
        }
    }

    private void hotRestart() {
        if (!this.started) {
            throw new IllegalStateException();
        }
        if (log.isInfoLevelEnabled()) {
            log.logInfoMessage("Executing hot restart of TelemetryCommandProcessor.");
        }
        DisruptorEnvironment newDisruptorEnvironment = new DisruptorEnvironment(this.etmConfiguration, this.threadFactory, this.persistenceEnvironment, this.metricRegistry);
        RingBuffer<TelemetryCommand> newRingBuffer = newDisruptorEnvironment.start();
        DisruptorEnvironment oldDisruptorEnvironment = this.disruptorEnvironment;

        this.ringBuffer = newRingBuffer;
        this.disruptorEnvironment = newDisruptorEnvironment;
        oldDisruptorEnvironment.shutdown();
    }

    @Override
    public void stop() {
        if (!this.started) {
            throw new IllegalStateException();
        }
        this.disruptorEnvironment.shutdown();
    }

    @Override
    public void stopAll() {
        if (!this.started) {
            throw new IllegalStateException();
        }
        this.etmConfiguration.removeConfigurationChangeListener(this);
        this.disruptorEnvironment.shutdown();
        try {
            this.persistenceEnvironment.close();
        } catch (IOException e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Failed to close PersistenceEnvironment", e);
            }
        }
    }

    @Override
    public void processTelemetryEvent(final SqlTelemetryEventBuilder builder, String importProfile) {
        processTelemetryEvent(builder.build(), importProfile);
    }

    @Override
    public void processTelemetryEvent(final SqlTelemetryEvent event, String importProfile) {
        preProcess();
        final Context timerContext = this.offerTimer.time();
        TelemetryCommand target = null;
        long sequence = this.ringBuffer.next();
        try {
            target = this.ringBuffer.get(sequence);
            target.initialize(event, importProfile);
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("Processing sql event with id '" + event.id + "'.");
            }
        } catch (Exception e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Failed to initialize sql event with id '" + event.id + "'.", e);
            }
            if (target != null) {
                target.initializeToNoop();
            }
        } finally {
            this.ringBuffer.publish(sequence);
            timerContext.stop();
        }
    }

    @Override
    public void processTelemetryEvent(final HttpTelemetryEventBuilder builder, String importProfile) {
        processTelemetryEvent(builder.build(), importProfile);
    }

    @Override
    public void processTelemetryEvent(final HttpTelemetryEvent event, String importProfile) {
        preProcess();
        final Context timerContext = this.offerTimer.time();
        TelemetryCommand target = null;
        long sequence = this.ringBuffer.next();
        try {
            target = this.ringBuffer.get(sequence);
            target.initialize(event, importProfile);
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("Processing http event with id '" + event.id + "'.");
            }
        } catch (Exception e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Failed to initialize http event with id '" + event.id + "'.", e);
            }
            if (target != null) {
                target.initializeToNoop();
            }
        } finally {
            this.ringBuffer.publish(sequence);
            timerContext.stop();
        }
    }

    @Override
    public void processTelemetryEvent(final LogTelemetryEventBuilder builder, String importProfile) {
        processTelemetryEvent(builder.build(), importProfile);
    }

    @Override
    public void processTelemetryEvent(final LogTelemetryEvent event, String importProfile) {
        preProcess();
        final Context timerContext = this.offerTimer.time();
        TelemetryCommand target = null;
        long sequence = this.ringBuffer.next();
        try {
            target = this.ringBuffer.get(sequence);
            target.initialize(event, importProfile);
        } catch (Exception e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Failed to initialize log event with id '" + event.id + "'.", e);
            }
            if (target != null) {
                target.initializeToNoop();
            }
        } finally {
            this.ringBuffer.publish(sequence);
            timerContext.stop();
        }
    }

    @Override
    public void processTelemetryEvent(final MessagingTelemetryEventBuilder builder, String importProfile) {
        processTelemetryEvent(builder.build(), importProfile);
    }

    @Override
    public void processTelemetryEvent(final MessagingTelemetryEvent event, String importProfile) {
        preProcess();
        final Context timerContext = this.offerTimer.time();
        TelemetryCommand target = null;
        long sequence = this.ringBuffer.next();
        try {
            target = this.ringBuffer.get(sequence);
            target.initialize(event, importProfile);
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("Processing messaging event with id '" + event.id + "'.");
            }
        } catch (Exception e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Failed to initialize messaging event with id '" + event.id + "'.", e);
            }
            if (target != null) {
                target.initializeToNoop();
            }
        } finally {
            this.ringBuffer.publish(sequence);
            timerContext.stop();
        }
    }

    @Override
    public void processTelemetryEvent(final BusinessTelemetryEventBuilder builder, String importProfile) {
        processTelemetryEvent(builder.build(), importProfile);
    }

    @Override
    public void processTelemetryEvent(final BusinessTelemetryEvent event, String importProfile) {
        preProcess();
        final Context timerContext = this.offerTimer.time();
        TelemetryCommand target = null;
        long sequence = this.ringBuffer.next();
        try {
            target = this.ringBuffer.get(sequence);
            target.initialize(event, importProfile);
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("Processing business event with id '" + event.id + "'.");
            }
        } catch (Exception e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Failed to initialize business event with id '" + event.id + "'.", e);
            }
            if (target != null) {
                target.initializeToNoop();
            }
        } finally {
            this.ringBuffer.publish(sequence);
            timerContext.stop();
        }
    }

    @Override
    public MetricRegistry getMetricRegistry() {
        return this.metricRegistry;
    }

    @Override
    public boolean isReadyForProcessing() {
        if (!this.started) {
            return false;
        }
        return this.etmConfiguration.isLicenseValid();
    }

    private void preProcess() {
        if (!this.started) {
            throw new IllegalStateException();
        }
        if (this.etmConfiguration.isLicenseExpired()) {
            if (!this.licenseExpiredLogged) {
                BusinessEventLogger.logLicenseExpired();
                this.licenseExpiredLogged = true;
            }
            throw new EtmException(EtmException.LICENSE_EXPIRED);
        } else {
            this.licenseExpiredLogged = false;
        }
        if (!this.etmConfiguration.isLicenseValid()) {
            if (!this.licenseNotYetValidLogged) {
                BusinessEventLogger.logLicenseNotYetValid();
                this.licenseNotYetValidLogged = true;
            }
            throw new EtmException(EtmException.LICENSE_NOT_YET_VALID);
        } else {
            this.licenseNotYetValidLogged = false;
        }
        if (this.etmConfiguration.isLicenseCountExceeded()) {
            if (!this.licenseCountExceededLogged) {
                BusinessEventLogger.logLicenseCountExceeded();
                this.licenseCountExceededLogged = true;
            }
            throw new EtmException(EtmException.LICENSE_MESSAGE_COUNT_EXCEEDED);
        } else {
            this.licenseCountExceededLogged = false;
        }
        if (this.etmConfiguration.isLicenseSizeExceeded()) {
            if (!this.licenseSizeExceededLogged) {
                BusinessEventLogger.logLicenseSizeExceeded();
                this.licenseSizeExceededLogged = true;
            }
            throw new EtmException(EtmException.LICENSE_MESSAGE_SIZE_EXCEEDED);
        } else {
            this.licenseSizeExceededLogged = false;
        }
    }

    @Override
    public long getCurrentCapacity() {
        if (!this.started) {
            throw new IllegalStateException();
        }
        return this.ringBuffer.remainingCapacity();
    }

    @Override
    public void configurationChanged(ConfigurationChangedEvent event) {
        if (this.started && event.isAnyChanged(
                EtmConfiguration.CONFIG_KEY_ENHANCING_HANDLER_COUNT,
                EtmConfiguration.CONFIG_KEY_PERSISTING_HANDLER_COUNT,
                EtmConfiguration.CONFIG_KEY_EVENT_BUFFER_SIZE,
                EtmConfiguration.CONFIG_KEY_WAIT_STRATEGY)) {
            if (log.isInfoLevelEnabled()) {
                log.logInfoMessage("Detected a change in the configuration that needs a restart of the command processor.");
            }
            try {
                hotRestart();
            } catch (IllegalStateException e) {
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage("Failed to restart the command processor. Your processor is in an unknow state. Please restart the processor node.", e);
                }
            }
        }
    }
}