package io.wattom.tracer;

import net.bytebuddy.asm.Advice;

public final class LatencyAdvice {
    private LatencyAdvice() {}

    @Advice.OnMethodEnter
    static long enter() {
        return System.nanoTime();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    static void exit(@Advice.Enter long startNanos,
                     @Advice.Origin("#t") String className,
                     @Advice.Origin("#m") String methodName) {
        long elapsed = System.nanoTime() - startNanos;
        var stats = LatencyRegistry.get(className + "#" + methodName);
        if (stats != null) stats.record(elapsed);
    }
}
