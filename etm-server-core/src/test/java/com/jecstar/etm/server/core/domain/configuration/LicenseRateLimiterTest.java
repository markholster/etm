package com.jecstar.etm.server.core.domain.configuration;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the <code>LicenseThrottler</code> class.
 */
public class LicenseRateLimiterTest {

    /**
     * A license with 10 RU/s.
     */
    private final String limitedLicense = "eyJsaWNlbnNlIjp7Im93bmVyIjoiSmVjc3RhciBJbm5vdmF0aW9uIFYuTy5GLiIsInN0YXJ0X2RhdGUiOjE1Nzk3MTU5MzEyMDEsImV4cGlyeV9kYXRlIjozNDcxNTUxOTk5OTksIm1heF9yZXF1ZXN0X3VuaXRzX3Blcl9zZWNvbmQiOjEwLCJsaWNlbnNlX3R5cGUiOiJPTl9QUkVNIn0sImhhc2giOiI5NW0venJBQWI0OU5ubmh4RjVmZit1b3N1RWFrNkpmdWRoZ2xMQWtTaXgxWkM5andhb2o3VmNGRFFUVmJOSDBveHU1Qk50a1IrZ2dZQ2ppbDAwQVdIQT09Iiwic2lnbmF0dXJlIjoiRFlzX0VEWnpFRnd3S2ZFVjEwRVp0NGF1d0tWajA0eVZVaTNreXRabDNPRnpUTmE5VDJ0a1lvUFZULVZNUDlaeWVJUzF6WDhmQjFKWVNuaDV5NWhyTWc9PSJ9";

    /**
     * Test throttling with multiple threads with a license that has a limited ru/s rate.
     */
    @Test
    public void testThrottleWithLimitedLicense() throws InterruptedException {
        final var finishWaitTime = 1000;
        var etmConfiguration = new EtmConfiguration(getClass().getName());
        etmConfiguration.setLicenseKey(this.limitedLicense);
        final var nrOfThreads = 2;
        var rateLimiter = etmConfiguration.getLicenseRateLimiter();
        var executorService = Executors.newFixedThreadPool(nrOfThreads);
        var startTime = System.currentTimeMillis();
        var totalRu = 0L;
        // First do an iteration which add the work.
        for (int i = 0; i < 100; i++) {
            Thread.sleep(25);
            final var rus = (long) (Math.random() * 5);
            totalRu += rus;
            executorService.submit(() -> {
                rateLimiter.addRequestUnits(rus).throttle();
            });
        }
        // When all work is submitted, submit nrOfThreads tasks that make the rateLimiter check for license exceedance.
        // This can only be done by waiting the minimal check interval that is configured in the LicenseThrottler
        for (int i = 0; i < nrOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    Thread.sleep(finishWaitTime);
                } catch (InterruptedException e) {
                    fail(e);
                }
                rateLimiter.throttle();
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(60, TimeUnit.SECONDS);

        var totalTime = new BigDecimal(System.currentTimeMillis() - startTime).divide(new BigDecimal(1000), RoundingMode.UP).intValue();
        var calculatedTime = totalRu / etmConfiguration.getLicense().getMaxRequestUnitsPerSecond();
        assertTrue(totalTime >= calculatedTime, "Expected totalTime to be bigger than calculated time. totalTime: " + totalTime + ", calculatedTime: " + calculatedTime);
    }

    /**
     * Test shaping with multiple threads with a license that has a limited ru/s rate.
     */
    @Test
    public void testShapeWithLimitedLicense() {
        var etmConfiguration = new EtmConfiguration(getClass().getName());
        etmConfiguration.setLicenseKey(this.limitedLicense);
        var rateLimiter = etmConfiguration.getLicenseRateLimiter();
        var executionTime = 10;
        var requestUnits = 5d;

        var sleepTime = rateLimiter.addRequestUnitsAndShape(requestUnits, executionTime);
        var expectedTime = (requestUnits / etmConfiguration.getLicense().getMaxRequestUnitsPerSecond()) * 1000;

        assertEquals(expectedTime, executionTime + sleepTime);
    }
}
