package com.jecstar.etm.processor.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;

public class OperatingSystemMetricSet implements MetricSet {

	private final OperatingSystemMXBean os;

    public OperatingSystemMetricSet() {
		this.os = ManagementFactory.getOperatingSystemMXBean();
	}
    
	@Override
	public Map<String, Metric> getMetrics() {
		final Map<String, Metric> gauges = new HashMap<String, Metric>();
        gauges.put("os.cpu.system_load_average", new Gauge<Double>() {
            @Override
            public Double getValue() {
                return os.getSystemLoadAverage();
            }
        });
        return Collections.unmodifiableMap(gauges);
	}

}
