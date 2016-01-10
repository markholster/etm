package com.jecstar.etm.core.enhancers;

import java.time.ZonedDateTime;

import com.jecstar.etm.core.domain.TelemetryEvent;

/**
 * Interface for all enhancers. The purpose of an enhancer is to enricht the
 * <code>TelemetryEvent</code> with data that isn't provided by the application
 * that offered the <code>TelemetryEvent</code>.
 * 
 * @author Mark Holster
 */
public interface TelemetryEventEnhancer<T extends TelemetryEvent<T>> {

	/**
	 * Enhance the <code>TelemetryEvent</code>.
	 * 
	 * @param event
	 *            The <code>TelemetryEvent</code> that may be enhanced.
	 * @param enhanceTime
	 *            The time the event is offered for enhancement. Useful if the
	 *            same system time is needed in several
	 *            <code>TelemetryEvent</code> properties.
	 */
	void enhance(final T event, final ZonedDateTime enhanceTime);
}
