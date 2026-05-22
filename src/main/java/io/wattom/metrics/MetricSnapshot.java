package io.wattom.metrics;

import java.util.List;

public record MetricSnapshot(
    long timestampNanos,
    float cpuJvmUser,
    float cpuJvmSystem,
    float cpuMachineTotal,
    List<HeapPool> heapPools,
    int pinnedVtCount,
    double starvationRatio,
    int carrierCount,
    long tlabBytesPerSec,
    List<String> deadlockChains,
    List<String> tracerLines,
    boolean cpuReady
) {
    public static MetricSnapshot empty() {
        return new MetricSnapshot(System.nanoTime(), -1f, -1f, -1f,
            List.of(), 0, 0.0, 0, 0L, List.of(), List.of(), false);
    }
}
