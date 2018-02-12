package com.jecstar.etm.launcher.slf4j;

import com.jecstar.etm.processor.internal.persisting.InternalBulkProcessorWrapper;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class EtmLoggerFactory implements ILoggerFactory {

    private final ConcurrentMap<String, Logger> loggerMap;
    private final LogConfiguration logConfiguration;

    private static InternalBulkProcessorWrapper bulkProcessorWrapper;

    public static void initialize(InternalBulkProcessorWrapper bulkProcessorWrapper) {
        EtmLoggerFactory.bulkProcessorWrapper = bulkProcessorWrapper;
    }

    public EtmLoggerFactory() {
        this.loggerMap = new ConcurrentHashMap<>();
        this.logConfiguration = new LogConfiguration();
    }

    @Override
    public Logger getLogger(String name) {
        Logger etmLogger = this.loggerMap.get(name);
        if (etmLogger != null) {
            return etmLogger;
        } else {
            Logger newInstance = new EtmLogger(name, this.logConfiguration, bulkProcessorWrapper);
            Logger oldInstance = loggerMap.putIfAbsent(name, newInstance);
            return oldInstance == null ? newInstance : oldInstance;
        }
    }

}
