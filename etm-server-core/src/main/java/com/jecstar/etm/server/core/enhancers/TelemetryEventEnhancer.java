/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

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
public interface TelemetryEventEnhancer extends AutoCloseable {

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
