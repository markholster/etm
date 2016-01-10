package com.jecstar.etm.processor.metrics;

import static com.codahale.metrics.MetricRegistry.name;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.RatioGauge;

public class MemoryUsageMetricSet implements MetricSet {
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

    private final MemoryMXBean mxBean;
    private final List<MemoryPoolMXBean> memoryPools;

    public MemoryUsageMetricSet() {
        this(ManagementFactory.getMemoryMXBean(),
             ManagementFactory.getMemoryPoolMXBeans());
    }

    public MemoryUsageMetricSet(MemoryMXBean mxBean,
                               Collection<MemoryPoolMXBean> memoryPools) {
        this.mxBean = mxBean;
        this.memoryPools = new ArrayList<MemoryPoolMXBean>(memoryPools);
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<String, Metric>();

        gauges.put("mem.total.init", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return mxBean.getHeapMemoryUsage().getInit() +
                        mxBean.getNonHeapMemoryUsage().getInit();
            }
        });

        gauges.put("mem.total.used", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return mxBean.getHeapMemoryUsage().getUsed() +
                        mxBean.getNonHeapMemoryUsage().getUsed();
            }
        });

        gauges.put("mem.total.max", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return mxBean.getHeapMemoryUsage().getMax() +
                        mxBean.getNonHeapMemoryUsage().getMax();
            }
        });

        gauges.put("mem.total.committed", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return mxBean.getHeapMemoryUsage().getCommitted() +
                        mxBean.getNonHeapMemoryUsage().getCommitted();
            }
        });


        gauges.put("mem.heap.init", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return mxBean.getHeapMemoryUsage().getInit();
            }
        });

        gauges.put("mem.heap.used", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return mxBean.getHeapMemoryUsage().getUsed();
            }
        });

        gauges.put("mem.heap.max", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return mxBean.getHeapMemoryUsage().getMax();
            }
        });

        gauges.put("mem.heap.committed", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return mxBean.getHeapMemoryUsage().getCommitted();
            }
        });

        gauges.put("mem.heap.usage", new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                final MemoryUsage usage = mxBean.getHeapMemoryUsage();
                return Ratio.of(usage.getUsed(), usage.getMax());
            }
        });

        gauges.put("mem.non-heap.init", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return mxBean.getNonHeapMemoryUsage().getInit();
            }
        });

        gauges.put("mem.non-heap.used", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return mxBean.getNonHeapMemoryUsage().getUsed();
            }
        });

        gauges.put("mem.non-heap.max", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return mxBean.getNonHeapMemoryUsage().getMax();
            }
        });

        gauges.put("mem.non-heap.committed", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return mxBean.getNonHeapMemoryUsage().getCommitted();
            }
        });

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

            gauges.put(name("mem", poolName, "max"),new Gauge<Long>() {
                @Override
                public Long getValue() {
                    return pool.getUsage().getMax();
                }
            });

            gauges.put(name("mem", poolName, "used"),new Gauge<Long>() {
                @Override
                public Long getValue() {
                    return pool.getUsage().getUsed();
                }
            });

            gauges.put(name("mem", poolName, "committed"),new Gauge<Long>() {
                @Override
                public Long getValue() {
                    return pool.getUsage().getCommitted();
                }
            });

            gauges.put(name("mem", poolName, "init"),new Gauge<Long>() {
                @Override
                public Long getValue() {
                    return pool.getUsage().getInit();
                }
            });
        }

        return Collections.unmodifiableMap(gauges);
    }
}
