package io.wattom.transport;

import io.wattom.metrics.HeapPool;
import io.wattom.metrics.MetricSnapshot;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MetricCodecTest {

    private static MetricSnapshot roundTrip(MetricSnapshot orig) throws IOException {
        var baos = new ByteArrayOutputStream();
        MetricCodec.encode(orig, new DataOutputStream(baos));
        return MetricCodec.decode(new DataInputStream(
            new ByteArrayInputStream(baos.toByteArray())));
    }

    @Test
    void roundTripFullSnapshot() throws IOException {
        var orig = new MetricSnapshot(
            1_000_000L, 0.45f, 0.05f, 0.60f,
            List.of(
                new HeapPool("Eden Space",   10_000_000L,  20_000_000L,  50_000_000L),
                new HeapPool("Old Gen",      80_000_000L, 100_000_000L, 256_000_000L)
            ),
            3, 0.375, 8, 4_000_000L,
            List.of("Thread-1 -> Thread-2 deadlock"),
            List.of("com.example.Foo#process p99=245us n=1000"),
            true
        );
        var dec = roundTrip(orig);
        assertEquals(orig.timestampNanos(), dec.timestampNanos());
        assertEquals(orig.cpuJvmUser(), dec.cpuJvmUser(), 0.001f);
        assertEquals(2, dec.heapPools().size());
        assertEquals("Eden Space", dec.heapPools().get(0).name());
        assertEquals(10_000_000L, dec.heapPools().get(0).used());
        assertEquals(3, dec.pinnedVtCount());
        assertEquals(0.375, dec.starvationRatio(), 0.001);
        assertEquals(1, dec.deadlockChains().size());
        assertEquals(1, dec.tracerLines().size());
        assertTrue(dec.cpuReady());
    }

    @Test
    void roundTripEmptySnapshot() throws IOException {
        var dec = roundTrip(MetricSnapshot.empty());
        assertEquals(0, dec.heapPools().size());
        assertEquals(0, dec.deadlockChains().size());
        assertFalse(dec.cpuReady());
    }
}
