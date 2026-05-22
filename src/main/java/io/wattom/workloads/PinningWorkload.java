package io.wattom.workloads;

public final class PinningWorkload {
    public static void main(String[] ignoredArgs) throws Exception {
        Object lock = new Object();
        for (int i = 0; i < 4; i++) {
            Thread.ofVirtual().start(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    synchronized (lock) {
                        try { Thread.sleep(100); }
                        catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                    }
                }
            });
        }
        Thread.sleep(300);
        System.out.println("READY");
        System.out.flush();
        Thread.sleep(Long.MAX_VALUE);
    }
}
