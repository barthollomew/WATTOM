package io.wattom.integration;

import io.wattom.launcher.AttachHelper;
import io.wattom.metrics.HeapPool;
import io.wattom.metrics.MetricSnapshot;
import io.wattom.transport.MetricsReceiver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Chaos harness: spawns {@code chaos.jvms} child JVMs each running NestedLockWorkload
 * (deeply nested, oscillating synchronized locks), attaches WATTOM to all simultaneously,
 * then validates pinning detection, continuous metric flow, and absence of monotonic heap
 * growth in the target JVMs.
 *
 * Override run duration: -Dchaos.seconds=7200 (default: 30 for CI).
 * Override JVM count:    -Dchaos.jvms=3      (default: 3).
 */
class NestedLockChaosIT {

    private static final int DEFAULT_JVMS    = 3;
    private static final int DEFAULT_SECONDS = 30;
    private static final int MIN_SNAPSHOTS   = 10;

    record JvmHandle(
        Process process,
        Path sockPath,
        MetricsReceiver receiver,
        List<MetricSnapshot> snapshots,
        AtomicInteger pinDetected,
        CountDownLatch ready
    ) {}

    @Test
    @Timeout(300)
    void chaos_multipleJvms_pinningDetectedWithoutLeak() throws Exception {
        String agentJar = System.getProperty("wattom.jar");
        assertNotNull(agentJar, "wattom.jar system property must be set by Gradle");

        int jvmCount    = Integer.getInteger("chaos.jvms", DEFAULT_JVMS);
        int durationSec = Integer.getInteger("chaos.seconds", DEFAULT_SECONDS);
        String javaCmd  = ProcessHandle.current().info().command().orElse("java");

        List<JvmHandle> handles = new ArrayList<>();
        try {
            for (int i = 0; i < jvmCount; i++) {
                Process child = new ProcessBuilder(
                    javaCmd, "-Xmx64m", "-cp", agentJar,
                    "io.wattom.workloads.NestedLockWorkload"
                ).redirectErrorStream(true).start();

                Path sock = Path.of("/tmp/wattom-chaos-" + child.pid() + ".sock");
                List<MetricSnapshot> snaps = Collections.synchronizedList(new ArrayList<>());
                AtomicInteger pins = new AtomicInteger(0);
                CountDownLatch readyLatch = new CountDownLatch(1);

                Thread.ofVirtual().name("ready-drainer-" + i).start(() -> {
                    try (var r = child.inputReader()) {
                        String line;
                        while ((line = r.readLine()) != null) {
                            if (line.contains("READY")) { readyLatch.countDown(); return; }
                        }
                    } catch (Throwable ignored) {}
                    readyLatch.countDown(); // unblock even if process exits early
                });

                MetricsReceiver receiver = new MetricsReceiver(sock, snap -> {
                    snaps.add(snap);
                    if (snap.pinnedVtCount() > 0) pins.incrementAndGet();
                });
                receiver.start();
                handles.add(new JvmHandle(child, sock, receiver, snaps, pins, readyLatch));
            }

            for (JvmHandle h : handles) {
                boolean ok = h.ready().await(20, TimeUnit.SECONDS);
                assertTrue(ok, "Child JVM (pid=" + h.process().pid() + ") never printed READY");
            }

            for (JvmHandle h : handles) {
                String pid = String.valueOf(h.process().pid());
                AttachHelper.attach(pid, agentJar, "socketPath=" + h.sockPath());
            }

            Thread.sleep(durationSec * 1_000L);

            for (int i = 0; i < handles.size(); i++) {
                JvmHandle h = handles.get(i);
                String label = "JVM-" + i + " (pid=" + h.process().pid() + ")";

                assertTrue(h.process().isAlive(),
                    label + " exited unexpectedly (possible OOM or crash)");

                int snaps = h.snapshots().size();
                assertTrue(snaps >= MIN_SNAPSHOTS,
                    label + " delivered only " + snaps + " snapshots (want >= " + MIN_SNAPSHOTS + ")");

                assertTrue(h.pinDetected().get() > 0,
                    label + " never detected virtual-thread carrier pinning");

                assertNoMonotonicHeapGrowth(h.snapshots(), label);
            }

        } finally {
            for (JvmHandle h : handles) {
                h.receiver().close();
                h.process().destroyForcibly();
            }
        }
    }

    private static void assertNoMonotonicHeapGrowth(List<MetricSnapshot> snaps, String label) {
        if (snaps.size() < 20) return;
        int warmup = 5;
        long first = totalHeap(snaps.get(warmup));
        long last  = totalHeap(snaps.get(snaps.size() - 1));
        long growthPct = (first > 0) ? ((last - first) * 100L / first) : 0;
        // Allow up to 200% growth (JIT compilation and class loading are legitimate).
        // What we're catching is an unbounded leak: e.g. 10× growth means something is wrong.
        assertTrue(growthPct < 200,
            label + " heap grew " + growthPct + "% over test (first=" + first +
            " B, last=" + last + " B). Possible agent memory leak.");
    }

    private static long totalHeap(MetricSnapshot snap) {
        return snap.heapPools().stream().mapToLong(HeapPool::used).sum();
    }
}
