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
        timestamps.removeIf(t -> t < cutoff);
        // Only add the event if it falls within the current window
        if (eventStartNanos >= cutoff) {
            timestamps.addLast(eventStartNanos);
        }
        int carriers = ForkJoinPool.commonPool().getParallelism();
        store.updatePinned(timestamps.size(), Math.min(1.0, timestamps.size() / (double) carriers));
    }
}
