package io.wattom.workloads;

/**
 * Generates virtual-thread carrier pinning by nesting synchronized blocks across
 * multiple lock levels. All threads acquire locks in the same forward order so there
 * is no deadlock risk; chaos comes from high concurrency competing for each lock.
 * Each hold sleep exceeds WATTOM's JFR VirtualThreadPinned threshold (20 ms).
 */
public final class NestedLockWorkload {
    static final int  LOCK_DEPTH = 8;
    static final int  VT_COUNT   = 16;
    static final long HOLD_MS    = 100L;

    public static void main(String[] args) throws Exception {
        Object[] locks = new Object[LOCK_DEPTH];
        for (int i = 0; i < LOCK_DEPTH; i++) locks[i] = new Object();

        for (int i = 0; i < VT_COUNT; i++) {
            Thread.ofVirtual().name("nested-vt-" + i).start(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        acquireNested(locks, 0);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
        }

        Thread.sleep(500);
        System.out.println("READY");
        System.out.flush();
        Thread.sleep(Long.MAX_VALUE);
    }

    private static void acquireNested(Object[] locks, int depth) throws InterruptedException {
        if (depth >= LOCK_DEPTH) {
            Thread.sleep(HOLD_MS);
            return;
        }
        synchronized (locks[depth]) {
            acquireNested(locks, depth + 1);
        }
    }
}
