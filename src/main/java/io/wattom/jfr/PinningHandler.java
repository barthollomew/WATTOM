package io.wattom.jfr;

import io.wattom.metrics.MetricsStore;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ForkJoinPool;

public final class PinningHandler {
    private final MetricsStore store;
    private final long windowNanos;
    private final Deque<Long> timestamps = new ArrayDeque<>();

    public PinningHandler(MetricsStore store, long windowNanos) {
        this.store = store;
        this.windowNanos = windowNanos;
    }

    public synchronized void handle(long eventStartNanos, long durationNanos,
                                    String threadName, String topFrame) {
        long cutoff = System.nanoTime() - windowNanos;
        // JFR delivers events in approximately chronological order, so addLast keeps the
        // deque in non-decreasing timestamp order; front-trimming is O(1) amortized.
        while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
            timestamps.pollFirst();
        }
        if (eventStartNanos >= cutoff) {
            timestamps.addLast(eventStartNanos);
        }
        int carriers = ForkJoinPool.commonPool().getParallelism();
        store.updatePinned(timestamps.size(), Math.min(1.0, timestamps.size() / (double) carriers));
    }
}
