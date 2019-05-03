package com.jecstar.etm.processor.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.Map;

public class OperatingSystemMetricSet implements MetricSet {

    private final Method getFreePhysicalMemorySize;
    private final Method getTotalPhysicalMemorySize;
    private final Method getFreeSwapSpaceSize;
    private final Method getTotalSwapSpaceSize;
    private final Method getSystemLoadAverage;
    private final Method getSystemCpuLoad;

    private final OperatingSystemMXBean os;

    public OperatingSystemMetricSet() {
        this.os = ManagementFactory.getOperatingSystemMXBean();
        this.getFreePhysicalMemorySize = getMethod("getFreePhysicalMemorySize");
        this.getTotalPhysicalMemorySize = getMethod("getTotalPhysicalMemorySize");
        this.getFreeSwapSpaceSize = getMethod("getFreeSwapSpaceSize");
        this.getTotalSwapSpaceSize = getMethod("getTotalSwapSpaceSize");
        this.getSystemLoadAverage = getMethod("getSystemLoadAverage");
        this.getSystemCpuLoad = getMethod("getSystemCpuLoad");
    }

    private Method getMethod(String methodName) {
        try {
            return Class.forName("com.sun.management.OperatingSystemMXBean").getMethod(methodName);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return Map.of(
                "os.cpu.system_load_average", (Gauge<Double>) os::getSystemLoadAverage,
                "os.cpu.system_load_percentage", (Gauge<Double>) OperatingSystemMetricSet.this::getLoadPercentage,
                "os.mem.total", (Gauge<Long>) OperatingSystemMetricSet.this::getTotalPhysicalMemorySize,
                "os.mem.free", (Gauge<Long>) OperatingSystemMetricSet.this::getFreePhysicalMemorySize,
                "os.swap.total", (Gauge<Long>) OperatingSystemMetricSet.this::getTotalSwapSpaceSize,
                "os.swap.free", (Gauge<Long>) OperatingSystemMetricSet.this::getFreeSwapSpaceSize
        );
    }

    private double getLoadPercentage() {
        if (this.getSystemCpuLoad != null) {
            try {
                return (double) this.getSystemCpuLoad.invoke(this.os);
            } catch (Exception e) {
                return -1;
            }
        }
        return -1;
    }

    private long getFreePhysicalMemorySize() {
        if (this.getFreePhysicalMemorySize == null) {
            return -1;
        }
        try {
            return (long) this.getFreePhysicalMemorySize.invoke(this.os);
        } catch (Exception e) {
            return -1;
        }
    }

    private long getTotalPhysicalMemorySize() {
        if (this.getTotalPhysicalMemorySize == null) {
            return -1;
        }
        try {
            return (long) this.getTotalPhysicalMemorySize.invoke(this.os);
        } catch (Exception e) {
            return -1;
        }
    }

    private long getFreeSwapSpaceSize() {
        if (this.getFreeSwapSpaceSize == null) {
            return -1;
        }
        try {
            return (long) this.getFreeSwapSpaceSize.invoke(this.os);
        } catch (Exception e) {
            return -1;
        }
    }

    private long getTotalSwapSpaceSize() {
        if (this.getTotalSwapSpaceSize == null) {
            return -1;
        }
        try {
            return (long) this.getTotalSwapSpaceSize.invoke(this.os);
        } catch (Exception e) {
            return -1;
        }
    }

}
