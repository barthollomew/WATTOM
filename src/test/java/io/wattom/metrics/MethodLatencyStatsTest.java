package io.wattom.metrics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MethodLatencyStatsTest {

    @Test
    void p99ReturnsNegativeOneWhenEmpty() {
        assertEquals(-1L, new MethodLatencyStats(100).p99Nanos());
    }

    @Test
    void p99AccurateForUniformDistribution() {
        var stats = new MethodLatencyStats(1000);
        for (int i = 1; i <= 1000; i++) stats.record(i);
        long p99 = stats.p99Nanos();
        assertTrue(p99 >= 990 && p99 <= 1000,
            "p99 of [1..1000] should be 990-1000, got " + p99);
    }

    @Test
    void ringBufferWrapsOldestEntry() {
        var stats = new MethodLatencyStats(3);
        stats.record(100); stats.record(200); stats.record(300);
        stats.record(400); // evicts 100; ring: [400, 200, 300]
        assertEquals(400L, stats.p99Nanos());
    }

    @Test
    void countCappedAtCapacity() {
        var stats = new MethodLatencyStats(5);
        for (int i = 0; i < 8; i++) stats.record(i * 10L);
        assertEquals(5, stats.count());
    }
}
