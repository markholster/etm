package com.jecstar.etm.processor.processor;

import java.io.Closeable;

import com.codahale.metrics.MetricRegistry;

public interface PersistenceEnvironment extends Closeable {

	CommandResources getCommandResources(MetricRegistry metricRegistry);
	
}
