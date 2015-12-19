package com.jecstar.etm.processor.processor;

import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.processor.TelemetryCommand;
import com.lmax.disruptor.ExceptionHandler;

public class TelemetryCommandExceptionHandler implements ExceptionHandler<TelemetryCommand> {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(TelemetryCommandExceptionHandler.class);

	@Override
	public void handleEventException(Throwable t, long sequence, TelemetryCommand command) {
		switch (command.commandType) {
		case SQL_EVENT:
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Unable to process sql event '" + command.sqlTelemetryEvent.id + "'.", t);
			}
			break;
		case HTTP_EVENT:
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Unable to process http event '" + command.httpTelemetryEvent.id + "'.", t);
			}
			break;
		case LOG_EVENT:
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Unable to process log event '" + command.logTelemetryEvent.id + "'.", t);
			}
			break;
		case MESSAGING_EVENT:
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Unable to process log event '" + command.messagingTelemetryEvent.id + "'.", t);
			}
			break;
		default:
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Unable to process event '" + command.commandType.name() + "'.", t);
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
