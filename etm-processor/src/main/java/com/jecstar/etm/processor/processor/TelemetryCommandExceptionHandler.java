package com.jecstar.etm.processor.processor;

import com.jecstar.etm.core.TelemetryCommand;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.lmax.disruptor.ExceptionHandler;

public class TelemetryCommandExceptionHandler implements ExceptionHandler<TelemetryCommand> {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(TelemetryCommandExceptionHandler.class);

	private final SourceCorrelationCache sourceCorrelations;

	public TelemetryCommandExceptionHandler(SourceCorrelationCache sourceCorrelations) {
		this.sourceCorrelations = sourceCorrelations;
	}

	@Override
	public void handleEventException(Throwable t, long sequence, TelemetryCommand command) {
		switch (command.commandType) {
		case MESSAGE_EVENT:
			this.sourceCorrelations.removeTelemetryEvent(command.messageEvent);
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Unable to process event '" + command.messageEvent.id + "'.", t);
			}
			break;
		default:
			break;
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
