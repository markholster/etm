package com.jecstar.etm.processor.processor;

import com.jecstar.etm.processor.repository.TelemetryEventRepository;

public interface PersistenceEnvironment {

	TelemetryEventRepository createTelemetryEventRepository();
	
	IdCorrelationCache getProcessingMap();

	void close();

	void createEnvironment();
	
}
