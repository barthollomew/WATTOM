package io.wattom.jfr;

import io.wattom.metrics.MetricsStore;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PinningHandlerTest {

    @Test
    void pinnedCountIncreasesOnHandle() {
        var store   = new MetricsStore();
        var handler = new PinningHandler(store, 10_000_000_000L); // 10s window
        assertEquals(0, store.snapshot().pinnedVtCount());
        handler.handle(System.nanoTime(), 50_000_000L, "Thread-1", "java.io.Foo.bar");
        assertEquals(1, store.snapshot().pinnedVtCount());
        handler.handle(System.nanoTime(), 30_000_000L, "Thread-2", "java.net.Bar.baz");
        assertEquals(2, store.snapshot().pinnedVtCount());
    }

    @Test
    void staleEventsExpireFromWindow() {
        var store   = new MetricsStore();
        var handler = new PinningHandler(store, 1_000_000L); // 1ms window
        // Inject an event timestamped 5ms in the past - older than the 1ms window
        handler.handle(System.nanoTime() - 5_000_000L, 50_000_000L, "T1", "foo");
        assertEquals(0, store.snapshot().pinnedVtCount());
    }
}
