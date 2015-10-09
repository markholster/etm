package com.jecstar.etm.processor.processor;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;

public interface PersistenceEnvironment {

	TelemetryEventRepository createTelemetryEventRepository(MetricRegistry metricRegistry);
	
	ScheduledReporter createMetricReporter(String nodeName, MetricRegistry metricRegistry);
	
	void createEnvironment();
	
	void close();

	
}
