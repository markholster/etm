package com.jecstar.etm.processor.core;

import com.codahale.metrics.MetricRegistry;

import java.io.Closeable;

public interface PersistenceEnvironment extends Closeable {

	CommandResources getCommandResources(MetricRegistry metricRegistry);
	
}
