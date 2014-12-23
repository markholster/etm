package com.holster.etm.processor;

import com.holster.etm.EtmException;
import com.holster.etm.TelemetryEvent;
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
