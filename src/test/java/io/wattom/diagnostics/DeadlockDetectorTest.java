package io.wattom.diagnostics;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DeadlockDetectorTest {

    @Test
    void returnsEmptyWhenNoDeadlock() {
        assertTrue(new DeadlockDetector().detect().isEmpty());
    }

    @Test
    void detectsDeadlockBetweenTwoThreads() throws InterruptedException {
        Object lockA = new Object(), lockB = new Object();
        var t1ready  = new CountDownLatch(1);
        var t2ready  = new CountDownLatch(1);

        Thread t1 = new Thread(() -> {
            synchronized (lockA) {
                t1ready.countDown();
                try { t2ready.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                synchronized (lockB) { /* never reached */ }
            }
        }, "WattomDL-1");

        Thread t2 = new Thread(() -> {
            synchronized (lockB) {
                t2ready.countDown();
                try { t1ready.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                synchronized (lockA) { /* never reached */ }
            }
        }, "WattomDL-2");

        t1.setDaemon(true); t2.setDaemon(true);
        t1.start(); t2.start();
        t1ready.await(); t2ready.await();
        Thread.sleep(150);

        List<String> chains = new DeadlockDetector().detect();
        assertFalse(chains.isEmpty(), "Expected deadlock detection");
        String all = String.join(" ", chains);
        assertTrue(all.contains("WattomDL"), "Expected thread names in chain: " + all);

        t1.interrupt(); t2.interrupt();
    }
}
