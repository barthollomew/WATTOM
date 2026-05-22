package io.wattom.jfr;

import io.wattom.metrics.MetricsStore;

public final class CpuLoadHandler {
    private final MetricsStore store;
    public CpuLoadHandler(MetricsStore store) { this.store = store; }
    public void handle(float jvmUser, float jvmSystem, float machineTotal) {
        store.updateCpu(jvmUser, jvmSystem, machineTotal);
    }
}
