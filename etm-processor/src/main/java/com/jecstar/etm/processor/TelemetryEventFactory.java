package com.jecstar.etm.processor;

import com.lmax.disruptor.EventFactory;

public class TelemetryEventFactory implements EventFactory<TelemetryCommand>{
	
	@Override
	public TelemetryCommand newInstance() {
		return new TelemetryCommand();
	}

}
