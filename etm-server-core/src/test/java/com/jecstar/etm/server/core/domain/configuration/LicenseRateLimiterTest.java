package com.jecstar.etm.server.core.domain.configuration;

import org.junit.jupiter.api.Test;

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
    private final String limitedLicense = "cqPQ3EgKLBGrYUJWZTyPVsFYCvlsvzWSr1oDqm8PV64AmtF7xshSDrnDUDgWG+0NCj9II5mzBp38ArEOPawJkU9evYUBkrBZqSCZAFdxMQRIb9kmJ/hOmxQOw1s3H92/cQ9hE3q+IxVV6WgaUoZ3OWnLJvTcOhWOqBCR4dwWbbEr43+0v0bQhEEPywP41htuWOjBFN3EY4dMELyoYM8U0qyibODlPNeMEpt/5a9+U2gU+CaVG/X8Ntr3i7CFijTXj4Smv71mhUxwjBC5+2JhXDvfEJ1OOP9CBN+XkcYVm51MVw6rZrovjHfxmlNly8hg9ZQ4r0ylG2QoAnNrZFE8+8vfkoDPQ6W7QympijK82srOyudm/NQ1geaLdfqxw5zmKYQOOZJG36bzXmJgfan61Oqv5NG46Got4Z5oVhBvbY6lDjofjGaFuYzwKnTiIDIyVxvlQo8ZIvCdtegw6fJRwDUtTiShptLS/pAsYA6PGjyrsjFro/n6om4gTk7fLeM9Z+0ulkMAFBIPoc2f+Im0c8BJ/cu9ZOy6jcv9o8kDQx3EJy6GUnsSBt0L2m7tQAanUFFBfpOn7S2iLbGXkESo9AjusuJV76hFZ25Zy/EvDYv7qR/qnf3rrLdnzjgz3QYQkMt3Az/UC5XsKT9qsQz3YrsIocMNmfHxsT3SukoTfsM=";

    /**
     * Test throttling with multiple threads with a license that has a limited ru/s rate.
     */
    @Test
    public void testThrottleWithLimitedLicense() throws InterruptedException {
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
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    fail(e);
                }
                rateLimiter.throttle();
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(60, TimeUnit.SECONDS);
        var totalTime = (System.currentTimeMillis() - startTime) / 1000;
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
