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

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Test class for the <code>ExponentialBackoffPolicy</code> class.
 */
public class ExponentialBackoffPolicyTest {

    @Test
    public void testBackoffPolicyForEveryMinute() {
        Duration duration = Duration.ofMinutes(1);
        // Every minute a notification until we hit the 45 minute mark. Step 45 hits the 45 minute mark so from step 32
        // (the nearest pow2 ) we expect 32 + 45 = step 77 to be notified again. From then on we expect steps from 45.
        int[] expectedNotificationSteps = new int[]{1, 2, 4, 8, 16, 32, 77, 122, 167, 212};
        for (int i = 1; i < 250; i++) {
            if (Arrays.binarySearch(expectedNotificationSteps, i) >= 0) {
                assertTrue(new ExponentialBackoffPolicy(i, duration).shouldBeNotified(), "Step " + i + " was expected to be notified.");
            } else {
                assertFalse(new ExponentialBackoffPolicy(i, duration).shouldBeNotified(), "Step " + i + " was not expected to be notified.");
            }
        }
    }

    @Test
    public void testBackoffPolicyForEveryThreeMinutes() {
        Duration duration = Duration.ofMinutes(3);
        // Every three minutes a notification until we hit the 45 minute mark. Step 16 (16 * 3 > 48) hits the 45 minute
        // mark so from step 8 (nearest pow2) we expect 8 + 15 = 23 to be notified again. From then on we expect steps
        // from 15.
        int[] expectedNotificationSteps = new int[]{1, 2, 4, 8, 23, 38, 53, 68, 83, 98, 113, 128, 143, 158, 173, 188,
                203, 218, 233, 248};
        for (int i = 1; i < 250; i++) {
            if (Arrays.binarySearch(expectedNotificationSteps, i) >= 0) {
                assertTrue(new ExponentialBackoffPolicy(i, duration).shouldBeNotified(), "Step " + i + " was expected to be notified.");
            } else {
                assertFalse(new ExponentialBackoffPolicy(i, duration).shouldBeNotified(), "Step " + i + " was not expected to be notified.");
            }
        }
    }
}
