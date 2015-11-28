package com.jecstar.etm.processor.processor;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;

public interface PersistenceEnvironment {

	CommandResources createCommandResources(MetricRegistry metricRegistry);
	
	ScheduledReporter createMetricReporter(String nodeName, MetricRegistry metricRegistry);
	
	void createEnvironment();
	
	void close();

	
}
