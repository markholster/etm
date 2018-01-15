package com.jecstar.etm.processor.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.regex.Pattern;

import static com.codahale.metrics.MetricRegistry.name;

public class GarbageCollectorMetricSet implements MetricSet {
	
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

    private final List<GarbageCollectorMXBean> garbageCollectors;

    /**
     * Creates a new set of gauges for all discoverable garbage collectors.
     */
    public GarbageCollectorMetricSet() {
        this(ManagementFactory.getGarbageCollectorMXBeans());
    }

    /**
     * Creates a new set of gauges for the given collection of garbage collectors.
     *
     * @param garbageCollectors    the garbage collectors
     */
    private GarbageCollectorMetricSet(Collection<GarbageCollectorMXBean> garbageCollectors) {
        this.garbageCollectors = new ArrayList<>(garbageCollectors);
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<>();
        for (final GarbageCollectorMXBean gc : this.garbageCollectors) {
            final String name = WHITESPACE.matcher(gc.getName()).replaceAll("-");
            gauges.put(name("gc.collectors", name, "count"), (Gauge<Long>) gc::getCollectionCount);

            gauges.put(name("gc.collectors", name, "time"), (Gauge<Long>) gc::getCollectionTime);
        }
        return Collections.unmodifiableMap(gauges);
    }
}
