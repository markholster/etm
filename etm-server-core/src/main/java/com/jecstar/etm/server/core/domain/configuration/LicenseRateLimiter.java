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

package com.jecstar.etm.server.core.domain.configuration;

import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * Class that is capable of throttling and shaping calls based on the <code>License</code>.
 * <p/>
 * Throttling is evaluated every second and hence allows for bursts within that second. Throttling should mainly
 * be used in background processes like the processors. The reason for that is the potential large sleep time because
 * of a burst.
 * When a processor handles 500 ru's in a single second with a license of 10 ru/s the thread will be blocked
 * for (500 - 10) / 10 = 49 seconds. This is not something you want to experience in an user interface.
 * The typical usage in a throttle situation would be as follow:
 * <pre> {@code
 * double processedRequestUnits = 20;
 * var rateLimiter = new LicenseRateLimiter(etmConfiguration);
 * // chain the adding of the request units to the rateLimiter with the throttle() method that calculates if throttling should be applied.
 * var throttleTime = rateLimiter.addRequestUnits(processedRequestUnits).throttle();
 * }</pre>
 * <p/>
 * Shaping is evaluated right away, and thus can only be applied after a method is executed. The time the method
 * must take according to the license is calculated, and the shaping (sleeping) is applied right away. No bursting is
 * allowed which means the sleep times are shorter compared to the throttle situations. This means that shaping could
 * be safely applied within an user interface. Shaping could be added with the following code:
 * <pre> {@code
 * var startTime = System.currentTimeMillis();
 * ... do some work that's equal to 5 request units.
 * var totalTime = System.currentTimeMillis() - startTime;
 * double processedRequestUnits = 5;
 * var rateLimiter = new LicenseRateLimiter(etmConfiguration);
 * var shapeTime = rateLimiter.addRequestUnitsAndShape(processedRequestUnits, totalTime);
 * }</pre>
 * <p/>
 * Note that the number of active ETM nodes is taken into account when calculating the available ru's for throttling or
 * shaping. For example, if a license allows for 10 ru/s and there are 2 active ETM nodes available the throttling will
 * calculate with &lt;License ru/s&gt;/&lt;Active node count&gt; is 5 ru/s.
 */
public class LicenseRateLimiter {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(LicenseRateLimiter.class);

    /**
     * The <code>EtmConfiguration</code> instance that holds the <code>License</code>.
     */
    private final EtmConfiguration etmConfiguration;

    /**
     * The number of processed request units.
     */
    private static final DoubleAdder processedRequestUnits = new DoubleAdder();

    /**
     * The number of milliseconds the threads have been throttled.
     */
    private static final LongAdder totalThrottleTime = new LongAdder();

    /**
     * The number of milliseconds the threads have been shaped.
     */
    private static final LongAdder totalShapeTime = new LongAdder();

    /**
     * A <code>ThreadLocal</code> that holds the check state per <code>Thread</code>.
     */
    private static final ThreadLocal<ThreadState> threadState = ThreadLocal.withInitial(ThreadState::new);

    /**
     * Constructs a new <code>LicenseThrottler</code> instance.
     *
     * @param etmConfiguration The <code>EtmConfiguration</code> that holds the license.
     */
    public LicenseRateLimiter(EtmConfiguration etmConfiguration) {
        this.etmConfiguration = etmConfiguration;
    }

    /**
     * Add a number of processed request units.
     * <p/>This method will <b>not</b> throttle or shape, but only adds the given request units to the amount of total
     * request units. You should call the {@link #throttle()} method to check if the thread needs to be blocked.
     * <p/>Note that throttling is only recommended for background processes where no direct user is involved.
     * Methods with user interaction should use the {@link #addRequestUnitsAndShape(double, long)} addRequestUnitsAndShape} method instead.
     *
     * @param requestUnits The number of processed request units.
     * @return The <code>LicenseThrottler</code> for chaining.
     */
    public LicenseRateLimiter addRequestUnits(double requestUnits) {
        processedRequestUnits.add(requestUnits);
        return this;
    }

    /**
     * Method that check if the max request units per second of the license is exceeded. If so, this method will
     * block the current thread until the license is honoured.
     *
     * @return The time in milliseconds this thread has throttled.
     */
    public long throttle() {
        long throttleTime = 0;
        final var licensedRequestUnitsPerSecond = getLicenseRequestUnitsPerSecond();
        if (licensedRequestUnitsPerSecond.equals(License.UNLIMITED)) {
            return throttleTime;
        }
        final var licensedRequestUnitsPerSecondPerNode = (double) licensedRequestUnitsPerSecond / getActiveNodeCount();
        final var state = threadState.get();
        if (state.lastChecked == 0) {
            // State never checked. Let's initialize and forget we ever reached this code.
            state.update(System.currentTimeMillis(), processedRequestUnits.longValue());
            return throttleTime;
        }
        final var requestUnits = processedRequestUnits.doubleValue();
        final var timeDiff = System.currentTimeMillis() - state.getLastChecked();
        if (timeDiff < 1000) {
            // Check per second
            return throttleTime;
        }
        final var requestUnitsProcessed = Math.abs(requestUnits - state.lastRequestUnits);
        final var requestUnitsPerSecond = requestUnitsProcessed / (timeDiff / 1000f);
        final var requestUnitsDiff = requestUnitsPerSecond - licensedRequestUnitsPerSecondPerNode;
        if (requestUnitsDiff > 0) {
            // License exceeded. Calculate how long the thread should sleep.
            throttleTime = (long) ((requestUnitsDiff / licensedRequestUnitsPerSecondPerNode) * 1000);
            if (log.isWarningLevelEnabled()) {
                log.logWarningMessage("Request Units threshold exceeded! Your license allows for "
                        + licensedRequestUnitsPerSecond + " ru/s (spread over " + getActiveNodeCount()
                        + " active ETM nodes), but ETM has processed "
                        + requestUnitsPerSecond + " ru/s. This thread (" + Thread.currentThread().getName()
                        + ") will be throttled for " + throttleTime + "ms.");
            }
            totalThrottleTime.add(throttleTime);
            try {
                Thread.sleep(throttleTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        state.update(System.currentTimeMillis(), processedRequestUnits.longValue());
        return throttleTime;
    }

    /**
     * Add an amount of request units to the rate limiter and shape the thread execution time when necessary. This means
     * that when a method took 250 ms to handle 10 ru's and there's a license of 10 ru/s this method will block for 750ms.
     * Keep in mind that this method will not honour the license in multi-threaded situations. If the previous example
     * is execute twice in parallel, both methods will block for 750ms in parallel. This means that both methods
     * are finished within 1 second, which is the equivalent of a license of 20 ru/s.
     *
     * @param requestUnits  The request units that were handled.
     * @param executionTime The time it took to execute the given request units in milliseconds.
     * @return The sleep time.
     */
    public long addRequestUnitsAndShape(double requestUnits, long executionTime) {
        addRequestUnits(requestUnits);
        long shapeTime = 0;
        final var licensedRequestUnitsPerSecond = getLicenseRequestUnitsPerSecond();
        if (licensedRequestUnitsPerSecond.equals(License.UNLIMITED)) {
            return shapeTime;
        }
        final var licensedRequestUnitsPerSecondPerNode = (double) licensedRequestUnitsPerSecond / getActiveNodeCount();
        final var expectedTime = (requestUnits / licensedRequestUnitsPerSecondPerNode) * 1000;
        final var timeDiff = expectedTime - executionTime;
        if (timeDiff > 0) {
            shapeTime = (long) timeDiff;
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("Request Units threshold exceeded! Your license allows for "
                        + licensedRequestUnitsPerSecond + " ru/s (spread over " + getActiveNodeCount()
                        + " active ETM nodes), but ETM has processed " + requestUnits + " in "
                        + executionTime + " seconds. This thread (" + Thread.currentThread().getName()
                        + ") will be shaped for " + shapeTime + "ms.");
            }
            totalShapeTime.add(shapeTime);
            try {
                Thread.sleep(shapeTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return shapeTime;
    }

    /**
     * Gives the licensed request units. When no license is available the unlicensed value will be returned.
     *
     * @return The ru/s rate that is configured in the license, or the default unlicensed value when no license can be found.
     * @see License#UNLICENSED_REQUEST_UNITS_PER_SECOND
     */
    private Long getLicenseRequestUnitsPerSecond() {
        if (this.etmConfiguration == null || this.etmConfiguration.getLicense() == null) {
            return License.UNLICENSED_REQUEST_UNITS_PER_SECOND;
        }
        return this.etmConfiguration.getLicense().getMaxRequestUnitsPerSecond();
    }

    /**
     * Gives the number of active ETM nodes.
     *
     * @return The number of active ETM nodes.
     */
    private int getActiveNodeCount() {
        if (this.etmConfiguration == null) {
            return 1;
        }
        return this.etmConfiguration.getActiveNodeCount();
    }

    /**
     * Get the number of processed request units.
     *
     * @return The number of processed request units.
     */
    public double getProcessedRequestUnits() {
        return processedRequestUnits.sum();
    }

    /**
     * Get the time in milliseconds that has been throttled (slept) during the start of ETM. This is the time of all
     * threads combined. So if two threads slept both for 5 seconds in parallel then the total throttle time will be 10 seconds.
     *
     * @return The time that all threads have slept during throttling.
     */
    public long getTotalThrottleTime() {
        return totalThrottleTime.sum();
    }

    /**
     * Get the time in milliseconds that has been shaped (sleeped) during the start of ETM. This is the time of all
     * threads combined. So if two threads slept both for 5 seconds in parallel then the total shape time will be 10 seconds.
     *
     * @return The time that all threads have slept during shaping.
     */
    public long getTotalShapeTime() {
        return totalShapeTime.sum();
    }

    /**
     * The state of a thread between each checks.
     */
    private static class ThreadState {
        /**
         * The epoch time we've last checked if the throttling should be applied.
         */
        private long lastChecked;

        /**
         * The number of request units processed since we've last checked.
         */
        private double lastRequestUnits;

        private ThreadState() {
            this.lastChecked = System.currentTimeMillis();
        }

        /**
         * Gives the epoch time the license exceedance is last checked.
         *
         * @return The epoch time since we've last checked if the license is exceeded.
         */
        public long getLastChecked() {
            return this.lastChecked;
        }

        /**
         * Update the object with the latest state.
         *
         * @param lastChecked      The epoch time the <code>Thread</code> has last checked for license exceedance.
         * @param lastRequestUnits The total number of processed request during the last checked time.
         */
        public void update(long lastChecked, double lastRequestUnits) {
            this.lastChecked = lastChecked;
            this.lastRequestUnits = lastRequestUnits;
        }
    }
}
