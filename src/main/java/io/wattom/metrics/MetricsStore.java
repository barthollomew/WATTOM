package io.wattom.metrics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public final class MetricsStore {
    private volatile float  cpuJvmUser   = -1f;
    private volatile float  cpuJvmSystem = -1f;
    private volatile float  cpuMachine   = -1f;
    private volatile boolean cpuReady    = false;
    private volatile long   tlabBps      = 0L;
    private volatile int    carrierCount;
    private volatile int    pinnedCount  = 0;
    private volatile double starvation   = 0.0;
    private final List<HeapPool> heapPools   = new CopyOnWriteArrayList<>();
    private final List<String>   deadlocks   = new CopyOnWriteArrayList<>();
    private final AtomicReference<List<String>> tracerLines = new AtomicReference<>(List.of());

    public MetricsStore() {
        this.carrierCount = java.util.concurrent.ForkJoinPool.commonPool().getParallelism();
    }

    public void updateCpu(float user, float sys, float machine) {
        cpuJvmUser = user; cpuJvmSystem = sys; cpuMachine = machine; cpuReady = true;
    }
    public void updatePinned(int count, double s)  { pinnedCount = count; starvation = s; }
    public void updateTlab(long bps)               { tlabBps = bps; }
    public void updateHeapPools(List<HeapPool> p)  { heapPools.clear(); heapPools.addAll(p); }
    public void updateDeadlocks(List<String> d)    { deadlocks.clear(); deadlocks.addAll(d); }
    public void updateTracerLines(List<String> t)  { tracerLines.set(List.copyOf(t)); }

    public MetricSnapshot snapshot() {
        return new MetricSnapshot(System.nanoTime(),
            cpuJvmUser, cpuJvmSystem, cpuMachine,
            List.copyOf(heapPools),
            pinnedCount, starvation, carrierCount,
            tlabBps,
            List.copyOf(deadlocks),
            tracerLines.get(),
            cpuReady);
    }
}
