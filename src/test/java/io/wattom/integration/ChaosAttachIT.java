package io.wattom.integration;

import io.wattom.launcher.AttachHelper;
import io.wattom.transport.MetricsReceiver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ChaosAttachIT {

    @Test
    @Timeout(60)
    void attachUnderHighCpuDoesNotOom() throws Exception {
        String agentJar = System.getProperty("wattom.jar");
        assertNotNull(agentJar, "wattom.jar system property must be set by Gradle");

        Process child = new ProcessBuilder(
            ProcessHandle.current().info().command().orElse("java"),
            "-Xmx64m",
            "-cp", agentJar,
            "io.wattom.workloads.CpuBurnWorkload"
        ).redirectErrorStream(true).start();

        try (var reader = child.inputReader()) {
            String line;
            while ((line = reader.readLine()) != null && !line.contains("READY")) {}
        }

        String pid = String.valueOf(child.pid());
        Path socketPath = Path.of("/tmp/wattom-chaos-" + pid + ".sock");

        CountDownLatch got3 = new CountDownLatch(3);
        MetricsReceiver receiver = new MetricsReceiver(socketPath, snap -> got3.countDown());
        receiver.start();

        assertDoesNotThrow(() -> AttachHelper.attach(pid, agentJar, "socketPath=" + socketPath));

        boolean ok = got3.await(20, TimeUnit.SECONDS);
        receiver.close();
        child.destroyForcibly();

        assertTrue(ok, "Did not receive 3 snapshots under high CPU within 20s");
    }
}
