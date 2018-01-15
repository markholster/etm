package com.jecstar.etm.processor.reader;

import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;


/**
 * Class that maintains the number of <code>DestinationReader</code> instances that should be active.
 * <p>
 * <code>DestinationReader</code> instances should call the {@link DestinationReaderPool#increaseIfPossible()} method when
 * they think the pool should be increased. To prevent flooding the internal ringbuffer the pool can only increase when
 * the ringbuffer has sufficient space.
 *
 * @param <T> The <code>DestinationReader</code> implementation.
 */
public class DestinationReaderPool<T extends DestinationReader> {

    private static final LogWrapper log = LogFactory.getLogger(DestinationReaderPool.class);

    private final TelemetryCommandProcessor processor;
    private final ExecutorService executorService;
    private final String destinationName;
    private final int minReaders;
    private final int maxReaders;
    private final Function<DestinationReaderInstantiationContext<T>, T> readerInstantiator;

    private final List<DestinationReaderContext> currentReaders = new ArrayList<>();

    /**
     * Constructs a new <code>DestinationReaderPool</code> instance.
     *
     * This constructor starts {@link #minReaders } threads by calling the {@link #readerInstantiator} function.
     *
     * @param processor The <code>TelemetryCommandProcessor</code> used to query for the available ringbuffer space.
     * @param executorService The <code>ExecutorService</code> used to spawn new threads.
     * @param destinationName The name of the destination.
     * @param minReaders The minimum number of <code>DestinationReader</code> instances that should be started.
     * @param maxReaders The maximum number of <code>DestinationReader</code> instances that is allowed to be started.
     * @param readerInstantiator The <code>Function</code> that instantiates new <code>DestinationReader</code>.
     */
    public DestinationReaderPool(
            TelemetryCommandProcessor processor,
            ExecutorService executorService,
            String destinationName,
            int minReaders,
            int maxReaders,
            Function<DestinationReaderInstantiationContext<T>, T> readerInstantiator
    ) {
        this.processor = processor;
        this.executorService = executorService;
        this.destinationName = destinationName;
        this.minReaders = minReaders;
        this.maxReaders = maxReaders;
        this.readerInstantiator = readerInstantiator;
        initialize();
    }

    /**
     * Initializes this pool to the minimum number of threads.
     */
    private void initialize() {
        synchronized (this.currentReaders) {
            for (int i = 0; i < this.minReaders; i++) {
                T reader = this.readerInstantiator.apply(new DestinationReaderInstantiationContext<>(this, i));
                Future<?> future = this.executorService.submit(reader);
                this.currentReaders.add(new DestinationReaderContext(reader, future));
            }
        }
    }

    /**
     * Increase the number of readers in this pool if possible.
     */
    public void increaseIfPossible() {
        long currentRingBufferCapacity = this.processor.getCurrentCapacity();
        if (currentRingBufferCapacity < 128) {
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("Request to increase the number of listeners on destination '" + this.destinationName + "' denied because the ringbuffer has only " + currentRingBufferCapacity + " available slots.");
            }
            return;
        }
        synchronized (this.currentReaders) {
            removeFinishedFutures();
            if (this.currentReaders.size() < this.maxReaders) {
                T reader = this.readerInstantiator.apply(new DestinationReaderInstantiationContext<>(this, this.currentReaders.size()));
                Future<?> future = this.executorService.submit(reader);
                this.currentReaders.add(new DestinationReaderContext(reader, future));
                if (log.isInfoLevelEnabled()) {
                    log.logInfoMessage("Increased the number of listeners on destination '" + this.destinationName + "' to " + this.currentReaders.size() + ".");
                }
            }
        }
    }

    /**
     * Decrease the number of readers in this pool if possible.
     */
    public void decreaseIfPossible() {
        synchronized (this.currentReaders) {
            removeFinishedFutures();
            if (this.currentReaders.size() > this.minReaders) {
                DestinationReaderContext context = this.currentReaders.get(this.currentReaders.size() - 1);
                context.reader.stop();
                context.future.cancel(true);
                this.currentReaders.remove(context);
                if (log.isInfoLevelEnabled()) {
                    log.logInfoMessage("Decreased the number of listeners on destination '" + this.destinationName + "' to " + this.currentReaders.size() + ".");
                }
            }
        }
    }

    /**
     * Gives the number of current active readers.
     *
     * @return The number of active readers.
     */
    public int getNumberOfActiveReaders() {
        synchronized (this.currentReaders) {
            removeFinishedFutures();
        }
        return this.currentReaders.size();
    }

    /**
     * Remove the finished <code>Future</code> instances of the currentReaders list.
     *
     * A <code>Future</code> should only be finished on program shutdown, but we're better safe tha sorry in this case.
     */
    private void removeFinishedFutures() {
        Iterator<DestinationReaderContext> iterator = this.currentReaders.iterator();
        this.currentReaders.removeIf(p -> p.future.isDone());
    }

    /**
     * Stops all running <Code>DestinationReader</Code> instances.
     */
    public void stop() {
        synchronized (this.currentReaders) {
            removeFinishedFutures();
            Iterator<DestinationReaderContext> iterator = this.currentReaders.iterator();
            while (iterator.hasNext()) {
                DestinationReaderContext context = iterator.next();
                context.reader.stop();
                context.future.cancel(true);
                iterator.remove();
            }
        }
    }

    private class DestinationReaderContext {

        private final T reader;
        private final Future<?> future;

        DestinationReaderContext(T reader, Future<?> future) {
            this.reader = reader;
            this.future = future;
        }
    }
}