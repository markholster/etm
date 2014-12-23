package com.holster.etm;

import com.lmax.disruptor.ExceptionHandler;

public class TelemetryEventExceptionHandler implements ExceptionHandler {

	@Override
    public void handleEventException(Throwable ex, long sequence, Object event) {
		if (ex instanceof EtmException && event instanceof TelemetryEvent) {
		}
		ex.printStackTrace();
		//TODO extra logging for non-retry situations.
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
