package com.jecstar.etm.processor.processor;

import java.util.Map;

import com.jecstar.etm.processor.repository.CorrelationBySourceIdResult;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;

public interface PersistenceEnvironment {

	TelemetryEventRepository createTelemetryEventRepository();
	
	Map<String, CorrelationBySourceIdResult> getProcessingMap();

	void close();
	
}
