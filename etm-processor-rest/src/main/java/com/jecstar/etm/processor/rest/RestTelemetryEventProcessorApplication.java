package com.jecstar.etm.processor.rest;

import com.jecstar.etm.processor.core.TelemetryCommandProcessor;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

public class RestTelemetryEventProcessorApplication extends Application {

    public RestTelemetryEventProcessorApplication(TelemetryCommandProcessor processor) {
        RestTelemetryEventProcessor.setProcessor(processor);
    }

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<>();
        classes.add(RestTelemetryEventProcessor.class);
        return classes;
    }
}
