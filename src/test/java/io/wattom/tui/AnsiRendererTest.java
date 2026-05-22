package io.wattom.tui;

import io.wattom.metrics.HeapPool;
import io.wattom.metrics.MetricSnapshot;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AnsiRendererTest {

    @Test
    void renderShowsCpuAndPinningWhenReady() {
        var snap = new MetricSnapshot(0L, 0.45f, 0.05f, 0.60f,
            List.of(new HeapPool("Eden Space", 50_000_000L, 100_000_000L, 200_000_000L)),
            2, 0.25, 8, 1_000_000L, List.of(), List.of(), true);
        String out = AnsiRenderer.renderPlain(snap, 80);
        assertTrue(out.contains("CPU"),    "Expected 'CPU' row");
        assertTrue(out.contains("45"),     "Expected JVM CPU %");
        assertTrue(out.contains("Pinned"), "Expected pinning row");
        assertTrue(out.contains("Eden"),   "Expected heap pool name");
    }

    @Test
    void renderShowsDashesWhenCpuNotReady() {
        String out = AnsiRenderer.renderPlain(MetricSnapshot.empty(), 80);
        assertTrue(out.contains("--"), "Expected '--' when CPU data not yet available");
    }

    @Test
    void renderIncludesDeadlockChain() {
        var snap = new MetricSnapshot(0L, 0.1f, 0.0f, 0.2f,
            List.of(), 0, 0.0, 8, 0L,
            List.of("Thread-1 -> Thread-2 deadlock"), List.of(), true);
        assertTrue(AnsiRenderer.renderPlain(snap, 80).contains("Thread-1"));
    }
}
