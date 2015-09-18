package com.jecstar.etm.core.domain.converter;

import com.jecstar.etm.core.domain.TelemetryEvent;

public interface TelemetryEventConverter<T> {

	T convert(TelemetryEvent telemetryEvent, TelemetryEventTags telemetryEventTags);
}
