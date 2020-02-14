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

package com.jecstar.etm.processor.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.RatioGauge;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.*;
import java.util.regex.Pattern;

import static com.codahale.metrics.MetricRegistry.name;

public class MemoryUsageMetricSet implements MetricSet {

    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

    private final MemoryMXBean mxBean;
    private final List<MemoryPoolMXBean> memoryPools;

    public MemoryUsageMetricSet() {
        this(ManagementFactory.getMemoryMXBean(),
                ManagementFactory.getMemoryPoolMXBeans());
    }

    private MemoryUsageMetricSet(MemoryMXBean mxBean,
                                 Collection<MemoryPoolMXBean> memoryPools) {
        this.mxBean = mxBean;
        this.memoryPools = new ArrayList<>(memoryPools);
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<>();

        gauges.put("mem.total.init", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getInit() +
                mxBean.getNonHeapMemoryUsage().getInit());

        gauges.put("mem.total.used", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getUsed() +
                mxBean.getNonHeapMemoryUsage().getUsed());

        gauges.put("mem.total.max", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getMax() +
                mxBean.getNonHeapMemoryUsage().getMax());

        gauges.put("mem.total.committed", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getCommitted() +
                mxBean.getNonHeapMemoryUsage().getCommitted());


        gauges.put("mem.heap.init", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getInit());

        gauges.put("mem.heap.used", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getUsed());

        gauges.put("mem.heap.max", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getMax());

        gauges.put("mem.heap.committed", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getCommitted());

        gauges.put("mem.heap.usage", new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                final MemoryUsage usage = mxBean.getHeapMemoryUsage();
                return Ratio.of(usage.getUsed(), usage.getMax());
            }
        });

        gauges.put("mem.non-heap.init", (Gauge<Long>) () -> mxBean.getNonHeapMemoryUsage().getInit());

        gauges.put("mem.non-heap.used", (Gauge<Long>) () -> mxBean.getNonHeapMemoryUsage().getUsed());

        gauges.put("mem.non-heap.max", (Gauge<Long>) () -> mxBean.getNonHeapMemoryUsage().getMax());

        gauges.put("mem.non-heap.committed", (Gauge<Long>) () -> mxBean.getNonHeapMemoryUsage().getCommitted());

        gauges.put("mem.non-heap.usage", new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                final MemoryUsage usage = mxBean.getNonHeapMemoryUsage();
                return Ratio.of(usage.getUsed(), usage.getMax());
            }
        });

        for (final MemoryPoolMXBean pool : memoryPools) {
            final String poolName = name("pools", WHITESPACE.matcher(pool.getName()).replaceAll("-"));

            gauges.put(name("mem", poolName, "usage"),
                    new RatioGauge() {
                        @Override
                        protected Ratio getRatio() {
                            MemoryUsage usage = pool.getUsage();
                            return Ratio.of(usage.getUsed(),
                                    usage.getMax() == -1 ? usage.getCommitted() : usage.getMax());
                        }
                    });

            gauges.put(name("mem", poolName, "max"), (Gauge<Long>) () -> pool.getUsage().getMax());

            gauges.put(name("mem", poolName, "used"), (Gauge<Long>) () -> pool.getUsage().getUsed());

            gauges.put(name("mem", poolName, "committed"), (Gauge<Long>) () -> pool.getUsage().getCommitted());

            gauges.put(name("mem", poolName, "init"), (Gauge<Long>) () -> pool.getUsage().getInit());
        }

        return Collections.unmodifiableMap(gauges);
    }
}
