package com.jecstar.etm.processor.reader;

public interface DestinationReader extends Runnable {

    /**
     * Stop the processing of events and release all resources.
     */
    void stop();
}
