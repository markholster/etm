package com.jecstar.etm.processor.core;

import java.io.Closeable;

import com.codahale.metrics.MetricRegistry;

public interface PersistenceEnvironment extends Closeable {

	CommandResources getCommandResources(MetricRegistry metricRegistry);
	
}
