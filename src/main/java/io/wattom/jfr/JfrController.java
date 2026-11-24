package io.wattom.jfr;

import io.wattom.metrics.MetricsStore;
import jdk.jfr.consumer.RecordingStream;
import java.time.Duration;
import java.time.Instant;

public final class JfrController implements AutoCloseable {
    private final RecordingStream stream;

    public JfrController(MetricsStore store) {
        stream = new RecordingStream();
        var pinning = new PinningHandler(store, 10_000_000_000L);
        var cpu     = new CpuLoadHandler(store);
        var tlab    = new TlabHandler(store);

        stream.enable("jdk.VirtualThreadPinned")
              .withThreshold(Duration.ofMillis(20))
              .withStackTrace();
        stream.enable("jdk.CPULoad").withPeriod(Duration.ofMillis(500));
        stream.enable("jdk.ObjectAllocationInNewTLAB");

        // Convert JFR epoch Instant to the System.nanoTime() clock used by PinningHandler.
        stream.onEvent("jdk.VirtualThreadPinned", e -> pinning.handle(
            System.nanoTime() - Math.max(0L, Duration.between(e.getStartTime(), Instant.now()).toNanos()),
            e.getDuration().toNanos(),
            e.getThread("eventThread") != null
                ? e.getThread("eventThread").getJavaName() : "unknown",
            e.hasField("pinnedReason") ? e.getString("pinnedReason") : ""));

        stream.onEvent("jdk.CPULoad", e ->
            cpu.handle(e.getFloat("jvmUser"), e.getFloat("jvmSystem"), e.getFloat("machineTotal")));

        stream.onEvent("jdk.ObjectAllocationInNewTLAB", e ->
            tlab.handle(e.getLong("tlabSize")));
    }

    public void start() { stream.startAsync(); }

    @Override
    public void close() {
        try { stream.close(); }
        catch (Throwable t) { System.err.println("[WATTOM] JFR close error: " + t.getMessage()); }
    }
}
