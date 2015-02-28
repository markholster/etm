package com.jecstar.etm.processor;

import com.lmax.disruptor.EventFactory;

public class TelemetryEventFactory implements EventFactory<TelemetryEvent>{
	
	@Override
	public TelemetryEvent newInstance() {
		return new TelemetryEvent();
	}

}
