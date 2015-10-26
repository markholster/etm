package com.jecstar.etm.processor.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class RestTelemetryEventProcessorApplication extends Application {

	@Override
    public Set<Class<?>> getClasses()
    {
       HashSet<Class<?>> classes = new HashSet<Class<?>>();
       classes.add(RestTelemetryEventProcessor.class);
       return classes;
    }
}
