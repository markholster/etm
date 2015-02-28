package com.jecstar.etm.processor.processor;

import java.util.Map;

import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.processor.TelemetryEvent;
import com.jecstar.etm.processor.repository.CorrelationBySourceIdResult;
import com.lmax.disruptor.ExceptionHandler;

public class TelemetryEventExceptionHandler implements ExceptionHandler {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(TelemetryEventExceptionHandler.class);

	private final Map<String, CorrelationBySourceIdResult> sourceCorrelations;

	public TelemetryEventExceptionHandler(Map<String, CorrelationBySourceIdResult> sourceCorrelations) {
		this.sourceCorrelations = sourceCorrelations;
	}

	@Override
	public void handleEventException(Throwable t, long sequence, Object event) {
		if (event instanceof TelemetryEvent) {
			TelemetryEvent telemetryEvent = (TelemetryEvent) event;
			this.sourceCorrelations.remove(telemetryEvent.sourceId);
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Unable to process event '" + telemetryEvent.id + "'.", t);
			}
		} else {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Unable to process event " + event.getClass().getName(), t);
			}
		}
	}

	@Override
	public void handleOnStartException(Throwable t) {
		if (log.isFatalLevelEnabled()) {
			log.logFatalMessage("Unable to start disruptor. No events will be processed", t);
		}
	}

	@Override
	public void handleOnShutdownException(Throwable t) {
		if (log.isWarningLevelEnabled()) {
			log.logWarningMessage("Unable to stop disruptor.", t);
		}
	}

}
