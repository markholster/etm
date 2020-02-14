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

package com.jecstar.etm.signaler.backoff;


import java.time.Duration;

public class ExponentialBackoffPolicy implements BackoffPolicy {


    private final int step;
    private final Duration dutionBetweenSteps;

    public ExponentialBackoffPolicy(int step, Duration duractionBetweenSteps) {
        this.step = step;
        this.dutionBetweenSteps = duractionBetweenSteps;
    }

    @Override
    public boolean shouldBeNotified() {
        final int maxBackoffTime = 60 * 45; // Max backoff to 45 minutes.
        if (this.dutionBetweenSteps.getSeconds() >= maxBackoffTime) {
            // If the duration between steps is bigger that the maximum backoff time then there's no need to backoff.
            return true;
        }
        final long totalSeconds = this.dutionBetweenSteps.multipliedBy(this.step - 1).getSeconds();
        if (totalSeconds < maxBackoffTime) {
            // The total time is <= the max backoff time so we should calculate if this step should be notified.
            return this.step == 1 || isPowerOfTwo(this.step);
        }
        // The total time has passed the back backoff time. We should calculate the nearest step.
        int stepNearestToAtLeastOncePer = previousPow2((int) (maxBackoffTime / this.dutionBetweenSteps.getSeconds()));

        // Subtract the time of "stepNearestToAtLeastOncePer" of the total time.
        long secondsLeft = totalSeconds - this.dutionBetweenSteps.multipliedBy(stepNearestToAtLeastOncePer - 1).getSeconds();
        return secondsLeft % maxBackoffTime < this.dutionBetweenSteps.getSeconds();
    }


    private boolean isPowerOfTwo(int value) {
        return value != 0 && (value & (value - 1)) == 0;
    }


    private int previousPow2(int value) {
        return value == 1 ? 1 : Integer.highestOneBit(value - 1);
    }
}
