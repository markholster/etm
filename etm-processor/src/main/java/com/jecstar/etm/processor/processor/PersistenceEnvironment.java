package com.jecstar.etm.processor.processor;

import com.jecstar.etm.processor.repository.TelemetryEventRepository;

public interface PersistenceEnvironment {

	TelemetryEventRepository createTelemetryEventRepository();
	
	void close();
	
}
