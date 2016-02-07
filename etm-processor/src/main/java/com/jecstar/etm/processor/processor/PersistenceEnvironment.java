package com.jecstar.etm.processor.processor;

import java.io.Closeable;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;

public interface PersistenceEnvironment extends Closeable {

	CommandResources getCommandResources(MetricRegistry metricRegistry);
	
	ScheduledReporter createMetricReporter(String nodeName, MetricRegistry metricRegistry);
	
}
