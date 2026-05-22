package io.wattom.jfr;

import io.wattom.metrics.MetricsStore;
import java.util.concurrent.atomic.AtomicLong;

public final class TlabHandler {
    private final MetricsStore store;
    private final AtomicLong windowBytes = new AtomicLong(0);
    private volatile long windowStart    = System.nanoTime();

    public TlabHandler(MetricsStore store) { this.store = store; }

    public synchronized void handle(long allocBytes) {
        long now = System.nanoTime();
        if (now - windowStart >= 1_000_000_000L) {
            store.updateTlab(windowBytes.getAndSet(0));
            windowStart = now;
        }
        windowBytes.addAndGet(allocBytes);
    }
}
