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
