package com.jecstar.etm.processor.ibmmq;

import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;

public class IbmMqProcessor {

	private TelemetryCommandProcessor processor;

	public IbmMqProcessor(TelemetryCommandProcessor processor, IbmMqProcessorConfiguration config) {
		this.processor = processor;
	}

	public void stop() {
		// TODO Auto-generated method stub
	}
}
