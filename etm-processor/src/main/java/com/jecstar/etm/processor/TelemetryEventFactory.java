package com.jecstar.etm.processor;

import com.jecstar.etm.core.TelemetryEvent;
import com.lmax.disruptor.EventFactory;

public class TelemetryEventFactory implements EventFactory<TelemetryEvent>{
	
	@Override
	public TelemetryEvent newInstance() {
		return new TelemetryEvent();
	}

}
