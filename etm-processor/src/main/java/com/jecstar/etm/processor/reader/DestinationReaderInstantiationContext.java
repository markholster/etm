package com.jecstar.etm.processor.reader;

/**
 * A context class that holds the context under which a new <code>DestinationReader</code> is created.
 * @param <T>
 */
public class DestinationReaderInstantiationContext<T extends DestinationReader> {

    private final DestinationReaderPool<T> destinationReaderPool;
    private final int indexInPool;

    DestinationReaderInstantiationContext(DestinationReaderPool<T> destinationReaderPool, int indexInPool) {
        this.destinationReaderPool = destinationReaderPool;
        this.indexInPool = indexInPool;
    }

    public DestinationReaderPool<T> getDestinationReaderPool() {
        return this.destinationReaderPool;
    }

    public int getIndexInPool() {
        return this.indexInPool;
    }
}
