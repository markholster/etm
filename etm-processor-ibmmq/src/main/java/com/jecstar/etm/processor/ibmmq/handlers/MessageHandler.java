package com.jecstar.etm.processor.ibmmq.handlers;

import com.jecstar.etm.core.TelemetryEvent;

public interface MessageHandler<T> {

	public boolean handleMessage(TelemetryEvent telemetryEvent, T message);
}
