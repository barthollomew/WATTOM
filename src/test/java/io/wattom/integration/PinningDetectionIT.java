package io.wattom.integration;

import io.wattom.launcher.AttachHelper;
import io.wattom.transport.MetricsReceiver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PinningDetectionIT {

    @Test
    @Timeout(45)
    void detectsVirtualThreadPinning() throws Exception {
        String agentJar = System.getProperty("wattom.jar");
        assertNotNull(agentJar, "wattom.jar system property must be set by Gradle");

        Process child = new ProcessBuilder(
            ProcessHandle.current().info().command().orElse("java"),
            "-cp", agentJar,
            "io.wattom.workloads.PinningWorkload"
        ).redirectErrorStream(true).start();

        try (var reader = child.inputReader()) {
            String line;
            while ((line = reader.readLine()) != null && !line.contains("READY")) { /* spin */ }
        }

        String pid = String.valueOf(child.pid());
        Path socketPath = Path.of("/tmp/wattom-it-" + pid + ".sock");

        CountDownLatch pinDetected = new CountDownLatch(1);
        AtomicReference<String> lastInfo = new AtomicReference<>("none");

        MetricsReceiver receiver = new MetricsReceiver(socketPath, snap -> {
            lastInfo.set("pinned=" + snap.pinnedVtCount() + " cpuReady=" + snap.cpuReady());
            if (snap.pinnedVtCount() > 0) pinDetected.countDown();
        });
        receiver.start();

        AttachHelper.attach(pid, agentJar, "socketPath=" + socketPath);

        boolean detected = pinDetected.await(30, TimeUnit.SECONDS);
        receiver.close();
        child.destroyForcibly();

        assertTrue(detected,
            "VT pinning not detected within 30s. Last snap: " + lastInfo.get());
    }
}
