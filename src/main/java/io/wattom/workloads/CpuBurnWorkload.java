package io.wattom.workloads;

public final class CpuBurnWorkload {
    public static void main(String[] ignoredArgs) throws Exception {
        int cores = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < cores; i++) {
            Thread.ofPlatform().daemon(true).start(() -> {
                double x = 1.0;
                while (!Thread.currentThread().isInterrupted()) x = Math.sqrt(x + 1);
            });
        }
        Thread.sleep(100);
        System.out.println("READY");
        System.out.flush();
        Thread.sleep(Long.MAX_VALUE);
    }
}
