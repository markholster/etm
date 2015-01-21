package com.holster.etm.processor.processor;

import java.util.Map;

import com.holster.etm.processor.TelemetryEvent;
import com.holster.etm.processor.repository.CorrelationBySourceIdResult;
import com.lmax.disruptor.ExceptionHandler;

public class TelemetryEventExceptionHandler implements ExceptionHandler {

	private final Map<String, CorrelationBySourceIdResult> sourceCorrelations;

	public TelemetryEventExceptionHandler(Map<String, CorrelationBySourceIdResult> sourceCorrelations) {
		this.sourceCorrelations = sourceCorrelations;
    }

	@Override
    public void handleEventException(Throwable ex, long sequence, Object event) {
		if (event instanceof TelemetryEvent) {
			TelemetryEvent telemetryEvent = (TelemetryEvent) event;
			this.sourceCorrelations.remove(telemetryEvent.sourceId);
		}
		// TODO Log exceptions
		ex.printStackTrace();
    }

	@Override
    public void handleOnStartException(Throwable ex) {
		// TODO log exceptions and fail?
    }

	@Override
    public void handleOnShutdownException(Throwable ex) {
	    // TODO Log exceptions
	    
    }

}
