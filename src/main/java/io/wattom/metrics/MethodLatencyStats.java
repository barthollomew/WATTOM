package io.wattom.metrics;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public final class MethodLatencyStats {
    private final long[] ring;
    private final int capacity;
    private final AtomicLong writeIdx = new AtomicLong(0);

    public MethodLatencyStats(int capacity) {
        this.capacity = capacity;
        this.ring = new long[capacity];
        Arrays.fill(ring, -1L);
    }

    public void record(long nanos) {
        ring[(int) (writeIdx.getAndIncrement() % capacity)] = nanos;
    }

    public long p99Nanos() {
        long[] valid = Arrays.stream(ring.clone()).filter(v -> v >= 0).sorted().toArray();
        if (valid.length == 0) return -1L;
        return valid[(int) Math.ceil(valid.length * 0.99) - 1];
    }

    public int count() {
        return (int) Math.min(writeIdx.get(), capacity);
    }
}
