package com.jecstar.etm.server.core.enhancers;

import com.jecstar.etm.domain.TelemetryEvent;

import java.time.ZonedDateTime;

/**
 * Interface for all enhancers. The purpose of an enhancer is to enrich the
 * <code>TelemetryEvent</code> with data that isn't provided by the application
 * that offered the <code>TelemetryEvent</code>.
 *
 * @author Mark Holster
 */
public interface TelemetryEventEnhancer {

    /**
     * Enhance the <code>TelemetryEvent</code>.
     *
     * @param event       The <code>TelemetryEvent</code> that may be enhanced.
     * @param enhanceTime The time the event is offered for enhancement. Useful if the
     *                    same system time is needed in several
     *                    <code>TelemetryEvent</code> properties.
     */
    void enhance(final TelemetryEvent<?> event, final ZonedDateTime enhanceTime);
}
